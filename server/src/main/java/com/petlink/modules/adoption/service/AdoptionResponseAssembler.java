package com.petlink.modules.adoption.service;

import com.petlink.common.TimeUtils;
import com.petlink.modules.adoption.entity.AdoptionApplication;
import com.petlink.modules.adoption.entity.AdoptionRecord;
import com.petlink.modules.adoption.mapper.AdoptionRecordMapper;
import com.petlink.modules.adoption.vo.AdoptionAnimalResponse;
import com.petlink.modules.adoption.vo.AdoptionApplicationDetailResponse;
import com.petlink.modules.adoption.vo.AdoptionApplicationSummaryResponse;
import com.petlink.modules.adoption.vo.AdoptionRecordResponse;
import com.petlink.modules.animal.entity.Animal;
import com.petlink.modules.animal.entity.AnimalImage;
import com.petlink.modules.animal.mapper.AnimalImageMapper;
import com.petlink.modules.animal.mapper.AnimalMapper;
import com.petlink.modules.animal.service.AnimalAccessService;
import com.petlink.security.UserPrincipal;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class AdoptionResponseAssembler {
    private final AnimalMapper animalMapper;
    private final AnimalImageMapper imageMapper;
    private final AdoptionRecordMapper recordMapper;
    private final AnimalAccessService animalAccess;

    public AdoptionResponseAssembler(AnimalMapper animalMapper,AnimalImageMapper imageMapper,AdoptionRecordMapper recordMapper,AnimalAccessService animalAccess){
        this.animalMapper=animalMapper;this.imageMapper=imageMapper;this.recordMapper=recordMapper;this.animalAccess=animalAccess;
    }

    public AdoptionApplicationSummaryResponse summary(AdoptionApplication app){
        Animal animal=requiredAnimal(app.getAnimalId());
        return summary(app,animal);
    }

    public List<AdoptionApplicationSummaryResponse> summaries(List<AdoptionApplication> applications){
        if(applications.isEmpty()) return List.of();
        Map<Long,Animal> animals=requiredAnimals(applications.stream().map(AdoptionApplication::getAnimalId).collect(Collectors.toList()));
        return applications.stream().map(app -> summary(app,animals.get(app.getAnimalId()))).collect(Collectors.toList());
    }

    private AdoptionApplicationSummaryResponse summary(AdoptionApplication app,Animal animal){
        return new AdoptionApplicationSummaryResponse(String.valueOf(app.getId()),String.valueOf(app.getAnimalId()),String.valueOf(app.getUserId()),animal.getName(),app.getStatus(),
                TimeUtils.toOffset(app.getCreatedAt()),TimeUtils.toOffset(app.getUpdatedAt()));
    }

    public AdoptionApplicationDetailResponse detail(AdoptionApplication app,UserPrincipal viewer){
        Animal animal=requiredAnimal(app.getAnimalId());
        AdoptionApplicationSummaryResponse base=summary(app,animal);
        boolean canReadImage=animalAccess.isVisible(viewer,animal) || isFinalAdopter(viewer,animal.getId());
        AdoptionAnimalResponse animalView=animal(animal,canReadImage);
        return new AdoptionApplicationDetailResponse(base.getId(),base.getAnimalId(),base.getAnimalName(),base.getStatus(),base.getCreatedAt(),base.getUpdatedAt(),
                String.valueOf(app.getUserId()),app.getAdoptionReason(),app.getHousingCondition(),app.getFamilyMembers(),app.getPetExperience(),app.getContact(),
                app.getReviewerId()==null?null:String.valueOf(app.getReviewerId()),TimeUtils.toOffset(app.getReviewedAt()),app.getRejectReason(),animalView);
    }

    public AdoptionRecordResponse record(AdoptionRecord record){
        Animal animal=requiredAnimal(record.getAnimalId());
        return record(record,animal,imageMapper.selectCover(animal.getId()));
    }

    public List<AdoptionRecordResponse> records(List<AdoptionRecord> records){
        if(records.isEmpty()) return List.of();
        List<Long> animalIds=records.stream().map(AdoptionRecord::getAnimalId).collect(Collectors.toList());
        Map<Long,Animal> animals=requiredAnimals(animalIds);
        Map<Long,AnimalImage> covers=imageMapper.selectCovers(new java.util.ArrayList<>(new LinkedHashSet<>(animalIds))).stream()
                .collect(Collectors.toMap(AnimalImage::getAnimalId,Function.identity()));
        return records.stream().map(record -> record(record,animals.get(record.getAnimalId()),covers.get(record.getAnimalId()))).collect(Collectors.toList());
    }

    private AdoptionRecordResponse record(AdoptionRecord record,Animal animal,AnimalImage cover){
        return new AdoptionRecordResponse(String.valueOf(record.getId()),String.valueOf(record.getApplicationId()),String.valueOf(record.getAnimalId()),
                String.valueOf(record.getUserId()),TimeUtils.toOffset(record.getAdoptedAt()),TimeUtils.toOffset(record.getCreatedAt()),animal(animal,cover));
    }

    private AdoptionAnimalResponse animal(Animal animal,boolean includeCover){
        AnimalImage cover=includeCover ? imageMapper.selectCover(animal.getId()) : null;
        return animal(animal,cover);
    }

    private AdoptionAnimalResponse animal(Animal animal,AnimalImage cover){
        String url=cover==null?null:"/api/media/animal-images/"+cover.getId();
        return new AdoptionAnimalResponse(String.valueOf(animal.getId()),animal.getName(),animal.getSpecies(),animal.getSex(),url);
    }
    private boolean isFinalAdopter(UserPrincipal viewer,Long animalId){
        return viewer!=null && recordMapper.existsByAnimalAndUser(animalId,viewer.getUserId())==1;
    }
    private Animal requiredAnimal(Long id){
        Animal animal=animalMapper.selectById(id);
        if(animal==null) throw new IllegalStateException("adoption row references missing animal");
        return animal;
    }

    private Map<Long,Animal> requiredAnimals(List<Long> requestedIds){
        Set<Long> uniqueIds=new LinkedHashSet<>(requestedIds);
        Map<Long,Animal> animals=new HashMap<>();
        for(Animal animal:animalMapper.selectBatchIds(uniqueIds)) animals.put(animal.getId(),animal);
        if(animals.size()!=uniqueIds.size()) throw new IllegalStateException("adoption row references missing animal");
        return animals;
    }
}
