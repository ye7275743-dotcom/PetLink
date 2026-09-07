package com.petlink.modules.admin.service;

import com.petlink.common.BusinessException;
import com.petlink.common.ErrorCode;
import com.petlink.common.TimeUtils;
import com.petlink.modules.admin.mapper.AdminMapper;
import com.petlink.modules.admin.mapper.StatusCountRow;
import com.petlink.modules.admin.mapper.TrendCountRow;
import com.petlink.modules.admin.vo.AdminStatsOverviewResponse;
import com.petlink.modules.admin.vo.AdminStatsTrendPointResponse;
import com.petlink.modules.admin.vo.AdminStatsTrendsResponse;
import com.petlink.security.UserPrincipal;
import org.springframework.stereotype.Service;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminStatsService {
    private static final List<String> CLUE_STATUSES=List.of("PENDING_REVIEW","REJECTED","WITHDRAWN","WAITING_ACCEPT","CONVERTED","CLOSED");
    private static final List<String> TASK_STATUSES=List.of("WAITING_START","IN_PROGRESS","SUCCESS","FAILED","CANCELED");
    private static final List<String> ANIMAL_STATUSES=List.of("TREATING","OBSERVING","AVAILABLE","SUSPENDED","ADOPTED");
    private static final List<String> APPLICATION_STATUSES=List.of("PENDING","APPROVED","REJECTED","WITHDRAWN","INVALIDATED");
    private final AdminMapper mapper;

    public AdminStatsService(AdminMapper mapper){this.mapper=mapper;}

    public AdminStatsOverviewResponse overview(UserPrincipal admin) {
        AdminUserService.requireAdmin(admin);
        Map<String,Long> users=new LinkedHashMap<>();
        users.put("total",mapper.countAllUsers());users.put("enabled",mapper.countUsersByStatus("ENABLED"));
        users.put("disabled",mapper.countUsersByStatus("DISABLED"));users.put("users",mapper.countUsersByRole("USER"));
        users.put("rescuers",mapper.countUsersByRole("RESCUER"));users.put("admins",mapper.countUsersByRole("ADMIN"));
        return new AdminStatsOverviewResponse(users,statusCounts(CLUE_STATUSES,mapper.countClueStatuses()),
                statusCounts(TASK_STATUSES,mapper.countTaskStatuses()),statusCounts(ANIMAL_STATUSES,mapper.countAnimalStatuses()),
                statusCounts(APPLICATION_STATUSES,mapper.countApplicationStatuses()),single("total",mapper.countAllAdoptionRecords()),
                single("total",mapper.countAllFollowUps()));
    }

    public AdminStatsTrendsResponse trends(UserPrincipal admin,String fromRaw,String toRaw,String granularity) {
        AdminUserService.requireAdmin(admin);
        if(!"DAY".equals(granularity))throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        LocalDate from=parseDate(fromRaw);LocalDate to=parseDate(toRaw);
        if(java.time.temporal.ChronoUnit.DAYS.between(from,to)>365)throw new BusinessException(ErrorCode.INVALID_PARAMETER,"查询范围不能超过 366 天");
        if(from.isAfter(to))throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        LocalDateTime start;LocalDateTime end;
        try{start=from.atStartOfDay();end=to.plusDays(1).atStartOfDay();}catch(DateTimeException ex){throw new BusinessException(ErrorCode.INVALID_PARAMETER);}
        Map<String,Long> rescues=trendMap(mapper.countRescueSuccessByDay(start,end));
        Map<String,Long> adoptions=trendMap(mapper.countAdoptionsByDay(start,end));
        List<AdminStatsTrendPointResponse> points=new ArrayList<>();
        for(LocalDate day=from;!day.isAfter(to);day=day.plusDays(1)){
            String key=day.toString();points.add(new AdminStatsTrendPointResponse(key,rescues.getOrDefault(key,0L),adoptions.getOrDefault(key,0L)));
        }
        return new AdminStatsTrendsResponse(from.toString(),to.toString(),"DAY",TimeUtils.ZONE.getId(),points);
    }

    private static LocalDate parseDate(String raw){if(raw==null||raw.isBlank())throw new BusinessException(ErrorCode.INVALID_PARAMETER);try{return LocalDate.parse(raw);}catch(DateTimeParseException ex){throw new BusinessException(ErrorCode.INVALID_PARAMETER);}}
    private static Map<String,Long> statusCounts(List<String> statuses,List<StatusCountRow> rows){Map<String,Long> out=new LinkedHashMap<>();statuses.forEach(s->out.put(s,0L));for(StatusCountRow row:rows){if(out.containsKey(row.getStatus()))out.put(row.getStatus(),row.getCount());}return out;}
    private static Map<String,Long> trendMap(List<TrendCountRow> rows){Map<String,Long> out=new LinkedHashMap<>();for(TrendCountRow row:rows)out.put(row.getDate(),row.getCount());return out;}
    private static Map<String,Long> single(String key,long value){Map<String,Long> out=new LinkedHashMap<>();out.put(key,value);return out;}
}
