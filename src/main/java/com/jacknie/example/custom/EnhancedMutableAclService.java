package com.jacknie.example.custom;

import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.MutableAclService;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.security.acls.model.ObjectIdentity;

import java.io.Serializable;

public interface EnhancedMutableAclService extends MutableAclService {

    /**
     * ACL 객체 조회
     * @param object 대상 객체
     * @return ACL 객체
     */
    default MutableAcl getMutableAcl(Object object) {
        ObjectIdentity oid = new ObjectIdentityImpl(object);
        return getMutableAcl(oid);
    }

    /**
     * ACL 객체 조회
     * @param type 클래스
     * @param identifier 객체 식별 정보
     * @return ACL 객체
     */
    default MutableAcl getMutableAcl(Class<?> type, Serializable identifier) {
        ObjectIdentity oid = new ObjectIdentityImpl(type, identifier);
        return getMutableAcl(oid);
    }

    /**
     * ACL 객체 조회
     * @param type 객체 식별 정보 타입
     * @param identifier 객체 식별 정보
     * @return ACL 객체
     */
    default MutableAcl getMutableAcl(String type, Serializable identifier) {
        ObjectIdentity oid = new ObjectIdentityImpl(type, identifier);
        return getMutableAcl(oid);
    }

    /**
     * ACL 객체 조회
     * @param oid 객체 식별 정보
     * @return ACL 객체
     */
    default MutableAcl getMutableAcl(ObjectIdentity oid) {
        try {
            return (MutableAcl) readAclById(oid);
        } catch (NotFoundException e) {
            return createAcl(oid);
        }
    }

}
