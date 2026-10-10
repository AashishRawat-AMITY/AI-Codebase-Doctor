package com.codebasedoctor.codebasedoctor.service;

import com.codebasedoctor.codebasedoctor.dto.RepositoryRequest;
import com.codebasedoctor.codebasedoctor.dto.RepositoryResponse;
import com.codebasedoctor.codebasedoctor.model.Repository;
import com.codebasedoctor.codebasedoctor.repository.RepositoryRepository;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class RepositoryService {

    private final RepositoryRepository repositoryRepository;
    private final GitRepositoryService gitRepositoryService;
    private final RepositoryFileService repositoryFileService;
    private final AIService aiService;

    public RepositoryService(
            RepositoryRepository repositoryRepository,
            GitRepositoryService gitRepositoryService,
            RepositoryFileService repositoryFileService,
            AIService aiService) {

        this.repositoryRepository = repositoryRepository;
        this.gitRepositoryService = gitRepositoryService;
        this.repositoryFileService = repositoryFileService;
        this.aiService = aiService;
    }

    public RepositoryResponse createRepository(RepositoryRequest request) {

        Repository repository = new Repository();

        repository.setName(request.getName());
        repository.setGithubUrl(request.getGithubUrl());
        repository.setStatus("CLONING");
        repository.setCreatedAt(LocalDateTime.now());

        Repository savedRepository = repositoryRepository.save(repository);

        try {
            // 1. Clone the GitHub repository
            File repositoryDirectory =
                    gitRepositoryService.cloneRepository(
                            savedRepository.getGithubUrl(),
                            savedRepository.getId()
                    );

            // 2. Update status
            savedRepository.setStatus("PROCESSING");
            repositoryRepository.save(savedRepository);

            // 3. Scan repository files and save metadata
            repositoryFileService.scanRepository(
                    repositoryDirectory,
                    savedRepository
            );

            // 4. Index the repository in the FastAPI RAG service

            aiService.indexRepository(
                    String.valueOf(savedRepository.getId()),
                    savedRepository.getGithubUrl()
            );


            // 5. Mark READY only after indexing succeeds
            savedRepository.setStatus("READY");
            repositoryRepository.save(savedRepository);

        } catch (Exception e) {

            savedRepository.setStatus("FAILED");
            repositoryRepository.save(savedRepository);

            throw new RuntimeException(
                    "Failed to clone, scan, or index the repository",
                    e
            );
        }

        return convertToResponse(savedRepository);
    }

    public List<RepositoryResponse> getAllRepositories() {

        return repositoryRepository.findAll()
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    private RepositoryResponse convertToResponse(Repository repository) {

        return new RepositoryResponse(
                repository.getId(),
                repository.getName(),
                repository.getGithubUrl(),
                repository.getStatus(),
                repository.getCreatedAt()
        );
    }
}