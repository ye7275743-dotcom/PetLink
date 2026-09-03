package com.petlink.modules.content.service;

import com.petlink.common.TimeUtils;
import com.petlink.modules.adoption.mapper.AdoptionRecordMapper;
import com.petlink.modules.animal.entity.Animal;
import com.petlink.modules.animal.entity.AnimalImage;
import com.petlink.modules.animal.mapper.AnimalImageMapper;
import com.petlink.modules.animal.mapper.AnimalMapper;
import com.petlink.modules.animal.service.AnimalAccessService;
import com.petlink.modules.content.entity.Favorite;
import com.petlink.modules.content.vo.FavoriteAnimalResponse;
import com.petlink.modules.content.vo.FavoriteAnimalSummaryResponse;
import com.petlink.security.UserPrincipal;
import org.springframework.stereotype.Component;

@Component
public class FavoriteResponseAssembler {
    private final AnimalMapper animalMapper;
    private final AnimalImageMapper imageMapper;
    private final AnimalAccessService animalAccess;
    private final AdoptionRecordMapper adoptionRecordMapper;

    public FavoriteResponseAssembler(AnimalMapper animalMapper,AnimalImageMapper imageMapper,
                                     AnimalAccessService animalAccess,AdoptionRecordMapper adoptionRecordMapper){
        this.animalMapper=animalMapper;this.imageMapper=imageMapper;this.animalAccess=animalAccess;this.adoptionRecordMapper=adoptionRecordMapper;
    }

    public FavoriteAnimalResponse assemble(UserPrincipal principal,Favorite favorite){
        Animal animal=animalMapper.selectById(favorite.getAnimalId());
        if(animal==null) throw new IllegalStateException("favorite references missing animal");
        AnimalImage cover=imageMapper.selectCover(animal.getId());
        boolean mediaVisible=animalAccess.isVisible(principal,animal)
                || (principal!=null && adoptionRecordMapper.existsByAnimalAndUser(animal.getId(),principal.getUserId())==1);
        String coverUrl=cover!=null && mediaVisible ? "/api/media/animal-images/"+cover.getId() : null;
        FavoriteAnimalSummaryResponse summary=new FavoriteAnimalSummaryResponse(String.valueOf(animal.getId()),animal.getName(),
                animal.getSpecies(),animal.getSex(),animal.getEstimatedAgeMonths(),animal.getColor(),animal.getHealthCondition(),
                animal.getStatus(),coverUrl);
        return new FavoriteAnimalResponse(String.valueOf(favorite.getId()),TimeUtils.toOffset(favorite.getCreatedAt()),summary);
    }
}
