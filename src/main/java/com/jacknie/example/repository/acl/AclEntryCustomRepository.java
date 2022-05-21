package com.jacknie.example.repository.acl;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface AclEntryCustomRepository {

    /**
     * ACL 소스 데이터 ACE 파트 정보 목록 맵 조회
     * @param oidIds 객체 식별 정보 아이디 목록
     * @return ACE 파트 정보 목록 맵
     */
    Map<Long, List<AclSourceAcePart>> findAclSourceAcePartsMap(Set<Long> oidIds);

    /**
     * ACE 정보 맵 조회
     * @param aclOid 객체 식별 정보 entity
     * @return ACE 정보 맵
     */
    Map<Long, AclEntry> findMapByObjectIdentity(AclObjectIdentity aclOid);
}
