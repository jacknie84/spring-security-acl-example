package com.jacknie.example.repository.acl;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AclSidRepository extends JpaRepository<AclSid, Long>, AclSidCustomRepository {

    boolean existsBySid(String sid);

}
