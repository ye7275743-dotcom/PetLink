package com.petlink.modules.animal.service;

import com.petlink.common.PageResponse;
import com.petlink.modules.animal.dto.AddHealthRecordRequest;
import com.petlink.modules.animal.dto.AnimalStatusActionRequest;
import com.petlink.modules.animal.dto.AppendAnimalImagesRequest;
import com.petlink.modules.animal.dto.UpdateAnimalRequest;
import com.petlink.modules.animal.vo.AnimalDetailResponse;
import com.petlink.modules.animal.vo.AnimalSummaryResponse;
import com.petlink.modules.animal.vo.HealthRecordPublicResponse;
import com.petlink.modules.animal.vo.HealthRecordResponse;
import com.petlink.security.UserPrincipal;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AnimalService {
    private final AnimalQueryService query;
    private final AnimalTransactionalService tx;
    public AnimalService(AnimalQueryService query,AnimalTransactionalService tx){this.query=query;this.tx=tx;}
    public PageResponse<AnimalSummaryResponse> publicList(int p,int s,String species,String sex){return query.publicList(p,s,species,sex);}
    public AnimalDetailResponse detail(UserPrincipal principal,Long id){return query.detail(principal,id);}
    public PageResponse<AnimalSummaryResponse> responsible(UserPrincipal principal,int p,int s,String status){return query.responsible(principal,p,s,status);}
    public AnimalDetailResponse update(UserPrincipal principal,Long id,UpdateAnimalRequest r){return tx.update(principal,id,r);}
    public AnimalDetailResponse addImages(UserPrincipal principal,Long id,AppendAnimalImagesRequest r){return tx.addImages(principal,id,r);}
    public AnimalDetailResponse deleteImage(UserPrincipal principal,Long id,Long imageId){return tx.deleteImage(principal,id,imageId);}
    public HealthRecordResponse addHealthRecord(UserPrincipal principal,Long id,AddHealthRecordRequest r){return tx.addHealthRecord(principal,id,r);}
    public List<HealthRecordPublicResponse> healthRecords(UserPrincipal principal,Long id){return query.healthRecords(principal,id);}
    public AnimalDetailResponse statusAction(UserPrincipal principal,Long id,AnimalStatusActionRequest r){return tx.statusAction(principal,id,r);}
}
