package com.petlink.modules.animal;

import com.petlink.common.BusinessException;
import com.petlink.common.ErrorCode;
import com.petlink.infrastructure.file.service.FileStorageService;
import com.petlink.modules.adoption.mapper.AdoptionRecordMapper;
import com.petlink.modules.animal.entity.Animal;
import com.petlink.modules.animal.entity.AnimalImage;
import com.petlink.modules.animal.mapper.AnimalImageMapper;
import com.petlink.modules.animal.mapper.AnimalMapper;
import com.petlink.modules.animal.service.AnimalAccessService;
import com.petlink.modules.animal.service.AnimalMediaService;
import com.petlink.security.UserPrincipal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AnimalMediaServiceTest {
    @Test void visitorCanReadAvailableAnimalImage() {
        Fixture f=new Fixture(); f.animal.setStatus("AVAILABLE"); when(f.access.isVisible(null,f.animal)).thenReturn(true);
        FileStorageService.ImageBinary expected=new FileStorageService.ImageBinary(new byte[]{1},"image/png"); when(f.storage.readImage("animals/1/x.png")).thenReturn(expected);
        assertSame(expected,f.service.read(null,11L));
    }
    @Test void finalAdopterCanReadAdoptedAnimalImage() {
        Fixture f=new Fixture(); f.animal.setStatus("ADOPTED"); UserPrincipal user=new UserPrincipal(7L,"USER");
        when(f.access.isVisible(user,f.animal)).thenReturn(false); when(f.adoptions.existsByAnimalAndUser(1L,7L)).thenReturn(1);
        FileStorageService.ImageBinary expected=new FileStorageService.ImageBinary(new byte[]{1},"image/png"); when(f.storage.readImage(anyString())).thenReturn(expected);
        assertSame(expected,f.service.read(user,11L));
    }
    @Test void nonFinalHistoricalUserCannotReadAdoptedImage() {
        Fixture f=new Fixture(); UserPrincipal user=new UserPrincipal(8L,"USER"); when(f.access.isVisible(user,f.animal)).thenReturn(false);
        when(f.adoptions.existsByAnimalAndUser(1L,8L)).thenReturn(0);
        BusinessException ex=assertThrows(BusinessException.class,()->f.service.read(user,11L));
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND,ex.getErrorCode()); verifyNoInteractions(f.storage);
    }
    @Test void missingImageIs404() {
        AnimalImageMapper images=mock(AnimalImageMapper.class); AnimalMediaService service=new AnimalMediaService(images,mock(AnimalMapper.class),mock(AnimalAccessService.class),mock(AdoptionRecordMapper.class),mock(FileStorageService.class));
        BusinessException ex=assertThrows(BusinessException.class,()->service.read(null,999L)); assertEquals(ErrorCode.RESOURCE_NOT_FOUND,ex.getErrorCode());
    }
    private static class Fixture {
        final AnimalImageMapper images=mock(AnimalImageMapper.class); final AnimalMapper animals=mock(AnimalMapper.class);
        final AnimalAccessService access=mock(AnimalAccessService.class); final AdoptionRecordMapper adoptions=mock(AdoptionRecordMapper.class);
        final FileStorageService storage=mock(FileStorageService.class); final AnimalMediaService service=new AnimalMediaService(images,animals,access,adoptions,storage);
        final Animal animal=new Animal();
        Fixture(){AnimalImage image=new AnimalImage();image.setId(11L);image.setAnimalId(1L);image.setImagePath("animals/1/x.png");animal.setId(1L);animal.setStatus("ADOPTED");when(images.selectById(11L)).thenReturn(image);when(animals.selectById(1L)).thenReturn(animal);}
    }
}
