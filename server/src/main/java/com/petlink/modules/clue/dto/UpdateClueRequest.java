package com.petlink.modules.clue.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.OffsetDateTime;

public class UpdateClueRequest {
    private String location;
    private OffsetDateTime foundTime;
    private String animalDescription;
    private String sceneDescription;
    private String contact;

    @JsonIgnore private boolean locationPresent;
    @JsonIgnore private boolean foundTimePresent;
    @JsonIgnore private boolean animalDescriptionPresent;
    @JsonIgnore private boolean sceneDescriptionPresent;
    @JsonIgnore private boolean contactPresent;

    public String getLocation() { return location; }
    public void setLocation(String location) { this.locationPresent = true; this.location = location; }
    public OffsetDateTime getFoundTime() { return foundTime; }
    public void setFoundTime(OffsetDateTime foundTime) { this.foundTimePresent = true; this.foundTime = foundTime; }
    public String getAnimalDescription() { return animalDescription; }
    public void setAnimalDescription(String animalDescription) { this.animalDescriptionPresent = true; this.animalDescription = animalDescription; }
    public String getSceneDescription() { return sceneDescription; }
    public void setSceneDescription(String sceneDescription) { this.sceneDescriptionPresent = true; this.sceneDescription = sceneDescription; }
    public String getContact() { return contact; }
    public void setContact(String contact) { this.contactPresent = true; this.contact = contact; }

    public boolean isLocationPresent() { return locationPresent; }
    public boolean isFoundTimePresent() { return foundTimePresent; }
    public boolean isAnimalDescriptionPresent() { return animalDescriptionPresent; }
    public boolean isSceneDescriptionPresent() { return sceneDescriptionPresent; }
    public boolean isContactPresent() { return contactPresent; }
    public boolean hasAnyField() { return locationPresent || foundTimePresent || animalDescriptionPresent || sceneDescriptionPresent || contactPresent; }
}
