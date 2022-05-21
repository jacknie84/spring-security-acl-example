package com.jacknie.example.repository.acl;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AclObjectIdentityRepository extends JpaRepository<AclObjectIdentity, Long>, AclObjectIdentityCustomRepository {

    boolean existsByObjectIdIdentity(String objectIdIdentity);
}
