package com.petlink.modules.clue;

import com.petlink.common.BusinessException;
import com.petlink.common.ErrorCode;
import com.petlink.modules.clue.entity.RescueClue;
import com.petlink.modules.clue.mapper.RescueClueMapper;
import com.petlink.modules.clue.service.ClueResponseAssembler;
import com.petlink.modules.clue.service.RescueClueQueryService;
import com.petlink.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RescueClueQueryServiceTest {
    private RescueClueMapper mapper;
    private ClueResponseAssembler assembler;
    private RescueClueQueryService service;

    @BeforeEach
    void setUp() {
        mapper = mock(RescueClueMapper.class);
        assembler = mock(ClueResponseAssembler.class);
        service = new RescueClueQueryService(mapper, assembler);
    }

    @Test
    void userCannotDiscoverAnotherUsersClue() {
        RescueClue clue = clue(3001L, 2002L, "PENDING_REVIEW");
        when(mapper.selectById(3001L)).thenReturn(clue);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.requireVisibleClue(new UserPrincipal(1001L, "USER"), 3001L));
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void rescuerCanSeeWaitingAcceptanceAndHistoricalTaskClue() {
        RescueClue waiting = clue(3001L, 2002L, "WAITING_ACCEPT");
        when(mapper.selectById(3001L)).thenReturn(waiting);
        assertSame(waiting, service.requireVisibleClue(new UserPrincipal(1001L, "RESCUER"), 3001L));

        RescueClue converted = clue(3002L, 2002L, "CONVERTED");
        when(mapper.selectById(3002L)).thenReturn(converted);
        when(mapper.existsTaskForRescuer(3002L, 1001L)).thenReturn(1);
        assertSame(converted, service.requireVisibleClue(new UserPrincipal(1001L, "RESCUER"), 3002L));
    }

    @Test
    void invalidPaginationIs40001() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.mine(new UserPrincipal(1001L, "USER"), 0, 20, null));
        assertEquals(ErrorCode.INVALID_PARAMETER, ex.getErrorCode());
    }

    @Test
    void adminListDefaultsToPendingReview() {
        when(mapper.selectAdminPage("PENDING_REVIEW", null, 20, 0L)).thenReturn(java.util.List.of());
        when(mapper.countAdmin("PENDING_REVIEW", null)).thenReturn(0L);
        service.adminList(new UserPrincipal(1L, "ADMIN"), 1, 20, null, null);
        verify(mapper).selectAdminPage("PENDING_REVIEW", null, 20, 0L);
    }

    @Test
    void adminListTrimsAndForwardsKeyword() {
        when(mapper.selectAdminPage("WAITING_ACCEPT", "公园", 10, 10L)).thenReturn(java.util.List.of());
        when(mapper.countAdmin("WAITING_ACCEPT", "公园")).thenReturn(2L);
        var out=service.adminList(new UserPrincipal(1L, "ADMIN"), 2, 10, "WAITING_ACCEPT", "  公园  ");
        assertEquals(2L,out.getTotal());
        verify(mapper).selectAdminPage("WAITING_ACCEPT", "公园", 10, 10L);
    }

    @Test
    void adminListRejectsOverlongKeyword() {
        String keyword="x".repeat(101);
        assertEquals(ErrorCode.INVALID_PARAMETER,assertThrows(BusinessException.class,
                () -> service.adminList(new UserPrincipal(1L,"ADMIN"),1,20,null,keyword)).getErrorCode());
    }

    private RescueClue clue(Long id, Long publisher, String status) {
        RescueClue clue = new RescueClue();
        clue.setId(id);
        clue.setPublisherId(publisher);
        clue.setStatus(status);
        return clue;
    }
}
