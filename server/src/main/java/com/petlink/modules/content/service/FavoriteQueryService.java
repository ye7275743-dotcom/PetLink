package com.petlink.modules.content.service;

import com.petlink.common.BusinessException;
import com.petlink.common.ErrorCode;
import com.petlink.common.PageResponse;
import com.petlink.modules.content.entity.Favorite;
import com.petlink.modules.content.mapper.FavoriteMapper;
import com.petlink.modules.content.vo.FavoriteAnimalResponse;
import com.petlink.security.UserPrincipal;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FavoriteQueryService {
    private final FavoriteMapper mapper;
    private final FavoriteResponseAssembler assembler;
    public FavoriteQueryService(FavoriteMapper mapper,FavoriteResponseAssembler assembler){this.mapper=mapper;this.assembler=assembler;}

    public Favorite find(UserPrincipal principal,Long animalId){
        requireMember(principal);requireId(animalId);
        return mapper.selectByUserAndAnimal(principal.getUserId(),animalId);
    }

    public FavoriteAnimalResponse response(UserPrincipal principal,Favorite favorite){return assembler.assemble(principal,favorite);}

    public PageResponse<FavoriteAnimalResponse> mine(UserPrincipal principal,int page,int size){
        requireMember(principal);validatePage(page,size);
        long offset=(long)(page-1)*size;
        List<FavoriteAnimalResponse> records=mapper.selectUserPage(principal.getUserId(),size,offset).stream()
                .map(f->assembler.assemble(principal,f)).collect(Collectors.toList());
        return new PageResponse<>(records,page,size,mapper.countByUser(principal.getUserId()));
    }

    static void requireMember(UserPrincipal p){if(p==null||!("USER".equals(p.getRoleCode())||"RESCUER".equals(p.getRoleCode()))) throw new BusinessException(ErrorCode.FORBIDDEN);}
    static void requireId(Long id){if(id==null||id<=0) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);}
    private void validatePage(int page,int size){if(page<1||size<1||size>100) throw new BusinessException(ErrorCode.INVALID_PARAMETER);}
}
