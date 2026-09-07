package com.petlink.modules.clue.service;

import com.petlink.common.BusinessException;
import com.petlink.common.ErrorCode;
import com.petlink.common.PageResponse;
import com.petlink.modules.clue.dto.AppendClueImagesRequest;
import com.petlink.modules.clue.dto.AuditClueRequest;
import com.petlink.modules.clue.dto.CreateClueRequest;
import com.petlink.modules.clue.dto.UpdateClueRequest;
import com.petlink.modules.clue.service.ClueRequestNormalizer.NormalizedCreate;
import com.petlink.modules.clue.vo.ClueDetailResponse;
import com.petlink.modules.clue.vo.ClueSummaryResponse;
import com.petlink.modules.clue.vo.CreateClueResponse;
import com.petlink.modules.clue.vo.IdempotencyKeyResponse;
import com.petlink.modules.clue.vo.StateActionResponse;
import com.petlink.security.UserPrincipal;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RescueClueService {
    private final ClueIdempotencyStore idempotencyStore;
    private final ClueRequestNormalizer normalizer;
    private final RescueClueTransactionalService transactionalService;
    private final RescueClueQueryService queryService;

    public RescueClueService(ClueIdempotencyStore idempotencyStore,
                             ClueRequestNormalizer normalizer,
                             RescueClueTransactionalService transactionalService,
                             RescueClueQueryService queryService) {
        this.idempotencyStore = idempotencyStore;
        this.normalizer = normalizer;
        this.transactionalService = transactionalService;
        this.queryService = queryService;
    }

    public IdempotencyKeyResponse issueIdempotencyKey(UserPrincipal principal) {
        requireMember(principal);
        return idempotencyStore.issue(principal.getUserId());
    }

    public CreateClueResponse create(UserPrincipal principal, String idempotencyKey, CreateClueRequest request) {
        requireMember(principal);
        NormalizedCreate normalized = normalizer.normalizeCreate(principal.getUserId(), request);
        ClueIdempotencyStore.AcquireResult acquired = idempotencyStore.acquire(
                principal.getUserId(), idempotencyKey, normalized.getFingerprint());
        if (acquired.isReplay()) {
            return acquired.getReplayResponse();
        }
        try {
            return transactionalService.create(principal.getUserId(), idempotencyKey, normalized);
        } catch (RuntimeException ex) {
            idempotencyStore.releaseToUnused(principal.getUserId(), idempotencyKey, normalized.getFingerprint());
            throw ex;
        }
    }

    public PageResponse<ClueSummaryResponse> mine(UserPrincipal principal, int page, int size, String status) {
        return queryService.mine(principal, page, size, status);
    }

    public ClueDetailResponse detail(UserPrincipal principal, Long clueId) {
        return queryService.detail(principal, clueId);
    }

    public ClueDetailResponse update(UserPrincipal principal, Long clueId, UpdateClueRequest request) {
        requireMember(principal);
        return transactionalService.update(principal.getUserId(), clueId, normalizer.normalizeUpdate(request));
    }

    public ClueDetailResponse appendImages(UserPrincipal principal, Long clueId, AppendClueImagesRequest request) {
        requireMember(principal);
        if (request == null) throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        List<String> tokens = normalizer.normalizeImageTokens(request.getImageTokens());
        return transactionalService.appendImages(principal.getUserId(), clueId, tokens);
    }

    public ClueDetailResponse deleteImage(UserPrincipal principal, Long clueId, Long imageId) {
        requireMember(principal);
        if (imageId == null || imageId <= 0) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        return transactionalService.deleteImage(principal.getUserId(), clueId, imageId);
    }

    public StateActionResponse withdraw(UserPrincipal principal, Long clueId) {
        requireMember(principal);
        return transactionalService.withdraw(principal.getUserId(), clueId);
    }

    public PageResponse<ClueSummaryResponse> adminList(UserPrincipal principal, int page, int size, String status, String keyword) {
        requireAdmin(principal);
        return queryService.adminList(principal, page, size, status, keyword);
    }

    public StateActionResponse audit(UserPrincipal principal, Long clueId, AuditClueRequest request) {
        requireAdmin(principal);
        return transactionalService.audit(principal.getUserId(), clueId, request);
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
