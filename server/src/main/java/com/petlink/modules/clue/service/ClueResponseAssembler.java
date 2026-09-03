package com.petlink.modules.clue.service;

import com.petlink.common.TimeUtils;
import com.petlink.modules.clue.entity.RescueClue;
import com.petlink.modules.clue.entity.RescueClueImage;
import com.petlink.modules.clue.mapper.RescueClueImageMapper;
import com.petlink.modules.clue.vo.ClueDetailResponse;
import com.petlink.modules.clue.vo.ClueImageResponse;
import com.petlink.modules.clue.vo.ClueSummaryResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ClueResponseAssembler {
    private final RescueClueImageMapper imageMapper;

    public ClueResponseAssembler(RescueClueImageMapper imageMapper) {
        this.imageMapper = imageMapper;
    }

    public ClueSummaryResponse summary(RescueClue clue) {
        RescueClueImage cover = imageMapper.selectCover(clue.getId());
        String coverUrl = cover == null ? null : imageUrl(cover.getId());
        return new ClueSummaryResponse(
                String.valueOf(clue.getId()), clue.getLocation(), TimeUtils.toOffset(clue.getFoundTime()),
                clue.getAnimalDescription(), clue.getStatus(), coverUrl,
                TimeUtils.toOffset(clue.getCreatedAt()), TimeUtils.toOffset(clue.getUpdatedAt()));
    }

    public ClueDetailResponse detail(RescueClue clue, boolean includeReviewerId) {
        List<ClueImageResponse> images = imageMapper.selectByClueId(clue.getId()).stream()
                .map(this::image)
                .collect(Collectors.toList());
        String coverUrl = images.isEmpty() ? null : images.get(0).getUrl();
        return new ClueDetailResponse(
                String.valueOf(clue.getId()), clue.getLocation(), TimeUtils.toOffset(clue.getFoundTime()),
                clue.getAnimalDescription(), clue.getStatus(), coverUrl,
                TimeUtils.toOffset(clue.getCreatedAt()), TimeUtils.toOffset(clue.getUpdatedAt()),
                clue.getSceneDescription(), clue.getContact(), clue.getRejectReason(),
                includeReviewerId && clue.getReviewerId() != null ? String.valueOf(clue.getReviewerId()) : null,
                TimeUtils.toOffset(clue.getReviewedAt()), images);
    }

    private ClueImageResponse image(RescueClueImage image) {
        return new ClueImageResponse(String.valueOf(image.getId()), imageUrl(image.getId()), image.getSortOrder());
    }

    private String imageUrl(Long imageId) {
        return "/api/media/rescue-clue-images/" + imageId;
    }
}
