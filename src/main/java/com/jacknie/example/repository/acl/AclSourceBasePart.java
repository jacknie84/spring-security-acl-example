package com.jacknie.example.repository.acl;

import com.jacknie.example.custom.SidType;
import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;

@Data
public class AclSourceBasePart {

    /**
     * 객체 식별 정보 아이디
     */
    private final Long objectIdentityId;

    /**
     * 부모 객체 식별 정보 아이디
     */
    private final Long parentObjectIdentityId;

    /**
     * 부모의 ACE 목록이 ACL 로 부터 상속 여부
     */
    private final Boolean entriesInheriting;

    /**
     * 객체 식별 정보
     */
    private final String objectIdIdentity;

    /**
     * 객체 식별 정보 아이디 타입
     */
    private final String classIdType;

    /**
     * 객체 클래스 이름
     */
    private final String className;

    /**
     * 보안 식별 정보 타입
     */
    private final SidType sidType;

    /**
     * 보안 식별 정보
     */
    private final String sid;

    @QueryProjection
    public AclSourceBasePart(Long objectIdentityId, Long parentObjectIdentityId, Boolean entriesInheriting, String objectIdIdentity, String classIdType, String className, SidType sidType, String sid) {
        this.objectIdentityId = objectIdentityId;
        this.parentObjectIdentityId = parentObjectIdentityId;
        this.entriesInheriting = entriesInheriting;
        this.objectIdIdentity = objectIdIdentity;
        this.classIdType = classIdType;
        this.className = className;
        this.sidType = sidType;
        this.sid = sid;
    }
}
