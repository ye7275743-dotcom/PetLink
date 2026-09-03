package com.petlink.modules.animal.service;

import com.petlink.common.TimeUtils;
import com.petlink.modules.animal.entity.Animal;
import com.petlink.modules.animal.entity.AnimalImage;
import com.petlink.modules.animal.entity.HealthRecord;
import com.petlink.modules.animal.mapper.AnimalImageMapper;
import com.petlink.modules.animal.mapper.HealthRecordMapper;
import com.petlink.modules.animal.vo.AnimalDetailResponse;
import com.petlink.modules.animal.vo.AnimalImageResponse;
import com.petlink.modules.animal.vo.AnimalSummaryResponse;
import com.petlink.modules.animal.vo.HealthRecordPublicResponse;
import com.petlink.modules.animal.vo.HealthRecordResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class AnimalResponseAssembler {
    private final AnimalImageMapper imageMapper;
    private final HealthRecordMapper healthRecordMapper;

    public AnimalResponseAssembler(AnimalImageMapper imageMapper, HealthRecordMapper healthRecordMapper) {
        this.imageMapper=imageMapper; this.healthRecordMapper=healthRecordMapper;
    }

    public AnimalSummaryResponse summary(Animal animal) {
        AnimalImage cover=imageMapper.selectCover(animal.getId());
        return new AnimalSummaryResponse(String.valueOf(animal.getId()),animal.getName(),animal.getSpecies(),animal.getSex(),
                animal.getEstimatedAgeMonths(),animal.getColor(),animal.getHealthCondition(),animal.getStatus(),
                cover == null ? null : mediaUrl(cover.getId()),animal.getVersion(),
                TimeUtils.toOffset(animal.getCreatedAt()),TimeUtils.toOffset(animal.getUpdatedAt()));
    }

    public AnimalDetailResponse detail(Animal animal, boolean privileged) {
        AnimalSummaryResponse base=summary(animal);
        List<AnimalImageResponse> images=new ArrayList<>();
        for (AnimalImage image : imageMapper.selectByAnimalId(animal.getId())) {
            images.add(new AnimalImageResponse(String.valueOf(image.getId()),mediaUrl(image.getId()),image.getSortOrder()));
        }
        List<HealthRecordPublicResponse> healthRecords=healthRecords(animal.getId(),privileged);
        return new AnimalDetailResponse(base.getId(),base.getName(),base.getSpecies(),base.getSex(),base.getEstimatedAgeMonths(),
                base.getColor(),base.getHealthCondition(),base.getStatus(),base.getCoverImageUrl(),base.getVersion(),
                base.getCreatedAt(),base.getUpdatedAt(),privileged ? String.valueOf(animal.getRescueTaskId()) : null,
                animal.getSuspendReason(),images,healthRecords);
    }

    public List<HealthRecordPublicResponse> healthRecords(Long animalId, boolean privileged) {
        List<HealthRecordPublicResponse> out=new ArrayList<>();
        for (HealthRecord record : healthRecordMapper.selectByAnimalId(animalId)) {
            if (privileged) out.add(fullHealth(record)); else out.add(publicHealth(record));
        }
        return out;
    }

    public HealthRecordResponse fullHealth(HealthRecord record) {
        return new HealthRecordResponse(String.valueOf(record.getId()),String.valueOf(record.getRecorderId()),record.getContent(),
                TimeUtils.toOffset(record.getCreatedAt()));
    }

    public HealthRecordPublicResponse publicHealth(HealthRecord record) {
        return new HealthRecordPublicResponse(String.valueOf(record.getId()),record.getContent(),TimeUtils.toOffset(record.getCreatedAt()));
    }

    private String mediaUrl(Long imageId) { return "/api/media/animal-images/" + imageId; }
}
