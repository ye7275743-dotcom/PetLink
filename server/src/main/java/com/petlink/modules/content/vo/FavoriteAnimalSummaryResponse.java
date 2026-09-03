package com.petlink.modules.content.vo;

public class FavoriteAnimalSummaryResponse {
    private final String id;
    private final String name;
    private final String species;
    private final String sex;
    private final Integer estimatedAgeMonths;
    private final String color;
    private final String healthCondition;
    private final String status;
    private final String coverImageUrl;

    public FavoriteAnimalSummaryResponse(String id, String name, String species, String sex,
                                         Integer estimatedAgeMonths, String color, String healthCondition,
                                         String status, String coverImageUrl) {
        this.id=id; this.name=name; this.species=species; this.sex=sex;
        this.estimatedAgeMonths=estimatedAgeMonths; this.color=color; this.healthCondition=healthCondition;
        this.status=status; this.coverImageUrl=coverImageUrl;
    }
    public String getId() { return id; }
    public String getName() { return name; }
    public String getSpecies() { return species; }
    public String getSex() { return sex; }
    public Integer getEstimatedAgeMonths() { return estimatedAgeMonths; }
    public String getColor() { return color; }
    public String getHealthCondition() { return healthCondition; }
    public String getStatus() { return status; }
    public String getCoverImageUrl() { return coverImageUrl; }
}
