package com.jacknie.example.custom;

import com.jacknie.example.repository.acl.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.util.Pair;
import org.springframework.lang.Nullable;
import org.springframework.security.acls.domain.AccessControlEntryImpl;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.model.AccessControlEntry;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.Sid;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Slf4j
@RequiredArgsConstructor
public class OperationsImpl implements AclOperations, LookupOperations {

    private final AclClassRepository classRepository;
    private final AclSidRepository sidRepository;
    private final AclObjectIdentityRepository oidRepository;
    private final AclEntryRepository entryRepository;
    private final ConversionService objectIdentifierConversionService;

    @Override
    public boolean existsObjectIdentity(ObjectIdentity oid) {
        Assert.hasText(oid.getType(), "ObjectIdentity type cannot be blank");
        Assert.notNull(oid.getIdentifier(), "ObjectIdentity identifier cannot be null");
        return oidRepository.existsByObjectIdentity(oid);
    }

    @Override
    public List<Long> findSidIds(SidType sidType, String sidName) {
        Assert.notNull(sidType, "sidType cannot be null");
        Assert.hasText(sidName, "sidName cannot be blank");
        return sidRepository.findIdsByTypeAndSid(sidType, sidName);
    }

    @Override
    public Long saveSid(SidType sidType, String sidName) {
        Assert.notNull(sidType, "sidType cannot be null");
        Assert.hasText(sidName, "sidName cannot be blank");
        AclSid entity = AclSid.builder().type(sidType).sid(sidName).build();
        return sidRepository.save(entity).getId();
    }

    @Override
    public List<Long> findClassIds(String type) {
        Assert.hasText(type, "type cannot be blank");
        return classRepository.findIdsByType(type);
    }

    @Override
    public boolean isAclClassIdSupported() {
        return true;
    }

    @Override
    public Long saveClass(String type) {
        return saveClass(type, null);
    }

    @Override
    public Long saveClass(String type, @Nullable String idClassName) {
        AclClass entity = AclClass.builder().className(type).classIdType(idClassName).build();
        return classRepository.save(entity).getId();
    }

    @Override
    public void saveObjectIdentity(long classId, ObjectIdentity oid, long sidId, boolean entriesInheriting) {
        Assert.notNull(oid, "oid cannot be null");
        AclSid aclSid = sidRepository.findById(sidId)
            .orElseThrow(() -> new IllegalArgumentException("cannot found AclSid entity by id: " + sidId));
        AclClass aclClass = classRepository.findById(classId)
            .orElseThrow(() -> new IllegalArgumentException("cannot found AclClass entity by id: " + classId));
        AclObjectIdentity entity = AclObjectIdentity.builder()
            .objectIdIdentity(oid.getIdentifier().toString())
            .entriesInheriting(entriesInheriting)
            .ownerSid(aclSid)
            .objectIdClass(aclClass)
            .build();
        oidRepository.save(entity);
    }

    @Override
    public void deleteEntries(ObjectIdentity oid) {
        AclObjectIdentity aclOid = getAclObjectIdentity(oid);
        List<AclEntry> entities = entryRepository.findAllByObjectIdentity(aclOid);
        entryRepository.deleteAll(entities);
    }

    @Override
    public void deleteObjectIdentity(ObjectIdentity oid) {
        AclObjectIdentity aclOid = getAclObjectIdentity(oid);
        oidRepository.delete(aclOid);
    }

    @Override
    public Set<Long> saveSids(List<Sid> sids) {
        Set<Pair<String, SidType>> sidAndTypePairs = sids.stream()
            .map(SidHelper::new)
            .map(sid -> Pair.of(sid.getSid(), sid.getType()))
            .collect(Collectors.toSet());
        Map<Pair<String, SidType>, AclSid> sidMap = sidRepository.findMapBySidAndTypePairIn(sidAndTypePairs);
        return sidAndTypePairs.stream().map(pair -> getAclSidId(sidMap, pair)).collect(Collectors.toSet());
    }

