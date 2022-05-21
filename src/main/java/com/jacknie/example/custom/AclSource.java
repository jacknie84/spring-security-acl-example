package com.jacknie.example.custom;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AclSource {

    /**
     * 객체 식별 정보 관리 아이디
     */
    private long aclId;

    /**
     * 부모 아이디
     */
    @Nullable
    private Long aclParentId;

    /**
     * 부모의 ACE 목록이 ACL 로 부터 상속 여부
     */
    private boolean entriesInheriting;

    /**
     * 객체 식별 정보
     */
    private Serializable identifier;

    /**
     * 도메인 객체에 대한 유형 정보
     */
    private String type;

    /**
     * 보안 식별 타입
     */
    private SidType aclSidType;

    /**
     * 보안 식별 정보
     */
    private String aclSid;

    /**
     * 엔트리 보안 식별 정보
     */
    private String aceSid;

    /**
     * 엔트리 아이디
     */
    private long aceId;

    /**
     * 엔트리 보안 식별 타입
     */
    private SidType aceSidType;

    /**
     * 마스킹 값
     */
    private int mask;

    /**
     * 권한 부여 여부
     */
    private boolean granting;

    /**
     * auditing 성공 여부
     */
    private boolean auditSuccess;

    /**
     * auditing 실패 여부
     */
    private boolean auditFailure;

}
