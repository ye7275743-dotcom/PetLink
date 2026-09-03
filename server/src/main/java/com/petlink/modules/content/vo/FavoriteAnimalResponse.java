package com.petlink.modules.content.vo;

import java.time.OffsetDateTime;

public class FavoriteAnimalResponse {
    private final String favoriteId;
    private final OffsetDateTime createdAt;
    private final FavoriteAnimalSummaryResponse animal;

    public FavoriteAnimalResponse(String favoriteId, OffsetDateTime createdAt, FavoriteAnimalSummaryResponse animal) {
        this.favoriteId=favoriteId; this.createdAt=createdAt; this.animal=animal;
    }
    public String getFavoriteId() { return favoriteId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public FavoriteAnimalSummaryResponse getAnimal() { return animal; }
}
