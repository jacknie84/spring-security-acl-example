package com.jacknie.example.custom;

import org.springframework.lang.Nullable;
import org.springframework.security.acls.model.AccessControlEntry;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.Sid;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface AclOperations {

    /**
     * 해당 객체 식별 정보가 관리 되고 있는지 확인
     * @param oid 객체 식별 정보
     * @return 관리 여부
     */
    boolean existsObjectIdentity(ObjectIdentity oid);

    /**
     * 보안 식별 정보 아이디 목록 조회
     * @param sidType 보안 식별 정보 타입
     * @param sidName 보안 식별 정보 이름
     * @return 보안 식별 정보 아이디 목록
     */
    List<Long> findSidIds(SidType sidType, String sidName);

    /**
     * 보안 식별 정보 저장 처리
     * @param sidType 보안 식별 정보 타입
     * @param sidName 보안 식별 정보 이름
     * @return 보안 식별 정보 아이디
     */
    Long saveSid(SidType sidType, String sidName);

    /**
     * 클래스 아이디 목록 조회
     * @param type 클래스 타입 문자열
     * @return 클래스 아이디 목록
     */
    List<Long> findClassIds(String type);

    /**
     * @return 기본 클래스 이름 저장 지원 여부
     */
    boolean isAclClassIdSupported();

    /**
     * 클래스 저장 처리
     * @param type 클래스 타입
     * @return 클래스 아이디
     */
    Long saveClass(String type);

    /**
     * 클래스 저장 처리
     * @param type 클래스 타입
     * @param idClassName 객체 식별 정보(아이디) 클래스 이름
     * @return 클래스 아이디
     */
    Long saveClass(String type, @Nullable String idClassName);

    /**
     * 객체 식별 정보 저장 처리
     * @param classId 클래스 아이디
     * @param oid 객체 식별 정보
     * @param sidId 보안 식별 정보 아이디
     * @param entriesInheriting 부모의 ACE 목록이 ACL 로 부터 상속 여부
     */
    void saveObjectIdentity(long classId, ObjectIdentity oid, long sidId, boolean entriesInheriting);

    /**
     * 객체 식별 정보에 해당하는 ACE 목록을 삭제 처리
     * @param oid 객체 식별 정보
     */
    void deleteEntries(ObjectIdentity oid);

    /**
     * 보안 식별 정보 목록 저장 처리
     * @param sids 보안 식별 정보 목록
     * @return 보안 식별 정보 아이디 목록
     */
    Set<Long> saveSids(List<Sid> sids);

    /**
     * ACL 에 해당하는 ACE 목록을 생성 처리
     * @param oidId 객체 식별 정보 아이디
     * @param entries 생성 대상 ACE 목록
     * @param sidIds 보안 식별 정보 아이디 목록
     */
    void saveEntries(long oidId, List<AccessControlEntry> entries, Set<Long> sidIds);

    /**
     * 객체 식별 정보 관리 삭제 처리
     * @param oid 객체 식별 정보
     */
    void deleteObjectIdentity(ObjectIdentity oid);

    /**
     * 객체 식별 정보 아이디 조회
     * @param oid 객체 식별 정보
     * @return 객체 식별 정보 아이디
     */
    Optional<Long> findObjectIdentityIdByObjectIdentity(ObjectIdentity oid);

    /**
     * 객체 식별 정보 수정 처리
     * @param id 객체 식별 정보 아이디
     * @param parentId 부모 아이디
     * @param ownerSid 소유 보안 식별 정보 아이디
     * @param entriesInheriting 부모의 ACE 목록이 ACL 로 부터 상속 여부
     * @throws NotFoundException 객체 식별 정보를 찾을 수 없는 경우 예외
     */
    void updateObjectIdentity(Serializable id, @Nullable Long parentId, long ownerSid, boolean entriesInheriting) throws NotFoundException;

    /**
     * 객체 식별 정보의 자식 목록을 조회
     * @param oid 객체 식별 정보
     * @return 자식 목록
     */
    List<ObjectIdentity> findChildren(ObjectIdentity oid);

}
