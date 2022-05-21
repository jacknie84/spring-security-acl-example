package com.jacknie.example.repository.acl;

import java.util.List;

public interface AclClassCustomRepository {

    /**
     * 클래스 아이디 목록 조회
     * @param type 클래스 타입
     * @return 클래스 아이디 목록
     */
    List<Long> findIdsByType(String type);
}
