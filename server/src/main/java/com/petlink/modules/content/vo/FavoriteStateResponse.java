package com.petlink.modules.content.vo;

public class FavoriteStateResponse {
    private final String animalId;
    private final boolean favorited;
    public FavoriteStateResponse(String animalId, boolean favorited) { this.animalId=animalId; this.favorited=favorited; }
    public String getAnimalId() { return animalId; }
    public boolean isFavorited() { return favorited; }
}
