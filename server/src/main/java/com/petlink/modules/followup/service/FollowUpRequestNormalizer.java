package com.petlink.modules.followup.service;

import com.petlink.common.BusinessException;
import com.petlink.common.ErrorCode;
import com.petlink.modules.followup.dto.SubmitFollowUpRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Component
public class FollowUpRequestNormalizer {
    public String normalizeIdempotencyKey(SubmitFollowUpRequest request) {
        if (request == null || request.getIdempotencyKey() == null) {
            throw new BusinessException(ErrorCode.INVALID_IDEMPOTENCY_KEY);
        }
        String raw=request.getIdempotencyKey().trim();
        if (raw.length()!=36) throw new BusinessException(ErrorCode.INVALID_IDEMPOTENCY_KEY);
        try {
            String canonical=UUID.fromString(raw).toString();
            if (!canonical.equals(raw.toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException();
            }
            return canonical;
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.INVALID_IDEMPOTENCY_KEY);
        }
    }

    public NormalizedSubmit normalizeForCreate(SubmitFollowUpRequest request, String canonicalKey) {
        if (request == null) throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        String content=optionalText(request.getContent(),2000);
        String health=optionalText(request.getHealthCondition(),1000);
        List<String> tokens=normalizeTokens(request.getImageTokens());
        if (content==null && health==null && tokens.isEmpty()) throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        return new NormalizedSubmit(canonicalKey,content,health,tokens);
    }

    private String optionalText(String raw,int max){
        if(raw==null) return null;
        String value=raw.trim();
        if(value.isEmpty()) return null;
        if(value.length()>max) throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        return value;
    }

    private List<String> normalizeTokens(List<String> raw){
        if(raw==null||raw.isEmpty()) return List.of();
        if(raw.size()>9) throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        List<String> out=new ArrayList<>(raw.size()); Set<String> seen=new HashSet<>();
        for(String token:raw){
            if(token==null) throw new BusinessException(ErrorCode.INVALID_FILE);
            String value=token.trim().toLowerCase(Locale.ROOT);
            try {
                if(!UUID.fromString(value).toString().equals(value)) throw new IllegalArgumentException();
            } catch(IllegalArgumentException ex){ throw new BusinessException(ErrorCode.INVALID_FILE); }
            if(!seen.add(value)) throw new BusinessException(ErrorCode.INVALID_PARAMETER);
            out.add(value);
        }
        return List.copyOf(out);
    }

    public static class NormalizedSubmit {
        private final String idempotencyKey,content,healthCondition;
        private final List<String> imageTokens;
        public NormalizedSubmit(String key,String content,String health,List<String> tokens){
            this.idempotencyKey=key;this.content=content;this.healthCondition=health;this.imageTokens=tokens;
        }
        public String getIdempotencyKey(){return idempotencyKey;}
        public String getContent(){return content;}
        public String getHealthCondition(){return healthCondition;}
        public List<String> getImageTokens(){return imageTokens;}
    }
}
