package org.example;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=";
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<LogicalRule> processPolicyPdf(String base64Pdf) {
        RestTemplate restTemplate = new RestTemplate();

        // 1. Setup the Multi-part Content (PDF + Instructions)
        Map<String, Object> pdfPart = Map.of("inline_data", Map.of("mime_type", "application/pdf", "data", base64Pdf));
        Map<String, Object> textPart = Map.of("text", "Act as a legal logic engine. Extract all policy rules as logical predicates.");

        Map<String, Object> content = Map.of("parts", Arrays.asList(pdfPart, textPart));

        // 2. Force JSON output mode
        Map<String, Object> generationConfig = Map.of("response_mime_type", "application/json");

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", Collections.singletonList(content));
        requestBody.put("generationConfig", generationConfig);

        // 3. Execute Request
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(API_URL + apiKey, requestBody, String.class);
            return parseGeminiResponse(response.getBody());
        } catch (Exception e) {
            throw new RuntimeException("Gemini API Error: " + e.getMessage());
        }
    }

    private List<LogicalRule> parseGeminiResponse(String rawBody) throws Exception {
        JsonNode root = objectMapper.readTree(rawBody);
        // Navigate through Gemini's nested response structure
        String jsonRules = root.path("candidates").get(0)
                .path("content").path("parts").get(0)
                .path("text").asText();

        return objectMapper.readValue(jsonRules, new TypeReference<List<LogicalRule>>(){});
    }
}