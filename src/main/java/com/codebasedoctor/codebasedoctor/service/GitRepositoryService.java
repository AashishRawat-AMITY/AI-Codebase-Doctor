package com.codebasedoctor.codebasedoctor.service;
import org.eclipse.jgit.api.Git;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;

@Service

public class GitRepositoryService {
    @Value("${codebase.storage.path}")
    private String storagePath;

    public File cloneRepository(String githubUrl, Long repositoryId)
            throws Exception {

        File repositoryDirectory =
                new File(storagePath, String.valueOf(repositoryId));

        if (!repositoryDirectory.exists()) {
            repositoryDirectory.mkdirs();
        }

        Git.cloneRepository()
                .setURI(githubUrl)
                .setDirectory(repositoryDirectory)
                .call();

        return repositoryDirectory;
    }
}
