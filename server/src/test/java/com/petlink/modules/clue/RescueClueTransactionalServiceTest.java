package com.petlink.modules.clue;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.petlink.common.BusinessException;
import com.petlink.common.ErrorCode;
import com.petlink.infrastructure.audit.service.OperationLogService;
import com.petlink.infrastructure.file.service.FileStorageService;
import com.petlink.modules.clue.dto.AuditClueRequest;
import com.petlink.modules.clue.entity.RescueClue;
import com.petlink.modules.clue.entity.RescueClueImage;
import com.petlink.modules.clue.mapper.RescueClueImageMapper;
import com.petlink.modules.clue.mapper.RescueClueMapper;
import com.petlink.modules.clue.service.ClueFileBindingService;
import com.petlink.modules.clue.service.ClueIdempotencyStore;
import com.petlink.modules.clue.service.ClueRequestNormalizer;
import com.petlink.modules.clue.service.ClueResponseAssembler;
import com.petlink.modules.clue.service.RescueClueTransactionalService;
import com.petlink.modules.clue.vo.ClueDetailResponse;
import com.petlink.modules.clue.vo.StateActionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

class RescueClueTransactionalServiceTest {
    private RescueClueMapper clueMapper;
    private RescueClueImageMapper imageMapper;
    private OperationLogService logService;
    private ClueResponseAssembler assembler;
    private ClueFileBindingService fileBindingService;
    private RescueClueTransactionalService service;

    @BeforeEach
    void setUp() {
        clueMapper = mock(RescueClueMapper.class);
        imageMapper = mock(RescueClueImageMapper.class);
        logService = mock(OperationLogService.class);
        assembler = mock(ClueResponseAssembler.class);
        fileBindingService = mock(ClueFileBindingService.class);
        service = new RescueClueTransactionalService(
                clueMapper, imageMapper, fileBindingService,
                mock(FileStorageService.class), logService, assembler, mock(ClueIdempotencyStore.class));
    }

    @Test
    void equalValuePatchDoesNotExecuteSqlUpdate() {
        RescueClue pending = pending(3001L, 1001L);
        when(clueMapper.selectForUpdate(3001L)).thenReturn(pending);
        ClueDetailResponse expected = mock(ClueDetailResponse.class);
        when(assembler.detail(pending, false)).thenReturn(expected);

        com.petlink.modules.clue.dto.UpdateClueRequest dto = new com.petlink.modules.clue.dto.UpdateClueRequest();
        dto.setLocation("original-location");
        ClueRequestNormalizer.NormalizedUpdate update = new ClueRequestNormalizer().normalizeUpdate(dto);

        assertSame(expected, service.update(1001L, 3001L, update));
        verify(clueMapper, never()).update(isNull(), any(Wrapper.class));
        verify(clueMapper, never()).selectById(3001L);
    }

    @Test
    void changedPatchUpdatesOnlyChangedColumns() {
        RescueClue pending = pending(3001L, 1001L);
        pending.setContact("13800138000");
        pending.setSceneDescription("old-scene");
        when(clueMapper.selectForUpdate(3001L)).thenReturn(pending);
        when(clueMapper.update(isNull(), any(Wrapper.class))).thenReturn(1);

        RescueClue saved = pending(3001L, 1001L);
        saved.setContact("13900139000");
        saved.setSceneDescription("old-scene");
        when(clueMapper.selectById(3001L)).thenReturn(saved);
        ClueDetailResponse expected = mock(ClueDetailResponse.class);
        when(assembler.detail(saved, false)).thenReturn(expected);

        com.petlink.modules.clue.dto.UpdateClueRequest dto = new com.petlink.modules.clue.dto.UpdateClueRequest();
        dto.setLocation("original-location");
        dto.setContact("13900139000");
        ClueRequestNormalizer.NormalizedUpdate update = new ClueRequestNormalizer().normalizeUpdate(dto);

        assertSame(expected, service.update(1001L, 3001L, update));

        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<Wrapper<RescueClue>> captor = org.mockito.ArgumentCaptor.forClass(Wrapper.class);
        verify(clueMapper).update(isNull(), captor.capture());
        UpdateWrapper<RescueClue> wrapper = (UpdateWrapper<RescueClue>) captor.getValue();
        String sqlSet = wrapper.getSqlSet();
        assertNotNull(sqlSet);
        assertTrue(sqlSet.contains("contact"));
        assertFalse(sqlSet.contains("location"));
        assertFalse(sqlSet.contains("scene_description"));
    }

    @Test
    void appendImagesUsesMaxSortOrderPlusOneNotCountPlusOne() {
        when(clueMapper.selectForUpdate(3001L)).thenReturn(pending(3001L, 1001L));
        when(imageMapper.countByClueId(3001L)).thenReturn(2);
        when(imageMapper.selectMaxSortOrder(3001L)).thenReturn(4);
        RescueClue saved = pending(3001L, 1001L);
        when(clueMapper.selectById(3001L)).thenReturn(saved);
        when(assembler.detail(saved, false)).thenReturn(mock(ClueDetailResponse.class));

        service.appendImages(1001L, 3001L, List.of("550e8400-e29b-41d4-a716-446655440001"));

        verify(fileBindingService).bindImages(1001L, 3001L,
                List.of("550e8400-e29b-41d4-a716-446655440001"), 5);
    }

