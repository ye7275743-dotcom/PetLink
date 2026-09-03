package com.petlink.modules.animal;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.petlink.common.BusinessException;
import com.petlink.common.ErrorCode;
import com.petlink.infrastructure.audit.service.OperationLogService;
import com.petlink.infrastructure.file.service.FileStorageService;
import com.petlink.modules.animal.dto.AddHealthRecordRequest;
import com.petlink.modules.animal.dto.AnimalStatusActionRequest;
import com.petlink.modules.animal.dto.AppendAnimalImagesRequest;
import com.petlink.modules.animal.dto.UpdateAnimalRequest;
import com.petlink.modules.animal.entity.Animal;
import com.petlink.modules.animal.entity.AnimalImage;
import com.petlink.modules.animal.entity.HealthRecord;
import com.petlink.modules.animal.mapper.AnimalImageMapper;
import com.petlink.modules.animal.mapper.AnimalMapper;
import com.petlink.modules.animal.mapper.HealthRecordMapper;
import com.petlink.modules.animal.service.AnimalAccessService;
import com.petlink.modules.animal.service.AnimalFileBindingService;
import com.petlink.modules.animal.service.AnimalResponseAssembler;
import com.petlink.modules.animal.service.AnimalTransactionalService;
import com.petlink.modules.animal.vo.AnimalDetailResponse;
import com.petlink.modules.animal.vo.HealthRecordResponse;
import com.petlink.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AnimalTransactionalServiceTest {
    AnimalMapper animals; AnimalImageMapper images; HealthRecordMapper health; AnimalAccessService access;
    AnimalResponseAssembler assembler; AnimalFileBindingService binding; FileStorageService storage; OperationLogService logs;
    AnimalTransactionalService service; UserPrincipal rescuer=new UserPrincipal(100L,"RESCUER");

    @BeforeEach void setUp(){
        animals=mock(AnimalMapper.class);images=mock(AnimalImageMapper.class);health=mock(HealthRecordMapper.class);
        access=mock(AnimalAccessService.class);assembler=mock(AnimalResponseAssembler.class);binding=mock(AnimalFileBindingService.class);
        storage=mock(FileStorageService.class);logs=mock(OperationLogService.class);
        service=new AnimalTransactionalService(animals,images,health,access,assembler,binding,storage,logs);
    }

    @Test void equalPatchDoesNotExecuteUpdate() {
        Animal a=animal("TREATING",3); when(animals.selectById(1L)).thenReturn(a); AnimalDetailResponse expected=mock(AnimalDetailResponse.class);when(assembler.detail(a,true)).thenReturn(expected);
        UpdateAnimalRequest r=new UpdateAnimalRequest();r.setName("Cat");r.setVersion(3);
        assertSame(expected,service.update(rescuer,1L,r)); verify(animals,never()).update(isNull(),any(Wrapper.class));
    }

    @Test void changedPatchUsesOptimisticVersionAndChangedColumn() {
        Animal a=animal("TREATING",3); when(animals.selectById(1L)).thenReturn(a,a);when(animals.update(isNull(),any(Wrapper.class))).thenReturn(1);when(assembler.detail(any(),eq(true))).thenReturn(mock(AnimalDetailResponse.class));
        UpdateAnimalRequest r=new UpdateAnimalRequest();r.setName("New Cat");r.setSpecies("CAT");r.setVersion(3);
        service.update(rescuer,1L,r);
        @SuppressWarnings("unchecked") org.mockito.ArgumentCaptor<Wrapper<Animal>> cap=org.mockito.ArgumentCaptor.forClass(Wrapper.class);
        verify(animals).update(isNull(),cap.capture());
        UpdateWrapper<Animal> w=(UpdateWrapper<Animal>)cap.getValue(); String set=w.getSqlSet();
        assertTrue(set.contains("name")); assertFalse(set.contains("species")); assertTrue(set.contains("version = version + 1"));
    }

    @Test void stalePatchVersionIs40903() {
        Animal a=animal("TREATING",4);when(animals.selectById(1L)).thenReturn(a);UpdateAnimalRequest r=new UpdateAnimalRequest();r.setName("X");r.setVersion(3);
        BusinessException ex=assertThrows(BusinessException.class,()->service.update(rescuer,1L,r));assertEquals(ErrorCode.OPTIMISTIC_LOCK_CONFLICT,ex.getErrorCode());
    }

    @Test void appendImagesStartsAtMaxSortPlusOne() {
        Animal a=animal("TREATING",1);when(animals.selectForUpdate(1L)).thenReturn(a);when(images.maxSortOrder(1L)).thenReturn(4);when(animals.selectById(1L)).thenReturn(a);
        AnimalFileBindingService.PreparedBindings prepared=mock(AnimalFileBindingService.PreparedBindings.class);List<String> tokens=List.of("00000000-0000-0000-0000-000000000001");when(binding.lockAndValidate(100L,tokens)).thenReturn(prepared);when(assembler.detail(a,true)).thenReturn(mock(AnimalDetailResponse.class));
        AppendAnimalImagesRequest r=new AppendAnimalImagesRequest();r.setImageTokens(tokens);service.addImages(rescuer,1L,r);
        verify(binding).bindPreparedStartingAt(100L,prepared,1L,tokens,5);verify(binding).copyPrepared(prepared);
    }

    @Test void deleteImageReordersFollowingRowsInAscendingOrder() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) TransactionSynchronizationManager.initSynchronization();
        try {
            Animal a=animal("TREATING",1);when(animals.selectForUpdate(1L)).thenReturn(a);when(animals.selectById(1L)).thenReturn(a);
            AnimalImage target=image(10L,3);AnimalImage four=image(11L,4);AnimalImage five=image(12L,5);when(images.selectById(10L)).thenReturn(target);when(images.selectAfter(1L,3)).thenReturn(List.of(four,five));when(images.deleteById(10L)).thenReturn(1);when(images.moveSortOrder(anyLong(),anyInt(),anyInt())).thenReturn(1);when(assembler.detail(a,true)).thenReturn(mock(AnimalDetailResponse.class));
            service.deleteImage(rescuer,1L,10L);
            var order=inOrder(images);order.verify(images).moveSortOrder(11L,4,3);order.verify(images).moveSortOrder(12L,5,4);
        } finally { TransactionSynchronizationManager.clearSynchronization(); }
    }

    @Test void addHealthRecordIsAppendOnlyAndDoesNotUpdateAnimalSummary() {
        Animal a=animal("TREATING",1);when(animals.selectForUpdate(1L)).thenReturn(a);when(health.insert(any())).thenAnswer(inv->{HealthRecord r=inv.getArgument(0);r.setId(50L);return 1;});when(health.selectById(50L)).thenAnswer(inv->{HealthRecord r=new HealthRecord();r.setId(50L);r.setAnimalId(1L);r.setRecorderId(100L);r.setContent("better");r.setCreatedAt(LocalDateTime.now());return r;});when(assembler.fullHealth(any())).thenReturn(mock(HealthRecordResponse.class));
        AddHealthRecordRequest r=new AddHealthRecordRequest();r.setContent(" better ");service.addHealthRecord(rescuer,1L,r);
        verify(health).insert(argThat(x->"better".equals(x.getContent())&&Long.valueOf(100L).equals(x.getRecorderId())));verify(animals,never()).update(any(),any());
    }

    @Test void suspendUsesReasonAndFrozenLog() {
        Animal a=animal("AVAILABLE",5);Animal saved=animal("SUSPENDED",6);when(animals.selectById(1L)).thenReturn(a,saved);when(animals.update(isNull(),any(Wrapper.class))).thenReturn(1);when(assembler.detail(saved,true)).thenReturn(mock(AnimalDetailResponse.class));
        AnimalStatusActionRequest r=new AnimalStatusActionRequest();r.setAction("SUSPEND_ADOPTION");r.setSuspendReason(" treatment ");r.setVersion(5);service.statusAction(rescuer,1L,r);
        verify(logs).append("ANIMAL",1L,"SUSPEND_ADOPTION","AVAILABLE","SUSPENDED",100L,"treatment");
    }

    @Test void invalidStateActionReturns40901BeforeVersionConflict() {
        Animal a=animal("TREATING",7);when(animals.selectById(1L)).thenReturn(a);AnimalStatusActionRequest r=new AnimalStatusActionRequest();r.setAction("OPEN_ADOPTION");r.setVersion(6);
        BusinessException ex=assertThrows(BusinessException.class,()->service.statusAction(rescuer,1L,r));assertEquals(ErrorCode.BUSINESS_STATE_CONFLICT,ex.getErrorCode());
    }

    @Test void staleStateVersionReturns40903() {
        Animal a=animal("OBSERVING",7);when(animals.selectById(1L)).thenReturn(a);AnimalStatusActionRequest r=new AnimalStatusActionRequest();r.setAction("OPEN_ADOPTION");r.setVersion(6);
        BusinessException ex=assertThrows(BusinessException.class,()->service.statusAction(rescuer,1L,r));assertEquals(ErrorCode.OPTIMISTIC_LOCK_CONFLICT,ex.getErrorCode());
    }

    @Test void reasonFieldIsForbiddenForNonSuspendActionsEvenWhenNull() {
        AnimalStatusActionRequest r=new AnimalStatusActionRequest();r.setAction("TO_OBSERVING");r.setVersion(1);r.setSuspendReason(null);
        BusinessException ex=assertThrows(BusinessException.class,()->service.statusAction(rescuer,1L,r));assertEquals(ErrorCode.INVALID_PARAMETER,ex.getErrorCode());verifyNoInteractions(animals);
    }

    @Test void blankColorIsInvalidWhenFieldIsPresent() {
        Animal a=animal("TREATING",3); when(animals.selectById(1L)).thenReturn(a);
        UpdateAnimalRequest r=new UpdateAnimalRequest(); r.setColor("   "); r.setVersion(3);
        BusinessException ex=assertThrows(BusinessException.class,()->service.update(rescuer,1L,r));
        assertEquals(ErrorCode.INVALID_PARAMETER,ex.getErrorCode());
        verify(animals,never()).update(isNull(),any(Wrapper.class));
    }

    private Animal animal(String status,int version){Animal a=new Animal();a.setId(1L);a.setRescueTaskId(20L);a.setName("Cat");a.setSpecies("CAT");a.setSex("UNKNOWN");a.setHealthCondition("ok");a.setStatus(status);a.setVersion(version);a.setCreatedAt(LocalDateTime.now());a.setUpdatedAt(LocalDateTime.now());return a;}
    private AnimalImage image(Long id,int sort){AnimalImage i=new AnimalImage();i.setId(id);i.setAnimalId(1L);i.setSortOrder(sort);i.setImagePath("animals/1/"+id+".png");return i;}
}
