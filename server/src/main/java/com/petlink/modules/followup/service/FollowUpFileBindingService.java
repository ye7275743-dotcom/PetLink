package com.petlink.modules.followup.service;

import com.petlink.common.BusinessException;
import com.petlink.common.ErrorCode;
import com.petlink.common.TimeUtils;
import com.petlink.infrastructure.file.entity.TemporaryFile;
import com.petlink.infrastructure.file.mapper.TemporaryFileMapper;
import com.petlink.infrastructure.file.service.BoundTemporaryFileCleanupService;
import com.petlink.infrastructure.file.service.FileStorageService;
import com.petlink.modules.followup.entity.FollowUpImage;
import com.petlink.modules.followup.mapper.FollowUpImageMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class FollowUpFileBindingService {
    private static final Logger log=LoggerFactory.getLogger(FollowUpFileBindingService.class);
    private static final long MAX_SIZE=5L*1024*1024;
    private final TemporaryFileMapper temporaryFileMapper;
    private final FollowUpImageMapper imageMapper;
    private final FileStorageService storage;
    private final BoundTemporaryFileCleanupService cleanupService;

    public FollowUpFileBindingService(TemporaryFileMapper temporaryFileMapper,FollowUpImageMapper imageMapper,
                                      FileStorageService storage,BoundTemporaryFileCleanupService cleanupService){
        this.temporaryFileMapper=temporaryFileMapper;this.imageMapper=imageMapper;this.storage=storage;this.cleanupService=cleanupService;
    }

    public PreparedBindings lockAndValidate(Long ownerId,List<String> tokens){
        if(!TransactionSynchronizationManager.isSynchronizationActive()) throw new IllegalStateException("Transaction synchronization is required for follow-up file binding");
        PreparedBindings prepared=new PreparedBindings(new HashMap<>(),new ArrayList<>(),new ArrayList<>());
        registerSynchronization(prepared);
        if(tokens==null||tokens.isEmpty()) return prepared;
        List<TemporaryFile> locked=temporaryFileMapper.selectForUpdateByTokens(tokens);
        if(locked.size()!=tokens.size()) throw new BusinessException(ErrorCode.INVALID_FILE);
        LocalDateTime now=LocalDateTime.now(TimeUtils.ZONE);
        for(TemporaryFile file:locked){validate(file,ownerId,now);prepared.byToken.put(file.getToken(),file);}
        if(prepared.byToken.size()!=tokens.size()) throw new BusinessException(ErrorCode.INVALID_FILE);
        return prepared;
    }

    public void bindPrepared(Long ownerId,Long followUpId,List<String> tokens,PreparedBindings prepared){
        LocalDateTime now=LocalDateTime.now(TimeUtils.ZONE); int sort=1;
        for(String token:tokens){
            TemporaryFile temp=prepared.byToken.get(token); if(temp==null) throw new BusinessException(ErrorCode.INVALID_FILE);
            String formalPath=buildFormalPath(followUpId,temp.getFileExtension());
            FollowUpImage image=new FollowUpImage(); image.setFollowUpId(followUpId); image.setImagePath(formalPath); image.setSortOrder(sort++);
            if(imageMapper.insert(image)!=1) throw new IllegalStateException("follow_up_image insert affected rows != 1");
            int rows=temporaryFileMapper.bindUploaded(temp.getId(),ownerId,"FOLLOW_UP",followUpId,formalPath,now);
            if(rows!=1) throw new BusinessException(ErrorCode.BUSINESS_STATE_CONFLICT,"临时文件已失效或被其他业务使用");
            prepared.boundFiles.add(new BoundFile(temp.getId(),temp.getTempPath(),followUpId,formalPath));
        }
    }

    public void copyPrepared(PreparedBindings prepared){
        for(BoundFile file:prepared.boundFiles){
            prepared.createdFormalPaths.add(file.formalPath);
            storage.copyTemporaryToFormal(file.tempPath,file.formalPath);
        }
    }

    private void validate(TemporaryFile file,Long ownerId,LocalDateTime now){
        if(file==null||ownerId==null||!ownerId.equals(file.getOwnerId())||!"UPLOADED".equals(file.getStatus())
                ||file.getExpiresAt()==null||!file.getExpiresAt().isAfter(now)||file.getFileSizeBytes()==null||file.getFileSizeBytes()<=0||file.getFileSizeBytes()>MAX_SIZE)
            throw new BusinessException(ErrorCode.INVALID_FILE);
        String ext=file.getFileExtension(),mime=file.getMimeType();
        boolean ok=(("jpg".equals(ext)||"jpeg".equals(ext))&&"image/jpeg".equals(mime))||("png".equals(ext)&&"image/png".equals(mime));
        if(!ok) throw new BusinessException(ErrorCode.INVALID_FILE);
    }

    private String buildFormalPath(Long followUpId,String extension){
        return "follow-ups/"+followUpId+"/"+UUID.randomUUID().toString().toLowerCase(Locale.ROOT)+"."+extension;
    }

    private void registerSynchronization(PreparedBindings prepared){
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){
            @Override public void afterCommit(){
                for(BoundFile file:prepared.boundFiles){
                    try{cleanupService.cleanupOne(file.temporaryFileId,file.tempPath,"FOLLOW_UP",file.followUpId,file.formalPath);}
                    catch(RuntimeException ex){log.error("Post-commit follow-up temporary cleanup callback failed: tempId={}",file.temporaryFileId,ex);}
                }
            }
            @Override public void afterCompletion(int status){
                if(status!=STATUS_COMMITTED) for(String path:prepared.createdFormalPaths) storage.deleteQuietly(path,"follow-up creation transaction rollback");
            }
        });
    }

    public static class PreparedBindings {
        private final Map<String,TemporaryFile> byToken; private final List<BoundFile> boundFiles; private final List<String> createdFormalPaths;
        private PreparedBindings(Map<String,TemporaryFile> byToken,List<BoundFile> boundFiles,List<String> createdFormalPaths){this.byToken=byToken;this.boundFiles=boundFiles;this.createdFormalPaths=createdFormalPaths;}
    }
    private static class BoundFile {
        private final Long temporaryFileId; private final String tempPath; private final Long followUpId; private final String formalPath;
        private BoundFile(Long temporaryFileId,String tempPath,Long followUpId,String formalPath){this.temporaryFileId=temporaryFileId;this.tempPath=tempPath;this.followUpId=followUpId;this.formalPath=formalPath;}
    }
}
