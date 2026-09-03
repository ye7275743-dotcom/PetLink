package com.petlink.modules.rescue.dto;

import java.util.List;

public class RescueResultRequest {
    private String result;
    private String failureReason;
    private List<AnimalCreateRequest> animals;

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public List<AnimalCreateRequest> getAnimals() { return animals; }
    public void setAnimals(List<AnimalCreateRequest> animals) { this.animals = animals; }
}
