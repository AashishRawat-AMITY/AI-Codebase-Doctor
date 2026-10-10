package com.codebasedoctor.codebasedoctor.dto;

public class AskCodebaseRequest {

    private String repositoryId;
    private String question;

    public AskCodebaseRequest() {
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }
}