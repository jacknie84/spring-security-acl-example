package com.jacknie.example.custom;

import org.springframework.security.acls.model.ObjectIdentity;

import java.util.List;
import java.util.Set;

public interface LookupOperations {

    /**
     * ACL 소스 정보 목록 조회
     * @param oids 객체 식별 정보 목록
     * @return ACL 소스 정보 목록
     */
    List<AclSource> findAclSourcesByObjectIdentityIn(Set<ObjectIdentity> oids);

    /**
     * ACL 소스 정보 목록 조회
     * @param primaryKeys 식별 키 목록
     * @return ACL 소스 정보 목록
     */
    List<AclSource> findAclSourcesByObjectIdentityIdIn(Set<Long> primaryKeys);
}
