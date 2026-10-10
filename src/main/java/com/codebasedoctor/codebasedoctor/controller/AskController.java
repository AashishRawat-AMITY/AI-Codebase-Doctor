
package com.codebasedoctor.codebasedoctor.controller;

import com.codebasedoctor.codebasedoctor.service.AIService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ask")
@CrossOrigin(origins = "*")
public class AskController {

    private final AIService aiService;

    public AskController(AIService aiService) {
        this.aiService = aiService;
    }

    @PostMapping
    public ResponseEntity<?> askQuestion(
            @RequestBody Map<String, String> request) {

        String repositoryId = request.get("repositoryId");
        String question = request.get("question");

        if (repositoryId == null || repositoryId.isBlank()
                || question == null || question.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Repository ID and question are required"));
        }

        JsonNode response = aiService.askRepository(repositoryId, question);

        System.out.println("AI RESPONSE JSON: " + response.toString());

        Map<String, Object> result = new HashMap<>();
        result.put("repository_id", response.path("repository_id").asText());
        result.put("answer", response.path("answer").asText());
        result.put("sources", response.path("sources"));

        return ResponseEntity.ok(result);
    }
}