    @Override
    public void saveEntries(long oidId, List<AccessControlEntry> entries, Set<Long> sidIds) {
        AclObjectIdentity aclOid = oidRepository.findById(oidId).orElseThrow();
        Map<Long, AclEntry> entityMap = entryRepository.findMapByObjectIdentity(aclOid);
        Map<Pair<String, SidType>, AclSid> sidMap = sidRepository.findMapByIdIn(sidIds);
        Set<Long> ids = IntStream.range(0, entries.size())
            .mapToObj(i -> Pair.of(entries.get(i), i))
            .map(pair -> saveEntry(pair.getFirst(), pair.getSecond(), entityMap, aclOid, sidMap))
            .collect(Collectors.toSet());
        entityMap.entrySet().stream()
            .filter(entry -> !ids.contains(entry.getKey()))
            .map(Map.Entry::getValue)
            .forEach(entryRepository::delete);
    }

    @Override
    public Optional<Long> findObjectIdentityIdByObjectIdentity(ObjectIdentity oid) {
        Assert.notNull(oid, "oid cannot be null");
        return oidRepository.findIdByObjectIdentity(oid);
    }

    @Override
    public void updateObjectIdentity(Serializable id, @Nullable Long parentId, long ownerSid, boolean entriesInheriting) throws NotFoundException {
        Assert.notNull(id, "id cannot be null");
        AclObjectIdentity entity = oidRepository.findById((Long) id)
            .orElseThrow(() -> new NotFoundException("Unable to locate ACL to update"));
        Optional.ofNullable(parentId).flatMap(oidRepository::findById).ifPresent(entity::setParentObject);
        sidRepository.findById(ownerSid).ifPresent(entity::setOwnerSid);
        entity.setEntriesInheriting(entriesInheriting);
        oidRepository.save(entity);
    }

    @Override
    public List<ObjectIdentity> findChildren(ObjectIdentity oid) {
        Assert.notNull(oid, "oid cannot be null");
        List<ObjectIdentitySource> children = oidRepository.findChildrenByObjectIdentity(oid);
        return children.stream().map(this::toObjectIdentity).toList();
    }

    @Override
    public List<AclSource> findAclSourcesByObjectIdentityIn(Set<ObjectIdentity> oids) {
        List<AclSourceBasePart> baseParts = oidRepository.findAclSourceBasePartsByObjectIdentityIn(oids);
        Set<Long> oidIds = baseParts.stream().map(AclSourceBasePart::getObjectIdentityId).collect(Collectors.toSet());
        Map<Long, List<AclSourceAcePart>> acePartsMap = entryRepository.findAclSourceAcePartsMap(oidIds);
        return baseParts.stream().flatMap(basePart -> toAclSourceStream(basePart, acePartsMap)).toList();
    }

    @Override
    public List<AclSource> findAclSourcesByObjectIdentityIdIn(Set<Long> ids) {
        List<AclSourceBasePart> baseParts = oidRepository.findAclSourceBasePartsByObjectIdentityIdIn(ids);
        Set<Long> oidIds = baseParts.stream().map(AclSourceBasePart::getObjectIdentityId).collect(Collectors.toSet());
        Map<Long, List<AclSourceAcePart>> acePartsMap = entryRepository.findAclSourceAcePartsMap(oidIds);
        return baseParts.stream().flatMap(basePart -> toAclSourceStream(basePart, acePartsMap)).toList();
    }

    private Stream<AclSource> toAclSourceStream(AclSourceBasePart basePart, Map<Long, List<AclSourceAcePart>> acePartsMap) {
        List<AclSourceAcePart> aceParts = acePartsMap.getOrDefault(basePart.getObjectIdentityId(), Collections.emptyList());
        if (aceParts.isEmpty()) {
            return Stream.of(toAclSource(basePart));
        } else {
            return aceParts.stream().map(acePart -> toAclSource(basePart, acePart));
        }
    }

    private AclSource toAclSource(AclSourceBasePart basePart) {
        return toAclSource(basePart, null);
    }

