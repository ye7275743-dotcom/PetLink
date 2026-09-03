package com.petlink.modules.rescue.vo;

public class AnimalSummaryResponse {
    private final String id;
    private final String name;
    private final String species;
    private final String sex;
    private final String status;
    public AnimalSummaryResponse(String id, String name, String species, String sex, String status) {
        this.id=id; this.name=name; this.species=species; this.sex=sex; this.status=status;
    }
    public String getId(){ return id; }
    public String getName(){ return name; }
    public String getSpecies(){ return species; }
    public String getSex(){ return sex; }
    public String getStatus(){ return status; }
}