    @Test
    void withdrawDistinguishesHiddenResourceFromStateConflict() {
        when(clueMapper.withdrawPending(3001L, 1001L)).thenReturn(0);
        RescueClue other = pending(3001L, 2002L);
        when(clueMapper.selectById(3001L)).thenReturn(other);
        BusinessException hidden = assertThrows(BusinessException.class,
                () -> service.withdraw(1001L, 3001L));
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, hidden.getErrorCode());

        RescueClue ownReviewed = pending(3001L, 1001L);
        ownReviewed.setStatus("WAITING_ACCEPT");
        when(clueMapper.selectById(3001L)).thenReturn(ownReviewed);
        BusinessException conflict = assertThrows(BusinessException.class,
                () -> service.withdraw(1001L, 3001L));
        assertEquals(ErrorCode.BUSINESS_STATE_CONFLICT, conflict.getErrorCode());
    }

    @Test
    void rejectAuditWritesReasonAndFrozenOperationLog() {
        AuditClueRequest request = new AuditClueRequest();
        request.setDecision("REJECT");
        request.setRejectReason("  图片信息不足  ");
        when(clueMapper.auditReject(eq(3001L), eq(1L), any(LocalDateTime.class), eq("图片信息不足"))).thenReturn(1);
        RescueClue saved = pending(3001L, 1001L);
        saved.setStatus("REJECTED");
        saved.setUpdatedAt(LocalDateTime.now());
        when(clueMapper.selectById(3001L)).thenReturn(saved);

        var response = service.audit(1L, 3001L, request);
        assertEquals("REJECTED", response.getStatus());
        verify(logService).append("RESCUE_CLUE", 3001L, "AUDIT_REJECT",
                "PENDING_REVIEW", "REJECTED", 1L, "图片信息不足");
    }

    @Test
    void approveRejectReasonMustBeAbsentOrNull() {
        AuditClueRequest request = new AuditClueRequest();
        request.setDecision("APPROVE");
        request.setRejectReason("");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.audit(1L, 3001L, request));
        assertEquals(ErrorCode.INVALID_PARAMETER, ex.getErrorCode());
        verifyNoInteractions(logService);
    }

    @Test
    void deletingLastClueImageIs40901() {
        when(clueMapper.selectForUpdate(3001L)).thenReturn(pending(3001L, 1001L));
        RescueClueImage only = new RescueClueImage();
        only.setId(3101L);
        only.setSortOrder(1);
        when(imageMapper.selectByClueId(3001L)).thenReturn(List.of(only));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.deleteImage(1001L, 3001L, 3101L));
        assertEquals(ErrorCode.BUSINESS_STATE_CONFLICT, ex.getErrorCode());
    }

    @Test
    void concurrentAdminAuditAllowsOnlyOneSuccess() throws Exception {
        AuditClueRequest request = new AuditClueRequest();
        request.setDecision("APPROVE");

        AtomicInteger conditionalUpdates = new AtomicInteger();
        when(clueMapper.auditApprove(eq(3001L), anyLong(), any(LocalDateTime.class)))
                .thenAnswer(invocation -> conditionalUpdates.getAndIncrement() == 0 ? 1 : 0);
        RescueClue reviewed = pending(3001L, 1001L);
        reviewed.setStatus("WAITING_ACCEPT");
        reviewed.setUpdatedAt(LocalDateTime.now());
        when(clueMapper.selectById(3001L)).thenReturn(reviewed);

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Object> first = pool.submit(() -> runAuditAfter(start, 11L, request));
            Future<Object> second = pool.submit(() -> runAuditAfter(start, 12L, request));
            start.countDown();

            Object r1 = first.get();
            Object r2 = second.get();
            long successes = List.of(r1, r2).stream().filter(StateActionResponse.class::isInstance).count();
            long conflicts = List.of(r1, r2).stream()
                    .filter(BusinessException.class::isInstance)
                    .map(BusinessException.class::cast)
                    .filter(ex -> ex.getErrorCode() == ErrorCode.BUSINESS_STATE_CONFLICT)
                    .count();
            assertEquals(1, successes);
            assertEquals(1, conflicts);
            verify(logService, times(1)).append(eq("RESCUE_CLUE"), eq(3001L), eq("AUDIT_APPROVE"),
                    eq("PENDING_REVIEW"), eq("WAITING_ACCEPT"), anyLong(), isNull());
        } finally {
            pool.shutdownNow();
        }
    }

    private Object runAuditAfter(CountDownLatch start, Long adminId, AuditClueRequest request) {
        try {
            start.await();
            return service.audit(adminId, 3001L, request);
        } catch (BusinessException ex) {
            return ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(ex);
        }
    }

    private RescueClue pending(Long id, Long publisherId) {
        RescueClue clue = new RescueClue();
        clue.setId(id);
        clue.setPublisherId(publisherId);
        clue.setStatus("PENDING_REVIEW");
        clue.setLocation("original-location");
        clue.setUpdatedAt(LocalDateTime.now());
        return clue;
    }
}
