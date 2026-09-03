package com.petlink.modules.adoption.vo;

public class AdoptionAnimalResponse {
    private final String id;
    private final String name;
    private final String species;
    private final String sex;
    private final String coverImageUrl;
    public AdoptionAnimalResponse(String id,String name,String species,String sex,String coverImageUrl){this.id=id;this.name=name;this.species=species;this.sex=sex;this.coverImageUrl=coverImageUrl;}
    public String getId(){return id;} public String getName(){return name;} public String getSpecies(){return species;} public String getSex(){return sex;} public String getCoverImageUrl(){return coverImageUrl;}
}

