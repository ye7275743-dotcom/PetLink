package com.petlink.modules.rescue.dto;

import java.util.List;

public class AnimalCreateRequest {
    private String name;
    private String species;
    private String sex;
    private Integer estimatedAgeMonths;
    private String color;
    private String healthCondition;
    private String personality;
    private String adoptionRequirements;
    private String initialHealthRecord;
    private List<String> imageTokens;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSpecies() { return species; }
    public void setSpecies(String species) { this.species = species; }
    public String getSex() { return sex; }
    public void setSex(String sex) { this.sex = sex; }
    public Integer getEstimatedAgeMonths() { return estimatedAgeMonths; }
    public void setEstimatedAgeMonths(Integer estimatedAgeMonths) { this.estimatedAgeMonths = estimatedAgeMonths; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getHealthCondition() { return healthCondition; }
    public void setHealthCondition(String healthCondition) { this.healthCondition = healthCondition; }
    public String getPersonality() { return personality; }
    public void setPersonality(String personality) { this.personality = personality; }
    public String getAdoptionRequirements() { return adoptionRequirements; }
    public void setAdoptionRequirements(String adoptionRequirements) { this.adoptionRequirements = adoptionRequirements; }
    public String getInitialHealthRecord() { return initialHealthRecord; }
    public void setInitialHealthRecord(String initialHealthRecord) { this.initialHealthRecord = initialHealthRecord; }
    public List<String> getImageTokens() { return imageTokens; }
    public void setImageTokens(List<String> imageTokens) { this.imageTokens = imageTokens; }
}
