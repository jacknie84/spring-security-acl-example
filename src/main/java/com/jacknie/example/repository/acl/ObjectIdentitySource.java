package com.jacknie.example.repository.acl;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;
import org.springframework.lang.Nullable;

@Data
public class ObjectIdentitySource {

    /**
     * 객체 식별 정보
     */
    private final String objectIdIdentity;

    /**
     * 객체 식별 정보 클래스 이름
     */
    private final String className;

    /**
     * 객체 식별 정보 클래스 기본 이름
     */
    private final String classIdType;

    @QueryProjection
    public ObjectIdentitySource(String objectIdIdentity, String className, @Nullable String classIdType) {
        this.objectIdIdentity = objectIdIdentity;
        this.className = className;
        this.classIdType = classIdType;
    }
}
