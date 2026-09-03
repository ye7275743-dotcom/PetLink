package com.petlink.modules.content;

import com.petlink.common.BusinessException;
import com.petlink.common.ErrorCode;
import com.petlink.modules.animal.entity.Animal;
import com.petlink.modules.animal.mapper.AnimalMapper;
import com.petlink.modules.content.entity.Favorite;
import com.petlink.modules.content.mapper.FavoriteMapper;
import com.petlink.modules.content.service.FavoriteTransactionalService;
import com.petlink.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FavoriteTransactionalServiceTest {
    FavoriteMapper favorites;AnimalMapper animals;FavoriteTransactionalService service;
    final UserPrincipal user=new UserPrincipal(10L,"USER");
    @BeforeEach void setUp(){favorites=mock(FavoriteMapper.class);animals=mock(AnimalMapper.class);service=new FavoriteTransactionalService(favorites,animals);}
    @Test void createLocksAvailableAnimalAndInsertsAuthenticatedUser(){when(animals.selectForShare(5L)).thenReturn(animal("AVAILABLE"));when(favorites.insert(any())).thenAnswer(i->{Favorite f=i.getArgument(0);f.setId(8L);return 1;});Favorite saved=favorite();when(favorites.selectById(8L)).thenReturn(saved);assertSame(saved,service.create(user,5L));verify(animals).selectForShare(5L);verify(favorites).insert(argThat(f->f.getUserId().equals(10L)&&f.getAnimalId().equals(5L)));}
    @Test void createMissingAnimalIs404(){when(animals.selectForShare(5L)).thenReturn(null);assertEquals(ErrorCode.RESOURCE_NOT_FOUND,error(()->service.create(user,5L)));verify(favorites,never()).insert(any());}
    @Test void createUnavailableAnimalIs40901(){when(animals.selectForShare(5L)).thenReturn(animal("SUSPENDED"));assertEquals(ErrorCode.BUSINESS_STATE_CONFLICT,error(()->service.create(user,5L)));}
    @Test void createRejectsAdmin(){assertEquals(ErrorCode.FORBIDDEN,error(()->service.create(new UserPrincipal(1L,"ADMIN"),5L)));verify(animals,never()).selectForShare(anyLong());}
    @Test void deleteIsIdempotentAndDoesNotReadAnimal(){service.delete(user,5L);verify(favorites).deleteByUserAndAnimal(10L,5L);verifyNoInteractions(animals);}
    @Test void deleteInvalidIdIs404(){assertEquals(ErrorCode.RESOURCE_NOT_FOUND,error(()->service.delete(user,0L)));verifyNoInteractions(favorites,animals);}
    private Animal animal(String status){Animal a=new Animal();a.setId(5L);a.setStatus(status);return a;}
    private Favorite favorite(){Favorite f=new Favorite();f.setId(8L);f.setUserId(10L);f.setAnimalId(5L);return f;}
    private ErrorCode error(Runnable r){return assertThrows(BusinessException.class,r::run).getErrorCode();}
}
