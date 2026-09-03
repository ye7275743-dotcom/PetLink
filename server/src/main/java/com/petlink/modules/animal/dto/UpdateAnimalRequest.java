package com.petlink.modules.animal.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class UpdateAnimalRequest {
    private String name;
    private String species;
    private String sex;
    private Integer estimatedAgeMonths;
    private String color;
    private String healthCondition;
    private Integer version;

    @JsonIgnore private boolean namePresent;
    @JsonIgnore private boolean speciesPresent;
    @JsonIgnore private boolean sexPresent;
    @JsonIgnore private boolean estimatedAgeMonthsPresent;
    @JsonIgnore private boolean colorPresent;
    @JsonIgnore private boolean healthConditionPresent;

    public String getName() { return name; }
    public void setName(String name) { this.namePresent = true; this.name = name; }
    public String getSpecies() { return species; }
    public void setSpecies(String species) { this.speciesPresent = true; this.species = species; }
    public String getSex() { return sex; }
    public void setSex(String sex) { this.sexPresent = true; this.sex = sex; }
    public Integer getEstimatedAgeMonths() { return estimatedAgeMonths; }
    public void setEstimatedAgeMonths(Integer estimatedAgeMonths) { this.estimatedAgeMonthsPresent = true; this.estimatedAgeMonths = estimatedAgeMonths; }
    public String getColor() { return color; }
    public void setColor(String color) { this.colorPresent = true; this.color = color; }
    public String getHealthCondition() { return healthCondition; }
    public void setHealthCondition(String healthCondition) { this.healthConditionPresent = true; this.healthCondition = healthCondition; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public boolean isNamePresent() { return namePresent; }
    public boolean isSpeciesPresent() { return speciesPresent; }
    public boolean isSexPresent() { return sexPresent; }
    public boolean isEstimatedAgeMonthsPresent() { return estimatedAgeMonthsPresent; }
    public boolean isColorPresent() { return colorPresent; }
    public boolean isHealthConditionPresent() { return healthConditionPresent; }
    public boolean hasAnyBusinessField() {
        return namePresent || speciesPresent || sexPresent || estimatedAgeMonthsPresent || colorPresent || healthConditionPresent;
    }
}
