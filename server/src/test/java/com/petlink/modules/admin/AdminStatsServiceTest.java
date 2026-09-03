package com.petlink.modules.admin;

import com.petlink.common.BusinessException;
import com.petlink.common.ErrorCode;
import com.petlink.modules.admin.mapper.AdminMapper;
import com.petlink.modules.admin.mapper.StatusCountRow;
import com.petlink.modules.admin.mapper.TrendCountRow;
import com.petlink.modules.admin.service.AdminStatsService;
import com.petlink.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AdminStatsServiceTest {
    AdminMapper mapper;AdminStatsService service;UserPrincipal admin=new UserPrincipal(1L,"ADMIN");
    @BeforeEach void setUp(){mapper=mock(AdminMapper.class);service=new AdminStatsService(mapper);}
    @Test void overviewAlwaysReturnsAllFrozenStatuses(){when(mapper.countClueStatuses()).thenReturn(List.of(status("CLOSED",3)));when(mapper.countTaskStatuses()).thenReturn(List.of(status("SUCCESS",2)));when(mapper.countAnimalStatuses()).thenReturn(List.of());when(mapper.countApplicationStatuses()).thenReturn(List.of(status("PENDING",4)));var out=service.overview(admin);assertEquals(6,out.getRescueClues().size());assertEquals(0,out.getRescueClues().get("PENDING_REVIEW"));assertEquals(3,out.getRescueClues().get("CLOSED"));assertEquals(5,out.getRescueTasks().size());assertEquals(5,out.getAnimals().size());assertEquals(5,out.getAdoptionApplications().size());}
    @Test void overviewHasFixedUserAndHistoryCounters(){when(mapper.countAllUsers()).thenReturn(10L);when(mapper.countUsersByStatus("ENABLED")).thenReturn(8L);when(mapper.countUsersByRole("USER")).thenReturn(6L);when(mapper.countAllAdoptionRecords()).thenReturn(3L);when(mapper.countAllFollowUps()).thenReturn(7L);var out=service.overview(admin);assertEquals(10,out.getUsers().get("total"));assertEquals(8,out.getUsers().get("enabled"));assertEquals(6,out.getUsers().get("users"));assertEquals(3,out.getAdoptionRecords().get("total"));assertEquals(7,out.getFollowUps().get("total"));}
    @Test void trendsFillEveryInclusiveDayWithZero(){when(mapper.countRescueSuccessByDay(any(),any())).thenReturn(List.of(trend("2026-08-01",2),trend("2026-08-03",1)));when(mapper.countAdoptionsByDay(any(),any())).thenReturn(List.of(trend("2026-08-02",4)));var out=service.trends(admin,"2026-08-01","2026-08-03","DAY");assertEquals(3,out.getPoints().size());assertEquals(0,out.getPoints().get(1).getRescueSuccessCount());assertEquals(4,out.getPoints().get(1).getAdoptionCount());assertEquals("Asia/Shanghai",out.getZoneId());}
    @Test void trendsUsesExclusiveNextDayBoundary(){service.trends(admin,"2026-08-01","2026-08-03","DAY");verify(mapper).countRescueSuccessByDay(eq(java.time.LocalDateTime.of(2026,8,1,0,0)),eq(java.time.LocalDateTime.of(2026,8,4,0,0)));}
    @Test void granularityOtherThanDayRejected(){assertEquals(ErrorCode.INVALID_PARAMETER,error(()->service.trends(admin,"2026-08-01","2026-08-02","WEEK")));}
    @Test void malformedFromRejected(){assertEquals(ErrorCode.INVALID_PARAMETER,error(()->service.trends(admin,"2026/08/01","2026-08-02","DAY")));}
    @Test void malformedToRejected(){assertEquals(ErrorCode.INVALID_PARAMETER,error(()->service.trends(admin,"2026-08-01","bad","DAY")));}
    @Test void reverseRangeRejected(){assertEquals(ErrorCode.INVALID_PARAMETER,error(()->service.trends(admin,"2026-08-03","2026-08-02","DAY")));}
    @Test void nullDatesRejected(){assertEquals(ErrorCode.INVALID_PARAMETER,error(()->service.trends(admin,null,"2026-08-02","DAY")));}
    @Test void nonAdminCannotReadStats(){assertEquals(ErrorCode.FORBIDDEN,error(()->service.overview(new UserPrincipal(2L,"USER"))));}
    private StatusCountRow status(String s,long c){StatusCountRow r=new StatusCountRow();r.setStatus(s);r.setCount(c);return r;}
    private TrendCountRow trend(String d,long c){TrendCountRow r=new TrendCountRow();r.setDate(d);r.setCount(c);return r;}
    private ErrorCode error(Runnable action){return assertThrows(BusinessException.class,action::run).getErrorCode();}
}
