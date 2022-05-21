package com.jacknie.example.repository.acl;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AclClassRepository extends JpaRepository<AclClass, Long>, AclClassCustomRepository {

    boolean existsByClassName(String className);
}
