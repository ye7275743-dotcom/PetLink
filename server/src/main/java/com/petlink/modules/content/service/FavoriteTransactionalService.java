package com.petlink.modules.content.service;

import com.petlink.common.BusinessException;
import com.petlink.common.ErrorCode;
import com.petlink.modules.animal.entity.Animal;
import com.petlink.modules.animal.mapper.AnimalMapper;
import com.petlink.modules.content.entity.Favorite;
import com.petlink.modules.content.mapper.FavoriteMapper;
import com.petlink.security.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FavoriteTransactionalService {
    private final FavoriteMapper favoriteMapper;
    private final AnimalMapper animalMapper;
    public FavoriteTransactionalService(FavoriteMapper favoriteMapper,AnimalMapper animalMapper){this.favoriteMapper=favoriteMapper;this.animalMapper=animalMapper;}

    @Transactional
    public Favorite create(UserPrincipal principal,Long animalId){
        FavoriteQueryService.requireMember(principal);FavoriteQueryService.requireId(animalId);
        Animal animal=animalMapper.selectForShare(animalId);
        if(animal==null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        if(!"AVAILABLE".equals(animal.getStatus())) throw new BusinessException(ErrorCode.BUSINESS_STATE_CONFLICT);
        Favorite favorite=new Favorite();favorite.setUserId(principal.getUserId());favorite.setAnimalId(animalId);
        if(favoriteMapper.insert(favorite)!=1) throw new IllegalStateException("favorite insert affected rows != 1");
        Favorite saved=favoriteMapper.selectById(favorite.getId());
        if(saved==null) throw new IllegalStateException("new favorite cannot be reloaded");
        return saved;
    }

    @Transactional
    public void delete(UserPrincipal principal,Long animalId){
        FavoriteQueryService.requireMember(principal);FavoriteQueryService.requireId(animalId);
        favoriteMapper.deleteByUserAndAnimal(principal.getUserId(),animalId);
    }
}
