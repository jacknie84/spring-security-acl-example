package com.jacknie.example.repository.acl;

import com.jacknie.example.custom.SidType;
import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;

@Data
public class AclSourceAcePart {

    /**
     * ACE 아이디
     */
    private final Long id;

    /**
     * 보안 식별 정보
     */
    private final String sid;

    /**
     * 보안 식별 정보 타입
     */
    private final SidType sidType;

    /**
     * 마스킹 값
     */
    private final Integer mask;

    /**
     * 권한 부여 여부
     */
    private final Boolean granting;

    /**
     * auditing 성공 여부
     */
    private final Boolean auditSuccess;

    /**
     * auditing 실패 여부
     */
    private final Boolean auditFailure;

    @QueryProjection

    public AclSourceAcePart(Long id, String sid, SidType sidType, Integer mask, Boolean granting, Boolean auditSuccess, Boolean auditFailure) {
        this.id = id;
        this.sid = sid;
        this.sidType = sidType;
        this.mask = mask;
        this.granting = granting;
        this.auditSuccess = auditSuccess;
        this.auditFailure = auditFailure;
    }
}
