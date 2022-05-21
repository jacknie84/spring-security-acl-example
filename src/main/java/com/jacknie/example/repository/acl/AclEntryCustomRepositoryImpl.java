package com.jacknie.example.repository.acl;

import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.jacknie.example.repository.acl.QAclEntry.aclEntry;
import static com.querydsl.core.group.GroupBy.groupBy;
import static com.querydsl.core.group.GroupBy.list;

public class AclEntryCustomRepositoryImpl extends QuerydslRepositorySupport implements AclEntryCustomRepository {

    private QAclSourceAcePart aclSourceAcePart = new QAclSourceAcePart(
        aclEntry.id,
        aclEntry.sid.sid,
        aclEntry.sid.type,
        aclEntry.mask,
        aclEntry.granting,
        aclEntry.auditSuccess,
        aclEntry.auditFailure
    );

    public AclEntryCustomRepositoryImpl() {
        super(AclEntry.class);
    }

    @Override
    public Map<Long, List<AclSourceAcePart>> findAclSourceAcePartsMap(Set<Long> oidIds) {
        if (CollectionUtils.isEmpty(oidIds)) {
            return Collections.emptyMap();
        } else {
            return from(aclEntry)
                .leftJoin(aclEntry.sid)
                .where(aclEntry.objectIdentity.id.in(oidIds))
                .transform(groupBy(aclEntry.objectIdentity.id).as(list(aclSourceAcePart)));
        }
    }

    @Override
    public Map<Long, AclEntry> findMapByObjectIdentity(AclObjectIdentity aclOid) {
        return from(aclEntry)
            .where(aclEntry.objectIdentity.eq(aclOid))
            .transform(groupBy(aclEntry.id).as(aclEntry));
    }
}
