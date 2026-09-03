package com.petlink.modules.content;

import com.petlink.modules.adoption.mapper.AdoptionRecordMapper;
import com.petlink.modules.animal.entity.Animal;
import com.petlink.modules.animal.entity.AnimalImage;
import com.petlink.modules.animal.mapper.AnimalImageMapper;
import com.petlink.modules.animal.mapper.AnimalMapper;
import com.petlink.modules.animal.service.AnimalAccessService;
import com.petlink.modules.content.entity.Favorite;
import com.petlink.modules.content.service.FavoriteResponseAssembler;
import com.petlink.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FavoriteResponseAssemblerTest {
    AnimalMapper animals;AnimalImageMapper images;AnimalAccessService access;AdoptionRecordMapper adoptions;FavoriteResponseAssembler assembler;UserPrincipal user=new UserPrincipal(10L,"USER");
    @BeforeEach void setUp(){animals=mock(AnimalMapper.class);images=mock(AnimalImageMapper.class);access=mock(AnimalAccessService.class);adoptions=mock(AdoptionRecordMapper.class);assembler=new FavoriteResponseAssembler(animals,images,access,adoptions);when(animals.selectById(5L)).thenReturn(animal());}
    @Test void visibleAnimalReturnsCoverUrl(){when(images.selectCover(5L)).thenReturn(image());when(access.isVisible(eq(user),any())).thenReturn(true);assertEquals("/api/media/animal-images/7",assembler.assemble(user,favorite()).getAnimal().getCoverImageUrl());}
    @Test void hiddenAnimalDoesNotGainMediaFromFavorite(){when(images.selectCover(5L)).thenReturn(image());when(access.isVisible(eq(user),any())).thenReturn(false);when(adoptions.existsByAnimalAndUser(5L,10L)).thenReturn(0);assertNull(assembler.assemble(user,favorite()).getAnimal().getCoverImageUrl());}
    @Test void finalAdopterCanReceiveCover(){when(images.selectCover(5L)).thenReturn(image());when(adoptions.existsByAnimalAndUser(5L,10L)).thenReturn(1);assertEquals("/api/media/animal-images/7",assembler.assemble(user,favorite()).getAnimal().getCoverImageUrl());}
    @Test void noCoverAlwaysReturnsNull(){assertNull(assembler.assemble(user,favorite()).getAnimal().getCoverImageUrl());}
    @Test void responseIsLimitedProjectionWithCurrentStatus(){var r=assembler.assemble(user,favorite());assertEquals("8",r.getFavoriteId());assertEquals("5",r.getAnimal().getId());assertEquals("ADOPTED",r.getAnimal().getStatus());assertEquals("Milo",r.getAnimal().getName());}
    private Favorite favorite(){Favorite f=new Favorite();f.setId(8L);f.setAnimalId(5L);f.setUserId(10L);f.setCreatedAt(LocalDateTime.now());return f;}
    private Animal animal(){Animal a=new Animal();a.setId(5L);a.setName("Milo");a.setSpecies("CAT");a.setSex("UNKNOWN");a.setStatus("ADOPTED");a.setHealthCondition("good");return a;}
    private AnimalImage image(){AnimalImage i=new AnimalImage();i.setId(7L);i.setAnimalId(5L);return i;}
}
