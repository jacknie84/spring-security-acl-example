package com.jacknie.example.custom;

import lombok.Getter;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.Sid;
import org.springframework.util.Assert;

@Getter
public class SidHelper {

    /**
     * 보안 식별 정보 타입
     */
    private final SidType type;

    /**
     * 보안 식별 정보
     */
    private final String sid;

    public SidHelper(Sid sid) {
        Assert.notNull(sid, "sid cannot be null");
        if (sid instanceof PrincipalSid) {
            this.sid = ((PrincipalSid) sid).getPrincipal();
            this.type = SidType.PRINCIPAL;
        } else if (sid instanceof GrantedAuthoritySid) {
            this.sid = ((GrantedAuthoritySid) sid).getGrantedAuthority();
            this.type = SidType.GRANTED_AUTHORITY;
        } else {
            throw new IllegalArgumentException("Unsupported implementation of Sid");
        }
    }
}
