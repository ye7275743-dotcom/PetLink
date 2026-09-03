package com.petlink.modules.animal.dto;

import java.util.List;

public class AppendAnimalImagesRequest {
    private List<String> imageTokens;
    public List<String> getImageTokens() { return imageTokens; }
    public void setImageTokens(List<String> imageTokens) { this.imageTokens = imageTokens; }
}
