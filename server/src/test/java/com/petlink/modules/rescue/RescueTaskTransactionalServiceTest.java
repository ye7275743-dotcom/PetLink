package com.petlink.modules.rescue;

import com.petlink.common.BusinessException;
import com.petlink.common.ErrorCode;
import com.petlink.infrastructure.audit.service.OperationLogService;
import com.petlink.modules.animal.entity.Animal;
import com.petlink.modules.animal.mapper.AnimalMapper;
import com.petlink.modules.animal.mapper.HealthRecordMapper;
import com.petlink.modules.animal.service.AnimalFileBindingService;
import com.petlink.modules.clue.entity.RescueClue;
import com.petlink.modules.clue.mapper.RescueClueMapper;
import com.petlink.modules.rescue.dto.CancelRescueTaskRequest;
import com.petlink.modules.rescue.dto.FailureResolutionRequest;
import com.petlink.modules.rescue.entity.RescueRecord;
import com.petlink.modules.rescue.entity.RescueTask;
import com.petlink.modules.rescue.mapper.RescueRecordMapper;
import com.petlink.modules.rescue.mapper.RescueTaskMapper;
import com.petlink.modules.rescue.service.RescueRequestNormalizer;
import com.petlink.modules.rescue.service.RescueTaskResponseAssembler;
import com.petlink.modules.rescue.service.RescueTaskTransactionalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RescueTaskTransactionalServiceTest {
    RescueTaskMapper taskMapper; RescueRecordMapper recordMapper; RescueClueMapper clueMapper; AnimalMapper animalMapper;
    HealthRecordMapper healthMapper; AnimalFileBindingService fileBinding; OperationLogService logs; RescueTaskResponseAssembler assembler;
    RescueTaskTransactionalService service; RescueRequestNormalizer normalizer;

    @BeforeEach void setUp(){
        taskMapper=mock(RescueTaskMapper.class); recordMapper=mock(RescueRecordMapper.class); clueMapper=mock(RescueClueMapper.class);
        when(taskMapper.lockEnabledRescuer(anyLong())).thenAnswer(i->i.getArgument(0));
        animalMapper=mock(AnimalMapper.class); healthMapper=mock(HealthRecordMapper.class); fileBinding=mock(AnimalFileBindingService.class);
        logs=mock(OperationLogService.class); assembler=mock(RescueTaskResponseAssembler.class); normalizer=new RescueRequestNormalizer();
        service=new RescueTaskTransactionalService(taskMapper,recordMapper,clueMapper,animalMapper,healthMapper,fileBinding,logs,assembler,normalizer);
    }

    @Test void acceptConvertsClueCreatesWaitingTaskAndLogs(){
        when(clueMapper.acceptForRescue(3001L)).thenReturn(1);
        when(taskMapper.insert(any())).thenAnswer(i->{ RescueTask t=i.getArgument(0); t.setId(4001L); return 1; });
        RescueTask saved=task(4001L,3001L,2001L,"WAITING_START"); saved.setCreatedAt(LocalDateTime.now());
        when(taskMapper.selectById(4001L)).thenReturn(saved);
        var r=service.accept(2001L,3001L);
        assertEquals("4001",r.getTaskId()); assertEquals("WAITING_START",r.getStatus());
        verify(logs).append("RESCUE_CLUE",3001L,"ACCEPT_RESCUE","WAITING_ACCEPT","CONVERTED",2001L,null);
    }

    @Test void acceptAlreadyChangedIs409(){
        when(clueMapper.acceptForRescue(3001L)).thenReturn(0); RescueClue c=clue(3001L,"CONVERTED"); when(clueMapper.selectById(3001L)).thenReturn(c);
        assertEquals(ErrorCode.BUSINESS_STATE_CONFLICT,assertThrows(BusinessException.class,()->service.accept(2001L,3001L)).getErrorCode());
        verify(taskMapper).lockEnabledRescuer(2001L);verify(taskMapper,never()).insert(any());verifyNoInteractions(logs);
    }

    @Test void startWritesFrozenTaskLog(){
        when(taskMapper.start(eq(4001L),eq(2001L),any())).thenReturn(1); RescueTask saved=task(4001L,3001L,2001L,"IN_PROGRESS"); saved.setUpdatedAt(LocalDateTime.now()); when(taskMapper.selectById(4001L)).thenReturn(saved);
        assertEquals("IN_PROGRESS",service.start(2001L,4001L).getStatus());
        verify(logs).append("RESCUE_TASK",4001L,"START_RESCUE","WAITING_START","IN_PROGRESS",2001L,null);
    }

    @Test void addRecordLocksTaskAndRequiresInProgress(){
        when(taskMapper.selectForUpdate(4001L)).thenReturn(task(4001L,3001L,2001L,"WAITING_START"));
        assertEquals(ErrorCode.BUSINESS_STATE_CONFLICT,assertThrows(BusinessException.class,()->service.addRecord(2001L,4001L,"hello")).getErrorCode());
        verifyNoInteractions(recordMapper);
    }

    @Test void failedResultDoesNotChangeClueAndLogsReason(){
        when(taskMapper.fail(eq(4001L),eq(2001L),eq("escaped"),any())).thenReturn(1);
        RescueTask saved=task(4001L,3001L,2001L,"FAILED"); saved.setUpdatedAt(LocalDateTime.now()); when(taskMapper.selectById(4001L)).thenReturn(saved);
        var normalized=RescueRequestNormalizer.NormalizedResult.failed("escaped");
        Object out=service.submitResult(2001L,4001L,normalized);
        assertNotNull(out); verify(logs).append("RESCUE_TASK",4001L,"RESCUE_FAILED","IN_PROGRESS","FAILED",2001L,"escaped");
        verify(clueMapper,never()).closeConverted(anyLong()); verify(clueMapper,never()).reopenConverted(anyLong());
    }

    @Test void successUsesFrozenTaskThenClueLockOrderAndCreatesHealthRecord(){
        RescueTask lockedTask=task(4001L,3001L,2001L,"IN_PROGRESS"); when(taskMapper.selectForUpdate(4001L)).thenReturn(lockedTask);
        RescueClue lockedClue=clue(3001L,"CONVERTED"); when(clueMapper.selectForUpdate(3001L)).thenReturn(lockedClue);
        when(animalMapper.insert(any())).thenAnswer(i->{ Animal a=i.getArgument(0); a.setId(5001L); return 1; });
        when(healthMapper.insert(any())).thenReturn(1);
        AnimalFileBindingService.PreparedBindings prepared=mock(AnimalFileBindingService.PreparedBindings.class);
        when(fileBinding.lockAndValidate(eq(2001L),anyList())).thenReturn(prepared);
        when(taskMapper.succeed(eq(4001L),eq(2001L),any())).thenReturn(1); when(clueMapper.closeConverted(3001L)).thenReturn(1);
        var animal=new RescueRequestNormalizer.NormalizedAnimal("Milo","CAT","UNKNOWN",12,"orange","stable","bandaged",List.of());
        var out=service.submitResult(2001L,4001L,RescueRequestNormalizer.NormalizedResult.success(List.of(animal)));
        assertNotNull(out);
        InOrder frozenOrder=inOrder(taskMapper,clueMapper,fileBinding,animalMapper);
        frozenOrder.verify(taskMapper).selectForUpdate(4001L);
        frozenOrder.verify(clueMapper).selectForUpdate(3001L);
        frozenOrder.verify(fileBinding).lockAndValidate(eq(2001L),anyList());
        frozenOrder.verify(animalMapper).insert(any());
        verify(fileBinding).bindPrepared(eq(2001L),eq(prepared),anyList());
        verify(healthMapper).insert(argThat(h->h.getAnimalId().equals(5001L)&&h.getRecorderId().equals(2001L)&&h.getContent().equals("bandaged")));
        verify(logs).append("RESCUE_TASK",4001L,"COMPLETE_RESCUE","IN_PROGRESS","SUCCESS",2001L,null);
        verify(logs).append("RESCUE_CLUE",3001L,"CLOSE_AFTER_RESCUE","CONVERTED","CLOSED",2001L,null);
        verify(fileBinding).copyPrepared(prepared);
    }

    @Test void cancelLocksTaskThenClueAndReopens(){
        RescueTask task=task(4001L,3001L,2001L,"IN_PROGRESS"); when(taskMapper.selectForUpdate(4001L)).thenReturn(task);
        when(clueMapper.selectForUpdate(3001L)).thenReturn(clue(3001L,"CONVERTED")); when(taskMapper.cancel(eq(4001L),eq("admin cancel"),any())).thenReturn(1); when(clueMapper.reopenConverted(3001L)).thenReturn(1);
        RescueTask saved=task(4001L,3001L,2001L,"CANCELED"); saved.setUpdatedAt(LocalDateTime.now()); when(taskMapper.selectById(4001L)).thenReturn(saved);
        CancelRescueTaskRequest req=new CancelRescueTaskRequest(); req.setCancelReason(" admin cancel ");
        service.cancel(1L,4001L,req);
        InOrder order=inOrder(taskMapper,clueMapper); order.verify(taskMapper).selectForUpdate(4001L); order.verify(clueMapper).selectForUpdate(3001L);
        verify(logs).append("RESCUE_TASK",4001L,"CANCEL_RESCUE","IN_PROGRESS","CANCELED",1L,"admin cancel");
        verify(logs).append("RESCUE_CLUE",3001L,"REOPEN","CONVERTED","WAITING_ACCEPT",1L,"admin cancel");
    }

    @Test void oldFailedTaskCannotResolveNewerTaskClue(){
        RescueTask failed=task(4001L,3001L,2001L,"FAILED"); when(taskMapper.selectForUpdate(4001L)).thenReturn(failed); when(clueMapper.selectForUpdate(3001L)).thenReturn(clue(3001L,"CONVERTED"));
        when(taskMapper.selectLatestForUpdateByClue(3001L)).thenReturn(task(4002L,3001L,2002L,"IN_PROGRESS"));
        FailureResolutionRequest req=new FailureResolutionRequest(); req.setAction("REOPEN"); req.setResolutionReason("retry");
        assertEquals(ErrorCode.BUSINESS_STATE_CONFLICT,assertThrows(BusinessException.class,()->service.resolveFailure(1L,4001L,req)).getErrorCode());
        verify(logs,never()).append(anyString(),anyLong(),anyString(),any(),any(),anyLong(),any());
    }

    @Test void failureResolutionUsesResolutionReasonNotFailureReason(){
        RescueTask failed=task(4001L,3001L,2001L,"FAILED"); failed.setFailureReason("escaped"); when(taskMapper.selectForUpdate(4001L)).thenReturn(failed);
        when(clueMapper.selectForUpdate(3001L)).thenReturn(clue(3001L,"CONVERTED")); when(taskMapper.selectLatestForUpdateByClue(3001L)).thenReturn(failed); when(taskMapper.selectActiveForUpdateByClue(3001L)).thenReturn(null); when(clueMapper.reopenConverted(3001L)).thenReturn(1);
        RescueClue saved=clue(3001L,"WAITING_ACCEPT"); saved.setUpdatedAt(LocalDateTime.now()); when(clueMapper.selectById(3001L)).thenReturn(saved);
        FailureResolutionRequest req=new FailureResolutionRequest(); req.setAction("REOPEN"); req.setResolutionReason("new sighting");
        service.resolveFailure(1L,4001L,req);
        verify(logs).append("RESCUE_CLUE",3001L,"REOPEN","CONVERTED","WAITING_ACCEPT",1L,"new sighting");
    }


    @Test void concurrentAcceptOnlyOneCreatesTask() throws Exception {
        AtomicInteger updates=new AtomicInteger();
        when(clueMapper.acceptForRescue(3001L)).thenAnswer(i -> updates.getAndIncrement()==0 ? 1 : 0);
        when(clueMapper.selectById(3001L)).thenReturn(clue(3001L,"CONVERTED"));
        when(taskMapper.insert(any())).thenAnswer(i->{ RescueTask t=i.getArgument(0); t.setId(4001L); return 1; });
        RescueTask saved=task(4001L,3001L,2001L,"WAITING_START"); saved.setCreatedAt(LocalDateTime.now()); when(taskMapper.selectById(4001L)).thenReturn(saved);
        CountDownLatch start=new CountDownLatch(1); ExecutorService pool=Executors.newFixedThreadPool(2);
        try {
            Future<Object> a=pool.submit(()->acceptAfter(start)); Future<Object> b=pool.submit(()->acceptAfter(start)); start.countDown();
            Object r1=a.get(), r2=b.get();
            long ok=List.of(r1,r2).stream().filter(x->x instanceof com.petlink.modules.rescue.vo.AcceptTaskResponse).count();
            long conflicts=List.of(r1,r2).stream().filter(x->x instanceof BusinessException).map(x->(BusinessException)x)
                    .filter(x->x.getErrorCode()==ErrorCode.BUSINESS_STATE_CONFLICT).count();
            assertEquals(1,ok); assertEquals(1,conflicts); verify(taskMapper,times(1)).insert(any());
        } finally { pool.shutdownNow(); }
    }

    private Object acceptAfter(CountDownLatch start) {
        try { start.await(); return service.accept(2001L,3001L); }
        catch (BusinessException ex) { return ex; }
        catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new RuntimeException(ex); }
    }

    private RescueTask task(Long id,Long clueId,Long rescuer,String status){ RescueTask t=new RescueTask(); t.setId(id); t.setClueId(clueId); t.setRescuerId(rescuer); t.setStatus(status); return t; }
    private RescueClue clue(Long id,String status){ RescueClue c=new RescueClue(); c.setId(id); c.setStatus(status); return c; }
}