    private AclSource toAclSource(AclSourceBasePart basePart, @Nullable AclSourceAcePart acePart) {
        AclSource.AclSourceBuilder builder = AclSource.builder()
            .aclId(basePart.getObjectIdentityId())
            .aclParentId(basePart.getParentObjectIdentityId())
            .entriesInheriting(Optional.ofNullable(basePart.getEntriesInheriting()).orElse(false))
            .identifier(Optional.ofNullable(basePart.getClassIdType())
                .map(this::toClass)
                .map(type -> convert(basePart.getObjectIdIdentity(), type))
                .orElse(basePart.getObjectIdIdentity()))
            .type(basePart.getClassName())
            .aclSidType(basePart.getSidType())
            .aclSid(basePart.getSid());
        if (acePart != null) {
            builder
                .aceId(acePart.getId())
                .aceSid(acePart.getSid())
                .aceSidType(acePart.getSidType())
                .mask(Optional.ofNullable(acePart.getMask()).orElse(0))
                .granting(Optional.ofNullable(acePart.getGranting()).orElse(false))
                .auditSuccess(Optional.ofNullable(acePart.getAuditSuccess()).orElse(false))
                .auditFailure(Optional.ofNullable(acePart.getAuditFailure()).orElse(false));
        }
        return builder.build();
    }

    private AclObjectIdentity getAclObjectIdentity(ObjectIdentity oid) {
        Assert.notNull(oid, "oid cannot be null");
        return oidRepository.findByObjectIdentity(oid)
            .orElseThrow(() -> new IllegalArgumentException("cannot found AclObjectIdentity entity by ObjectIdentity: " + oid));
    }

    private ObjectIdentity toObjectIdentity(ObjectIdentitySource source) {
        Serializable identifier = Optional.ofNullable(source.getClassIdType())
            .map(this::toClass)
            .map(type -> convert(source.getObjectIdIdentity(), type))
            .orElse(source.getObjectIdIdentity());
        return new ObjectIdentityImpl(source.getClassName(), identifier);
    }

    @SuppressWarnings("unchecked")
    private <T extends Serializable> Class<T> toClass(String className) {
        try {
            return (Class<T>) Class.forName(className);
        }
        catch (ClassNotFoundException ex) {
            log.debug("Unable to find class type on classpath", ex);
            return null;
        }
    }

    private <T extends Serializable> T convert(String objectIdIdentity, Class<T> targetType) {
        if (objectIdentifierConversionService.canConvert(String.class, targetType)) {
            return objectIdentifierConversionService.convert(objectIdIdentity, targetType);
        } else {
            return null;
        }
    }

    private Long getAclSidId(Map<Pair<String, SidType>, AclSid> sidMap, Pair<String, SidType> pair) {
        AclSid entity = sidMap.get(pair);
        if (entity == null) {
            return saveSid(pair.getSecond(), pair.getFirst());
        } else {
            return entity.getId();
        }
    }

    private long saveEntry(
        AccessControlEntry entry,
        int aceOrder,
        Map<Long, AclEntry> entityMap,
        AclObjectIdentity aclOid,
        Map<Pair<String, SidType>, AclSid> sidMap
    ) {
        AclEntry entity = entry.getId() != null
            ? updateAclEntity(entry, aceOrder, entityMap)
            : createAclEntity(entry, aceOrder, aclOid, sidMap);
        return entity.getId();
    }

    private AclEntry updateAclEntity(AccessControlEntry entry, int aceOrder, Map<Long, AclEntry> entityMap) {
        AclEntry entity = Optional.ofNullable(entityMap.get((Long) entry.getId())).orElseThrow();
        entity.setAceOrder(aceOrder);
        entity.setMask(entry.getPermission().getMask());
        return entryRepository.save(entity);
    }

    private AclEntry createAclEntity(AccessControlEntry entry, int aceOrder, AclObjectIdentity aclOid, Map<Pair<String, SidType>, AclSid> sidMap) {
        Assert.isTrue(entry instanceof AccessControlEntryImpl, "Unknown ACE class");
        AccessControlEntryImpl entryImpl = (AccessControlEntryImpl) entry;
        SidHelper sidHelper = new SidHelper(entry.getSid());
        AclSid aclSid = sidMap.get(Pair.of(sidHelper.getSid(), sidHelper.getType()));
        AclEntry entity = AclEntry.builder()
            .aceOrder(aceOrder)
            .mask(entry.getPermission().getMask())
            .granting(entry.isGranting())
            .auditSuccess(entryImpl.isAuditSuccess())
            .auditFailure(entryImpl.isAuditFailure())
            .objectIdentity(aclOid)
            .sid(aclSid)
            .build();
        return entryRepository.save(entity);
    }
}
