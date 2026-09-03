package com.petlink.modules.clue.service;

import com.petlink.common.BusinessException;
import com.petlink.common.ErrorCode;
import com.petlink.common.PageResponse;
import com.petlink.modules.clue.entity.RescueClue;
import com.petlink.modules.clue.mapper.RescueClueMapper;
import com.petlink.modules.clue.vo.ClueDetailResponse;
import com.petlink.modules.clue.vo.ClueSummaryResponse;
import com.petlink.security.UserPrincipal;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RescueClueQueryService {
    public static final Set<String> CLUE_STATUSES = Set.of(
            "PENDING_REVIEW", "REJECTED", "WITHDRAWN", "WAITING_ACCEPT", "CONVERTED", "CLOSED");

    private final RescueClueMapper clueMapper;
    private final ClueResponseAssembler assembler;

    public RescueClueQueryService(RescueClueMapper clueMapper, ClueResponseAssembler assembler) {
        this.clueMapper = clueMapper;
        this.assembler = assembler;
    }

    public PageResponse<ClueSummaryResponse> mine(UserPrincipal principal, int page, int size, String status) {
        requireMember(principal);
        validatePage(page, size);
        String normalizedStatus = normalizeOptionalStatus(status, null);
        long offset = (long) (page - 1) * size;
        List<ClueSummaryResponse> records = clueMapper
                .selectMinePage(principal.getUserId(), normalizedStatus, size, offset)
                .stream().map(assembler::summary).collect(Collectors.toList());
        long total = clueMapper.countMine(principal.getUserId(), normalizedStatus);
        return new PageResponse<>(records, page, size, total);
    }

    public PageResponse<ClueSummaryResponse> adminList(UserPrincipal principal, int page, int size, String status) {
        requireAdmin(principal);
        validatePage(page, size);
        String normalizedStatus = normalizeOptionalStatus(status, "PENDING_REVIEW");
        long offset = (long) (page - 1) * size;
        List<ClueSummaryResponse> records = clueMapper
                .selectAdminPage(normalizedStatus, size, offset)
                .stream().map(assembler::summary).collect(Collectors.toList());
        long total = clueMapper.countAdmin(normalizedStatus);
        return new PageResponse<>(records, page, size, total);
    }

    public ClueDetailResponse detail(UserPrincipal principal, Long clueId) {
        RescueClue clue = requireVisibleClue(principal, clueId);
        return assembler.detail(clue, "ADMIN".equals(principal.getRoleCode()));
    }

    public RescueClue requireVisibleClue(UserPrincipal principal, Long clueId) {
        if (principal == null || clueId == null || clueId <= 0) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        RescueClue clue = clueMapper.selectById(clueId);
        if (clue == null || !isVisible(principal, clue)) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        return clue;
    }

    private boolean isVisible(UserPrincipal principal, RescueClue clue) {
        String role = principal.getRoleCode();
        Long userId = principal.getUserId();
        if ("ADMIN".equals(role)) return true;
        if ("USER".equals(role)) return userId.equals(clue.getPublisherId());
        if ("RESCUER".equals(role)) {
            return userId.equals(clue.getPublisherId())
                    || "WAITING_ACCEPT".equals(clue.getStatus())
                    || clueMapper.existsTaskForRescuer(clue.getId(), userId) == 1;
        }
        return false;
    }

    private String normalizeOptionalStatus(String status, String defaultValue) {
        if (status == null || status.isBlank()) return defaultValue;
        String value = status.trim();
        if (!CLUE_STATUSES.contains(value)) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        }
        return value;
    }

    private void validatePage(int page, int size) {
        if (page < 1 || size < 1 || size > 100) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        }
    }

    private void requireMember(UserPrincipal principal) {
        if (principal == null || !("USER".equals(principal.getRoleCode()) || "RESCUER".equals(principal.getRoleCode()))) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private void requireAdmin(UserPrincipal principal) {
        if (principal == null || !"ADMIN".equals(principal.getRoleCode())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
