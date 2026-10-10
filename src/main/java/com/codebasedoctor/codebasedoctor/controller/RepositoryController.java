
package com.codebasedoctor.codebasedoctor.controller;

import com.codebasedoctor.codebasedoctor.dto.RepositoryRequest;
import com.codebasedoctor.codebasedoctor.dto.RepositoryResponse;
import com.codebasedoctor.codebasedoctor.service.RepositoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/repositories")
@CrossOrigin(origins = "*")
public class RepositoryController {

    private final RepositoryService repositoryService;

    public RepositoryController(RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }

    @PostMapping
    public ResponseEntity<RepositoryResponse> createRepository(
            @Valid @RequestBody RepositoryRequest request) {

        RepositoryResponse response =
                repositoryService.createRepository(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<RepositoryResponse>> getAllRepositories() {

        return ResponseEntity.ok(
                repositoryService.getAllRepositories()
        );
    }
}
