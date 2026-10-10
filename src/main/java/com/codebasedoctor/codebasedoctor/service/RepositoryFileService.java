package com.codebasedoctor.codebasedoctor.service;
import com.codebasedoctor.codebasedoctor.model.Repository;
import com.codebasedoctor.codebasedoctor.model.RepositoryFile;
import com.codebasedoctor.codebasedoctor.repository.RepositoryFileRepository;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

@Service
public class RepositoryFileService {
    private final RepositoryFileRepository repositoryFileRepository;

    public RepositoryFileService(
            RepositoryFileRepository repositoryFileRepository) {

        this.repositoryFileRepository = repositoryFileRepository;
    }

    public void scanRepository(
            File repositoryDirectory,
            Repository repository) {

        scanDirectory(repositoryDirectory, repository);
    }

    private void scanDirectory(
            File directory,
            Repository repository) {

        File[] files = directory.listFiles();

        if (files == null) {
            return;
        }

        for (File file : files) {

            if (file.isDirectory()) {

                if (shouldIgnoreDirectory(file.getName())) {
                    continue;
                }

                scanDirectory(file, repository);

            } else {

                if (isSupportedFile(file)) {

                    RepositoryFile repositoryFile =
                            new RepositoryFile();

                    repositoryFile.setFileName(file.getName());

                    repositoryFile.setFilePath(
                            file.getAbsolutePath()
                    );

                    repositoryFile.setLanguage(
                            detectLanguage(file.getName())
                    );

                    repositoryFile.setRepository(repository);

                    repositoryFileRepository.save(repositoryFile);
                }
            }
        }
    }

    private boolean shouldIgnoreDirectory(String name) {

        return name.equals(".git")
                || name.equals("target")
                || name.equals("node_modules")
                || name.equals("build");
    }

    private boolean isSupportedFile(File file) {

        String name = file.getName().toLowerCase();

        return name.endsWith(".java")
                || name.endsWith(".py")
                || name.endsWith(".js")
                || name.endsWith(".ts")
                || name.endsWith(".xml")
                || name.endsWith(".json")
                || name.endsWith(".yml")
                || name.endsWith(".yaml")
                || name.endsWith(".md");
    }

    private String detectLanguage(String fileName) {

        String name = fileName.toLowerCase();

        if (name.endsWith(".java")) {
            return "JAVA";
        }

        if (name.endsWith(".py")) {
            return "PYTHON";
        }

        if (name.endsWith(".js")) {
            return "JAVASCRIPT";
        }

        if (name.endsWith(".ts")) {
            return "TYPESCRIPT";
        }

        if (name.endsWith(".xml")) {
            return "XML";
        }

        if (name.endsWith(".json")) {
            return "JSON";
        }

        if (name.endsWith(".yml")
                || name.endsWith(".yaml")) {
            return "YAML";
        }

        if (name.endsWith(".md")) {
            return "MARKDOWN";
        }

        return "UNKNOWN";
    }
}
