package com.petlink.modules.adoption.vo;

public class AdoptionAuditResponse {
    private final AdoptionApplicationDetailResponse application;
    private final AdoptionRecordResponse adoptionRecord;
    public AdoptionAuditResponse(AdoptionApplicationDetailResponse application,AdoptionRecordResponse adoptionRecord){this.application=application;this.adoptionRecord=adoptionRecord;}
    public AdoptionApplicationDetailResponse getApplication(){return application;} public AdoptionRecordResponse getAdoptionRecord(){return adoptionRecord;}
}

