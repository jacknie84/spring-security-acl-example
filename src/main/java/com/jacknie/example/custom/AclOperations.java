package com.jacknie.example.custom;

import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.ObjectIdentity;

import java.util.List;

public interface AclOperations {

    /**
     * 해당 객체 식별 정보가 관리 되고 있는지 확인
     * @param oid 객체 식별 정보
     * @return 관리 여부
     */
    boolean existsObjectIdentity(ObjectIdentity oid);

    /**
     * Creates an entry in the acl_object_identity table for the passed ObjectIdentity.
     * The Sid is also necessary, as acl_object_identity has defined the sid column as
     * non-null.
     * @param oid to represent an acl_object_identity for
     * @param owner for the SID column (will be created if there is no acl_sid entry for
     * this particular Sid already)
     */
    void createObjectIdentity(ObjectIdentity oid, PrincipalSid owner);

    /**
     * ACL 삭제 처리
     * @param oid 객체 식별 정보
     * @param deleteChildren 자식 삭제 처리 여부
     */
    void deleteAcl(ObjectIdentity oid, boolean deleteChildren);

    /**
     * ACL 수정 처리
     * @param acl ACL 객체
     */
    void updateAcl(MutableAcl acl);

    /**
     * 객체 식별 정보의 자식 목록을 조회
     * @param oid 객체 식별 정보
     * @return 자식 목록
     */
    List<ObjectIdentity> findChildren(ObjectIdentity oid);

}
