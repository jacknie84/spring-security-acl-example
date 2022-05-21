package com.jacknie.example.custom;

import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.Sid;

public enum SidType {

    /**
     * 인증 된 사용자 보안 식별 타입
     */
    PRINCIPAL,

    /**
     * 인가 된 권한 보안 식별 타입
     */
    GRANTED_AUTHORITY;

    public Sid createSid(String sid) {
        return switch (this) {
            case PRINCIPAL -> new PrincipalSid(sid);
            case GRANTED_AUTHORITY -> new GrantedAuthoritySid(sid);
        };
    }
}
