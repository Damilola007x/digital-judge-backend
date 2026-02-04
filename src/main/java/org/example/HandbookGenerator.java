package org.example;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@Service
public class HandbookGenerator {

    @Value("${gemini.api.key}")
    private String apiKey;

    // Using Gemini 2.0 Flash via v1beta for best 2026 stability
    private final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";
    private final RestTemplate restTemplate = new RestTemplate();

    @SuppressWarnings("unchecked")
    private String callGemini(String prompt, boolean isJsonMode) {
        String cleanKey = apiKey.trim().replace("\"", "").replace("'", "");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", cleanKey);

        Map<String, Object> textPart = Map.of("text", prompt);
        Map<String, Object> contents = Map.of("parts", List.of(textPart));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", List.of(contents));

        if (isJsonMode) {
            requestBody.put("generationConfig", Map.of(
                    "response_mime_type", "application/json",
                    "temperature", 0.1
            ));
        }

        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(GEMINI_URL, entity, Map.class);

            Map<String, Object> body = response.getBody();
            if (body == null) return "{\"status\": \"ERROR\", \"explanation\": \"Empty response body\"}";

            List<Map<String, Object>> candidates = (List<Map<String, Object>>) body.get("candidates");
            if (candidates == null || candidates.isEmpty()) return "{\"status\": \"ERROR\", \"explanation\": \"No candidates returned\"}";

            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> contentParts = (List<Map<String, Object>>) content.get("parts");

            return (String) contentParts.get(0).get("text");

        } catch (Exception e) {
            return "{\"status\": \"ERROR\", \"explanation\": \"" + e.getMessage() + "\"}";
        }
    }

    public String translateHandbookToLogic(String pdfText) {
        String prompt = "Return a JSON array of formal logic rules from this text. " +
                "Format: [{\"predicate\": \"IsJunior\", \"isPositive\": true, \"arguments\": [\"User\"]}]. " +
                "Text: " + pdfText;
        return callGemini(prompt, true);
    }

    public String translateScenarioToFacts(String scenarioText) {
        String prompt = "Convert this scenario into a JSON array of logical facts. " +
                "Scenario: " + scenarioText;
        return callGemini(prompt, true);
    }

    /**
     * UPDATED: Now returns a structured JSON string so the Controller and JS
     * can perfectly sync the Status Header and the Explanation text.
     */
    public String generateExplanation(String rules, String scenario) {
        String prompt = "### GOAL\n" +
                "Analyze the scenario against the rules and provide a formal verdict.\n\n" +
                "### RULES DATA\n" + rules + "\n\n" +
                "### SCENARIO\n" + scenario + "\n\n" +
                "### RESPONSE FORMAT (JSON ONLY)\n" +
                "{\n" +
                "  \"status\": \"VIOLATION\" or \"COMPLIANT\",\n" +
                "  \"explanation\": \"A concise 2-sentence justification focusing ONLY on relevant rules.\"\n" +
                "}";

        // Set to true to ensure we get a parseable JSON object back
        return callGemini(prompt, true);
    }
}