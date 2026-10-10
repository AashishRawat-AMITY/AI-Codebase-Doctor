package com.codebasedoctor.codebasedoctor.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RepositoryRequest {
    @NotBlank(message = "Repository name is required")
    private String name;

    @NotBlank(message = "GitHub URL is required")
    private String githubUrl;
}
