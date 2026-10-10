
package com.codebasedoctor.codebasedoctor.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.util.HashMap;
import java.util.Map;

@Service
public class AIService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public AIService(
            @Value("${ai.service.url}") String aiServiceUrl,
            ObjectMapper objectMapper) {

        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);

        this.restClient = RestClient.builder()
                .baseUrl(aiServiceUrl)
                .requestFactory(requestFactory)
                .build();

        this.objectMapper = objectMapper;
    }

//    public void indexRepository(String repositoryId, String repositoryPath) {
//
//        Map<String, Object> request = new HashMap<>();
//        request.put("repository_id", repositoryId);
//        request.put("repository_path", repositoryPath);
//
//        String response = sendRequest("/api/index", request);
//
//        System.out.println("AI indexing response: " + response);
//    }

    public String indexRepository(String repositoryId, String repositoryPath) {

        Map<String, Object> request = new HashMap<>();
        request.put("repository_id", repositoryId);
        request.put("repository_path", repositoryPath);

        return sendRequest("/api/index", request);
    }



    public JsonNode askRepository(String repositoryId, String question) {

        Map<String, Object> request = new HashMap<>();
        request.put("repository_id", repositoryId);
        request.put("question", question);

        String response = sendRequest("/api/ask", request);

        try {
            JsonNode root = objectMapper.readTree(response);

            System.out.println("AI RESPONSE ROOT: " + root);

            if (root.has("answer")) {
                return root;
            }

            if (root.has("data") && root.get("data").has("answer")) {
                return root.get("data");
            }

            throw new RuntimeException(
                    "AI response does not contain an answer field: " + root);

        } catch (JsonProcessingException e) {
            throw new RuntimeException(
                    "Failed to parse AI service response: " + response, e);
        }
    }


    private String sendRequest(
            String endpoint,
            Map<String, Object> request) {

        try {
            String jsonBody = objectMapper.writeValueAsString(request);

            System.out.println("\n===== AI SERVICE REQUEST =====");
            System.out.println("Base URL: " + restClient);
            System.out.println("Endpoint: " + endpoint);
            System.out.println("Content-Type: application/json");
            System.out.println("Request JSON: " + jsonBody);
            System.out.println("HTTP Version: HTTP/1.1");
            System.out.println("==============================");

            String response = restClient.post()
                    .uri(endpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(jsonBody)
                    .retrieve()
                    .body(String.class);

            System.out.println("AI service response: " + response);

            return response;

        } catch (JsonProcessingException e) {
            throw new RuntimeException(
                    "Failed to serialize AI service request", e);
        } catch (Exception e) {
            System.err.println("AI service request failed: "
                    + e.getMessage());

            throw new RuntimeException(
                    "Failed to communicate with AI service", e);
        }
    }
}
