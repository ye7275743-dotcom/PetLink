package com.petlink.modules.animal;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AnimalFileBindingAppendTest {
    @AfterEach void cleanup(){if(TransactionSynchronizationManager.isSynchronizationActive())TransactionSynchronizationManager.clearSynchronization();}

    @Test void appendStartsAtProvidedMaxPlusOne() {
        TemporaryFileMapper tempMapper=mock(TemporaryFileMapper.class); AnimalImageMapper imageMapper=mock(AnimalImageMapper.class);
        AnimalFileBindingService service=new AnimalFileBindingService(tempMapper,imageMapper,mock(FileStorageService.class),mock(BoundTemporaryFileCleanupService.class));
        TransactionSynchronizationManager.initSynchronization();
        String t1="00000000-0000-0000-0000-000000000011", t2="00000000-0000-0000-0000-000000000012";
        TemporaryFile f1=temp(11L,t1), f2=temp(12L,t2);
        when(tempMapper.selectForUpdateByTokens(List.of(t1,t2))).thenReturn(List.of(f1,f2));
        when(imageMapper.insert(any())).thenReturn(1); when(tempMapper.bindUploaded(anyLong(),eq(2001L),eq("ANIMAL"),eq(5001L),anyString(),any())).thenReturn(1);
        AnimalFileBindingService.PreparedBindings prepared=service.lockAndValidate(2001L,List.of(t1,t2));
        service.bindPreparedStartingAt(2001L,prepared,5001L,List.of(t1,t2),5);
        var captor=org.mockito.ArgumentCaptor.forClass(AnimalImage.class); verify(imageMapper,times(2)).insert(captor.capture());
        assertEquals(5,captor.getAllValues().get(0).getSortOrder()); assertEquals(6,captor.getAllValues().get(1).getSortOrder());
    }

    private TemporaryFile temp(Long id,String token){
        TemporaryFile f=new TemporaryFile(); f.setId(id); f.setToken(token); f.setOwnerId(2001L); f.setStatus("UPLOADED");
        f.setExpiresAt(LocalDateTime.now().plusMinutes(30)); f.setFileSizeBytes(100L); f.setFileExtension("png"); f.setMimeType("image/png"); f.setTempPath("tmp/"+id+".png"); return f;
    }
}
