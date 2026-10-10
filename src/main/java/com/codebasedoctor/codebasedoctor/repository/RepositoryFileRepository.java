package com.codebasedoctor.codebasedoctor.repository;

import com.codebasedoctor.codebasedoctor.model.RepositoryFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositoryFileRepository
        extends JpaRepository<RepositoryFile, Long> {

}
