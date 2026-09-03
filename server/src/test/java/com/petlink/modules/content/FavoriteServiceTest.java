package com.petlink.modules.content;

import com.petlink.modules.content.entity.Favorite;
import com.petlink.modules.content.service.FavoriteQueryService;
import com.petlink.modules.content.service.FavoriteService;
import com.petlink.modules.content.service.FavoriteTransactionalService;
import com.petlink.modules.content.vo.FavoriteAnimalResponse;
import com.petlink.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FavoriteServiceTest {
    FavoriteQueryService query;FavoriteTransactionalService tx;FavoriteService service;UserPrincipal user=new UserPrincipal(10L,"USER");Favorite favorite;FavoriteAnimalResponse response;
    @BeforeEach void setUp(){query=mock(FavoriteQueryService.class);tx=mock(FavoriteTransactionalService.class);service=new FavoriteService(query,tx);favorite=new Favorite();favorite.setId(8L);response=mock(FavoriteAnimalResponse.class);}
    @Test void existingFavoriteReturns200SemanticsWithoutAnimalRecheck(){when(query.find(user,5L)).thenReturn(favorite);when(query.response(user,favorite)).thenReturn(response);var r=service.favorite(user,5L);assertFalse(r.isCreated());assertSame(response,r.getResponse());verifyNoInteractions(tx);}
    @Test void newFavoriteReturns201Semantics(){when(query.find(user,5L)).thenReturn(null);when(tx.create(user,5L)).thenReturn(favorite);when(query.response(user,favorite)).thenReturn(response);assertTrue(service.favorite(user,5L).isCreated());}
    @Test void namedUniqueRaceRecoversExistingFavorite(){Favorite concurrent=new Favorite();concurrent.setId(9L);when(query.find(user,5L)).thenReturn(null,concurrent);when(tx.create(user,5L)).thenThrow(new DataIntegrityViolationException("Duplicate key uk_favorite_user_animal"));when(query.response(user,concurrent)).thenReturn(response);var r=service.favorite(user,5L);assertFalse(r.isCreated());assertSame(response,r.getResponse());}
    @Test void unrelatedConstraintIsNotHidden(){when(query.find(user,5L)).thenReturn(null);when(tx.create(user,5L)).thenThrow(new DataIntegrityViolationException("fk_favorite_user"));assertThrows(DataIntegrityViolationException.class,()->service.favorite(user,5L));}
    @Test void namedUniqueWithoutRecoverableRowRethrows(){when(query.find(user,5L)).thenReturn(null);when(tx.create(user,5L)).thenThrow(new DataIntegrityViolationException("uk_favorite_user_animal"));assertThrows(DataIntegrityViolationException.class,()->service.favorite(user,5L));}
    @Test void unfavoriteDelegatesAndReturnsFinalFalseState(){var r=service.unfavorite(user,5L);verify(tx).delete(user,5L);assertEquals("5",r.getAnimalId());assertFalse(r.isFavorited());}
}
