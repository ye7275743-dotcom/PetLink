package com.petlink.modules.rescue;

import com.petlink.infrastructure.file.entity.TemporaryFile;
import com.petlink.infrastructure.file.mapper.TemporaryFileMapper;
import com.petlink.infrastructure.file.service.BoundTemporaryFileCleanupService;
import com.petlink.infrastructure.file.service.FileStorageService;
import com.petlink.modules.animal.entity.AnimalImage;
import com.petlink.modules.animal.mapper.AnimalImageMapper;
import com.petlink.modules.animal.service.AnimalFileBindingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AnimalFileBindingServiceTest {
    @AfterEach void cleanupSync(){
        if (TransactionSynchronizationManager.isSynchronizationActive()) TransactionSynchronizationManager.clearSynchronization();
    }

    @Test void eachAnimalSortOrderStartsAtOneAndClientTokenOrderIsPreserved() {
        TemporaryFileMapper tempMapper=mock(TemporaryFileMapper.class); AnimalImageMapper imageMapper=mock(AnimalImageMapper.class);
        FileStorageService storage=mock(FileStorageService.class); BoundTemporaryFileCleanupService cleanup=mock(BoundTemporaryFileCleanupService.class);
        AnimalFileBindingService service=new AnimalFileBindingService(tempMapper,imageMapper,storage,cleanup);
        TransactionSynchronizationManager.initSynchronization();

        String t1="00000000-0000-0000-0000-000000000001", t2="00000000-0000-0000-0000-000000000002", t3="00000000-0000-0000-0000-000000000003";
        TemporaryFile f1=temp(10L,t1), f2=temp(20L,t2), f3=temp(30L,t3);
        when(tempMapper.selectForUpdateByTokens(List.of(t2,t1,t3))).thenReturn(List.of(f1,f2,f3));
        when(imageMapper.insert(any())).thenReturn(1); when(tempMapper.bindUploaded(anyLong(),eq(2001L),eq("ANIMAL"),anyLong(),anyString(),any())).thenReturn(1);

        AnimalFileBindingService.PreparedBindings prepared=service.lockAndValidate(2001L,List.of(t2,t1,t3));
        service.bindPrepared(2001L,prepared,List.of(
                new AnimalFileBindingService.AnimalTokens(5001L,List.of(t2,t1)),
                new AnimalFileBindingService.AnimalTokens(5002L,List.of(t3))));

        var captor=org.mockito.ArgumentCaptor.forClass(AnimalImage.class); verify(imageMapper,times(3)).insert(captor.capture());
        List<AnimalImage> images=captor.getAllValues();
        assertEquals(5001L,images.get(0).getAnimalId()); assertEquals(1,images.get(0).getSortOrder());
        assertEquals(5001L,images.get(1).getAnimalId()); assertEquals(2,images.get(1).getSortOrder());
        assertEquals(5002L,images.get(2).getAnimalId()); assertEquals(1,images.get(2).getSortOrder());
        verify(tempMapper).selectForUpdateByTokens(List.of(t2,t1,t3));
    }

    @Test void invalidOwnerIsRejectedBeforeBinding() {
        TemporaryFileMapper tempMapper=mock(TemporaryFileMapper.class); AnimalImageMapper imageMapper=mock(AnimalImageMapper.class);
        AnimalFileBindingService service=new AnimalFileBindingService(tempMapper,imageMapper,mock(FileStorageService.class),mock(BoundTemporaryFileCleanupService.class));
        TransactionSynchronizationManager.initSynchronization();
        String token="00000000-0000-0000-0000-000000000001"; TemporaryFile file=temp(1L,token); file.setOwnerId(9999L);
        when(tempMapper.selectForUpdateByTokens(List.of(token))).thenReturn(List.of(file));
        assertThrows(com.petlink.common.BusinessException.class,()->service.lockAndValidate(2001L,List.of(token)));
        verifyNoInteractions(imageMapper);
    }

    private TemporaryFile temp(Long id,String token){
        TemporaryFile f=new TemporaryFile(); f.setId(id); f.setToken(token); f.setOwnerId(2001L); f.setStatus("UPLOADED");
        f.setExpiresAt(LocalDateTime.now().plusMinutes(30)); f.setFileSizeBytes(100L); f.setFileExtension("png"); f.setMimeType("image/png"); f.setTempPath("tmp/"+id+".png"); return f;
    }
}
