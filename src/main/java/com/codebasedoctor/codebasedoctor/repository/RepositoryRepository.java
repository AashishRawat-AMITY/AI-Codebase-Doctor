package com.codebasedoctor.codebasedoctor.repository;
import com.codebasedoctor.codebasedoctor.model.Repository;
import org.springframework.data.jpa.repository.JpaRepository;


public interface RepositoryRepository extends JpaRepository<Repository, Long> {

}