package com.jacknie.example.repository.acl;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AclClassRepository extends JpaRepository<AclClass, Long> {

    Optional<AclClass> findByClassName(String className);

    boolean existsByClassName(String className);
}
