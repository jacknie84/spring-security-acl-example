package com.jacknie.example.repository.acl;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AclObjectIdentityRepository extends JpaRepository<AclObjectIdentity, Long> {

    Optional<AclObjectIdentity> findByObjectIdIdentity(Long objectIdIdentity);

    boolean existsByObjectIdIdentity(long objectIdIdentity);
}
