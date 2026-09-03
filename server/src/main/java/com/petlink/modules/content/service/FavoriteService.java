package com.petlink.modules.content.service;

import com.petlink.common.PageResponse;
import com.petlink.modules.content.entity.Favorite;
import com.petlink.modules.content.vo.FavoriteAnimalResponse;
import com.petlink.modules.content.vo.FavoriteStateResponse;
import com.petlink.security.UserPrincipal;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class FavoriteService {
    private final FavoriteQueryService query;
    private final FavoriteTransactionalService tx;
    public FavoriteService(FavoriteQueryService query,FavoriteTransactionalService tx){this.query=query;this.tx=tx;}

    public FavoriteResult favorite(UserPrincipal principal,Long animalId){
        Favorite existing=query.find(principal,animalId);
        if(existing!=null) return new FavoriteResult(false,query.response(principal,existing));
        try {
            Favorite created=tx.create(principal,animalId);
            return new FavoriteResult(true,query.response(principal,created));
        } catch(DataIntegrityViolationException ex){
            if(!hasConstraint(ex,"uk_favorite_user_animal")) throw ex;
            Favorite concurrent=query.find(principal,animalId);
            if(concurrent==null) throw ex;
            return new FavoriteResult(false,query.response(principal,concurrent));
        }
    }

    public FavoriteStateResponse unfavorite(UserPrincipal principal,Long animalId){tx.delete(principal,animalId);return new FavoriteStateResponse(String.valueOf(animalId),false);}
    public PageResponse<FavoriteAnimalResponse> mine(UserPrincipal principal,int page,int size){return query.mine(principal,page,size);}
    private boolean hasConstraint(Throwable ex,String name){String needle=name.toLowerCase(Locale.ROOT);for(Throwable t=ex;t!=null;t=t.getCause()){String m=t.getMessage();if(m!=null&&m.toLowerCase(Locale.ROOT).contains(needle))return true;}return false;}

    public static class FavoriteResult {
        private final boolean created; private final FavoriteAnimalResponse response;
        public FavoriteResult(boolean created,FavoriteAnimalResponse response){this.created=created;this.response=response;}
        public boolean isCreated(){return created;} public FavoriteAnimalResponse getResponse(){return response;}
    }
}
