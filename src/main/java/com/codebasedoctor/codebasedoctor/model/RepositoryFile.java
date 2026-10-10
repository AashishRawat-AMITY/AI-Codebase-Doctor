package com.codebasedoctor.codebasedoctor.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "repository_files")
@Data
@NoArgsConstructor
@AllArgsConstructor

public class RepositoryFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;

    private String filePath;

    private String language;

    @ManyToOne
    @JoinColumn(name = "repository_id")
    private Repository repository;
}
