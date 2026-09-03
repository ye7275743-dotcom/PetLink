package com.petlink.modules.rescue.service;

import com.petlink.common.BusinessException;
import com.petlink.common.ErrorCode;
import com.petlink.common.PageResponse;
import com.petlink.modules.clue.entity.RescueClue;
import com.petlink.modules.clue.mapper.RescueClueMapper;
import com.petlink.modules.clue.service.ClueResponseAssembler;
import com.petlink.modules.clue.vo.ClueSummaryResponse;
import com.petlink.modules.rescue.entity.RescueTask;
import com.petlink.modules.rescue.mapper.RescueRecordMapper;
import com.petlink.modules.rescue.mapper.RescueTaskMapper;
import com.petlink.modules.rescue.vo.RescueRecordResponse;
import com.petlink.modules.rescue.vo.TaskDetailResponse;
import com.petlink.modules.rescue.vo.TaskSummaryResponse;
import com.petlink.security.UserPrincipal;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RescueTaskQueryService {
    private static final Set<String> TASK_STATUSES = Set.of("WAITING_START","IN_PROGRESS","SUCCESS","FAILED","CANCELED");
    private final RescueClueMapper clueMapper;
    private final RescueTaskMapper taskMapper;
    private final RescueRecordMapper recordMapper;
    private final ClueResponseAssembler clueAssembler;
    private final RescueTaskResponseAssembler taskAssembler;

    public RescueTaskQueryService(RescueClueMapper clueMapper, RescueTaskMapper taskMapper,
                                  RescueRecordMapper recordMapper, ClueResponseAssembler clueAssembler,
                                  RescueTaskResponseAssembler taskAssembler) {
        this.clueMapper=clueMapper; this.taskMapper=taskMapper; this.recordMapper=recordMapper;
        this.clueAssembler=clueAssembler; this.taskAssembler=taskAssembler;
    }

    public PageResponse<ClueSummaryResponse> waitingAcceptance(UserPrincipal principal, int page, int size) {
        requireRescuer(principal); validatePage(page,size);
        long offset=(long)(page-1)*size;
        List<ClueSummaryResponse> records=clueMapper.selectWaitingAcceptancePage(size, offset).stream()
                .map(clueAssembler::summary).collect(Collectors.toList());
        return new PageResponse<>(records,page,size,clueMapper.countWaitingAcceptance());
    }

    public PageResponse<TaskSummaryResponse> mine(UserPrincipal principal, int page, int size, String status) {
        requireRescuer(principal); validatePage(page,size);
        String normalized=normalizeStatus(status);
        long offset=(long)(page-1)*size;
        List<TaskSummaryResponse> records=taskMapper.selectMinePage(principal.getUserId(), normalized, size, offset).stream()
                .map(taskAssembler::summary).collect(Collectors.toList());
        return new PageResponse<>(records,page,size,taskMapper.countMine(principal.getUserId(), normalized));
    }

    public TaskDetailResponse detail(UserPrincipal principal, Long taskId) {
        RescueTask task=requireVisibleTask(principal, taskId);
        RescueClue clue=clueMapper.selectById(task.getClueId());
        if (clue == null) throw new IllegalStateException("rescue_task references missing rescue_clue");
        return taskAssembler.detail(task,clue);
    }

    public List<RescueRecordResponse> records(UserPrincipal principal, Long taskId) {
        RescueTask task=requireVisibleTask(principal, taskId);
        return recordMapper.selectByTaskId(task.getId()).stream().map(taskAssembler::record).collect(Collectors.toList());
    }

    public RescueTask requireVisibleTask(UserPrincipal principal, Long taskId) {
        if (principal == null || taskId == null || taskId <= 0) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        RescueTask task=taskMapper.selectById(taskId);
        if (task == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        if ("ADMIN".equals(principal.getRoleCode())) return task;
        if ("RESCUER".equals(principal.getRoleCode()) && principal.getUserId().equals(task.getRescuerId())) return task;
        throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) return null;
        String value=status.trim();
        if (!TASK_STATUSES.contains(value)) throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        return value;
    }
    private void validatePage(int page,int size) {
        if (page<1 || size<1 || size>100) throw new BusinessException(ErrorCode.INVALID_PARAMETER);
    }
    private void requireRescuer(UserPrincipal principal) {
        if (principal == null || !"RESCUER".equals(principal.getRoleCode())) throw new BusinessException(ErrorCode.FORBIDDEN);
    }
}
