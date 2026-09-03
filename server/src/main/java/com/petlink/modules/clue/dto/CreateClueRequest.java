package com.petlink.modules.clue.dto;

import java.time.OffsetDateTime;
import java.util.List;

public class CreateClueRequest {
    private String location;
    private OffsetDateTime foundTime;
    private String animalDescription;
    private String sceneDescription;
    private String contact;
    private List<String> imageTokens;

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public OffsetDateTime getFoundTime() { return foundTime; }
    public void setFoundTime(OffsetDateTime foundTime) { this.foundTime = foundTime; }
    public String getAnimalDescription() { return animalDescription; }
    public void setAnimalDescription(String animalDescription) { this.animalDescription = animalDescription; }
    public String getSceneDescription() { return sceneDescription; }
    public void setSceneDescription(String sceneDescription) { this.sceneDescription = sceneDescription; }
    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }
    public List<String> getImageTokens() { return imageTokens; }
    public void setImageTokens(List<String> imageTokens) { this.imageTokens = imageTokens; }
}
