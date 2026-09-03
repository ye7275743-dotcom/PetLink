package com.petlink.modules.clue.dto;

import java.util.List;

public class AppendClueImagesRequest {
    private List<String> imageTokens;

    public List<String> getImageTokens() { return imageTokens; }
    public void setImageTokens(List<String> imageTokens) { this.imageTokens = imageTokens; }
}
