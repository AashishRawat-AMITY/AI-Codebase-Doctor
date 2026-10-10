package com.codebasedoctor.codebasedoctor.dto;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class RepositoryResponse {
    private Long id;
    private String name;
    private String githubUrl;
    private String status;
    private LocalDateTime createdAt;
}
