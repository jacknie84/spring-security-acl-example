package com.jacknie.example.repository.acl;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPQLQuery;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.util.CollectionUtils;

import java.util.*;

import static com.jacknie.example.repository.acl.QAclObjectIdentity.aclObjectIdentity;
import static com.querydsl.core.group.GroupBy.groupBy;

public class AclObjectIdentityCustomRepositoryImpl extends QuerydslRepositorySupport implements AclObjectIdentityCustomRepository {

    private final QAclSourceBasePart aclSourceBasePart = new QAclSourceBasePart(
        aclObjectIdentity.id,
        aclObjectIdentity.parentObject.id,
        aclObjectIdentity.entriesInheriting,
        aclObjectIdentity.objectIdIdentity,
        aclObjectIdentity.objectIdClass.classIdType,
        aclObjectIdentity.objectIdClass.className,
        aclObjectIdentity.ownerSid.type,
        aclObjectIdentity.ownerSid.sid
    );

    private final QObjectIdentitySource objectIdentitySource = new QObjectIdentitySource(
        aclObjectIdentity.objectIdIdentity,
        aclObjectIdentity.objectIdClass.className,
        aclObjectIdentity.objectIdClass.classIdType
    );

    public AclObjectIdentityCustomRepositoryImpl() {
        super(AclObjectIdentity.class);
    }

    @Override
    public boolean existsByObjectIdentity(ObjectIdentity oid) {
        return from(aclObjectIdentity)
                .leftJoin(aclObjectIdentity.objectIdClass)
                .where(
                    aclObjectIdentity.objectIdIdentity.eq(oid.getIdentifier().toString()),
                    aclObjectIdentity.objectIdClass.className.eq(oid.getType())
                )
                .fetchCount() > 0;
    }

    @Override
    public List<AclSourceBasePart> findAclSourceBasePartsByObjectIdentityIn(Set<ObjectIdentity> oids) {
        if (CollectionUtils.isEmpty(oids)) {
            return Collections.emptyList();
        } else {
            return getAclSourceBasePartQuery()
                .where(
                    oids.stream()
                        .map(oid -> aclObjectIdentity.objectIdIdentity.eq(oid.getIdentifier().toString()).and(aclObjectIdentity.objectIdClass.className.eq(oid.getType())))
                        .reduce(BooleanExpression::or)
                        .orElseThrow()
                )
                .fetch();
        }
    }

    @Override
    public List<AclSourceBasePart> findAclSourceBasePartsByObjectIdentityIdIn(Set<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        } else {
            return getAclSourceBasePartQuery()
                .where(
                    ids.stream()
                        .map(aclObjectIdentity.id::eq)
                        .reduce(BooleanExpression::or)
                        .orElseThrow()
                )
                .fetch();
        }
    }

    @Override
    public Optional<AclObjectIdentity> findByObjectIdentity(ObjectIdentity oid) {
        return Optional.ofNullable(from(aclObjectIdentity)
            .where(
                aclObjectIdentity.objectIdIdentity.eq(oid.getIdentifier().toString()),
                aclObjectIdentity.objectIdClass.className.eq(oid.getType())
            )
            .fetchOne());
    }

    @Override
    public Map<Long, AclObjectIdentity> findMapByIdIn(Set<Long> oidIds) {
        if (CollectionUtils.isEmpty(oidIds)) {
            return Collections.emptyMap();
        } else {
            return from(aclObjectIdentity)
                .where(aclObjectIdentity.id.in(oidIds))
                .transform(groupBy(aclObjectIdentity.id).as(aclObjectIdentity));
        }
    }

    @Override
    public Optional<Long> findIdByObjectIdentity(ObjectIdentity oid) {
        return Optional.ofNullable(from(aclObjectIdentity).select(aclObjectIdentity.id)
            .where(
                aclObjectIdentity.objectIdIdentity.eq(oid.getIdentifier().toString()),
                aclObjectIdentity.objectIdClass.className.eq(oid.getType())
            )
            .fetchOne());
    }

    @Override
    public List<ObjectIdentitySource> findChildrenByObjectIdentity(ObjectIdentity oid) {
        return from(aclObjectIdentity).select(objectIdentitySource)
            .leftJoin(aclObjectIdentity.parentObject)
            .leftJoin(aclObjectIdentity.objectIdClass)
            .where(
                aclObjectIdentity.parentObject.objectIdIdentity.eq(oid.getIdentifier().toString()),
                aclObjectIdentity.parentObject.objectIdClass.className.eq(oid.getType())
            )
            .fetch();
    }

    private JPQLQuery<AclSourceBasePart> getAclSourceBasePartQuery() {
        return from(aclObjectIdentity).select(aclSourceBasePart)
            .leftJoin(aclObjectIdentity.ownerSid)
            .leftJoin(aclObjectIdentity.objectIdClass)
            .leftJoin(aclObjectIdentity.parentObject);
    }
}
