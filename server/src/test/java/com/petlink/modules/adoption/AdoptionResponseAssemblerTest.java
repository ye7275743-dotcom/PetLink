package com.petlink.modules.adoption;

import com.petlink.modules.adoption.entity.AdoptionApplication;
import com.petlink.modules.adoption.mapper.AdoptionRecordMapper;
import com.petlink.modules.adoption.service.AdoptionResponseAssembler;
import com.petlink.modules.animal.entity.Animal;
import com.petlink.modules.animal.entity.AnimalImage;
import com.petlink.modules.animal.mapper.AnimalImageMapper;
import com.petlink.modules.animal.mapper.AnimalMapper;
import com.petlink.modules.animal.service.AnimalAccessService;
import com.petlink.security.UserPrincipal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdoptionResponseAssemblerTest {
    @Test void historicalNonFinalApplicantDoesNotReceiveDeadCoverUrl(){
        AnimalMapper animals=mock(AnimalMapper.class);AnimalImageMapper images=mock(AnimalImageMapper.class);AdoptionRecordMapper records=mock(AdoptionRecordMapper.class);AnimalAccessService access=mock(AnimalAccessService.class);
        AdoptionResponseAssembler assembler=new AdoptionResponseAssembler(animals,images,records,access);Animal animal=animal();when(animals.selectById(5001L)).thenReturn(animal);UserPrincipal viewer=new UserPrincipal(1002L,"USER");when(access.isVisible(viewer,animal)).thenReturn(false);when(records.existsByAnimalAndUser(5001L,1002L)).thenReturn(0);
        var out=assembler.detail(app(1002L),viewer);assertNull(out.getAnimal().getCoverImageUrl());verify(images,never()).selectCover(5001L);
    }
    @Test void finalAdopterKeepsCoverUrlAfterAnimalIsAdopted(){
        AnimalMapper animals=mock(AnimalMapper.class);AnimalImageMapper images=mock(AnimalImageMapper.class);AdoptionRecordMapper records=mock(AdoptionRecordMapper.class);AnimalAccessService access=mock(AnimalAccessService.class);
        AdoptionResponseAssembler assembler=new AdoptionResponseAssembler(animals,images,records,access);Animal animal=animal();when(animals.selectById(5001L)).thenReturn(animal);UserPrincipal viewer=new UserPrincipal(1001L,"USER");when(access.isVisible(viewer,animal)).thenReturn(false);when(records.existsByAnimalAndUser(5001L,1001L)).thenReturn(1);AnimalImage cover=new AnimalImage();cover.setId(5101L);when(images.selectCover(5001L)).thenReturn(cover);
        var out=assembler.detail(app(1001L),viewer);assertEquals("/api/media/animal-images/5101",out.getAnimal().getCoverImageUrl());
    }
    private Animal animal(){Animal a=new Animal();a.setId(5001L);a.setName("Milo");a.setSpecies("CAT");a.setSex("MALE");a.setStatus("ADOPTED");return a;}
    private AdoptionApplication app(Long user){AdoptionApplication a=new AdoptionApplication();a.setId(6001L);a.setUserId(user);a.setAnimalId(5001L);a.setStatus("INVALIDATED");a.setAdoptionReason("r");a.setHousingCondition("h");a.setFamilyMembers("f");a.setPetExperience("p");a.setContact("c");return a;}
}

