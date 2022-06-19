package com.jacknie.example.repository.acl;

import java.util.Optional;

public interface AclClassCustomRepository {

    /**
     * 클래스 아이디 조회
     * @param type 클래스 타입
     * @return 클래스 아이디
     */
    Optional<Long> findIdByType(String type);
}
