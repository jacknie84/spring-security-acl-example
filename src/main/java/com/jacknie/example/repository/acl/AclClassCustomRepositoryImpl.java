package com.jacknie.example.repository.acl;

import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;

import java.util.List;

import static com.jacknie.example.repository.acl.QAclClass.aclClass;

public class AclClassCustomRepositoryImpl extends QuerydslRepositorySupport implements AclClassCustomRepository {

    public AclClassCustomRepositoryImpl() {
        super(AclClass.class);
    }

    @Override
    public List<Long> findIdsByType(String type) {
        return from(aclClass).select(aclClass.id)
            .where(aclClass.className.eq(type))
            .fetch();
    }
}
