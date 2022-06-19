package com.jacknie.example.custom;

import com.jacknie.example.repository.acl.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.util.Pair;
import org.springframework.lang.Nullable;
import org.springframework.security.acls.domain.AccessControlEntryImpl;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.*;
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
    public void createObjectIdentity(ObjectIdentity oid, PrincipalSid owner) {
        Long sidId = createOrRetrieveSidPrimaryKey(owner);
        Long classId = createOrRetrieveClassPrimaryKey(oid.getType(), oid.getIdentifier().getClass());
        saveObjectIdentity(classId, oid, sidId);
    }

    @Override
    public void deleteAcl(ObjectIdentity oid, boolean deleteChildren) {
        Assert.notNull(oid, "Object Identity required");
        Assert.notNull(oid.getIdentifier(), "Object Identity doesn't provide an identifier");

        if (deleteChildren) {
            List<ObjectIdentity> children = findChildren(oid);
            if (children != null) {
                for (ObjectIdentity child : children) {
                    deleteAcl(child, true);
                }
            }
        }

        // Delete this ACL's ACEs in the acl_entry table
        deleteEntries(oid);
        // Delete this ACL's acl_object_identity row
        deleteObjectIdentity(oid);
    }

    @Override
    public void updateAcl(MutableAcl acl) {
        Assert.notNull(acl.getId(), "Object Identity doesn't provide an identifier");

        // Update this ACL's ACEs in the acl_entry table
        updateEntries(acl);
        // Change the mutable columns in acl_object_identity
        updateObjectIdentity(acl);
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

    private void saveObjectIdentity(@Nullable Long classId, ObjectIdentity oid, @Nullable Long sidId) {
        Assert.notNull(oid, "oid cannot be null");
        AclSid aclSid = Optional.ofNullable(sidId).flatMap(sidRepository::findById)
            .orElseThrow(() -> new IllegalArgumentException("cannot found AclSid entity by id: " + sidId));
        AclClass aclClass = Optional.ofNullable(classId).flatMap(classRepository::findById)
            .orElseThrow(() -> new IllegalArgumentException("cannot found AclClass entity by id: " + classId));
        AclObjectIdentity entity = AclObjectIdentity.builder()
            .objectIdIdentity(oid.getIdentifier().toString())
            .entriesInheriting(true)
            .ownerSid(aclSid)
            .objectIdClass(aclClass)
            .build();
        oidRepository.save(entity);
    }

    private Long saveSid(SidType sidType, String sidName) {
        Assert.notNull(sidType, "sidType cannot be null");
        Assert.hasText(sidName, "sidName cannot be blank");
        AclSid entity = AclSid.builder().type(sidType).sid(sidName).build();
        return sidRepository.save(entity).getId();
    }

    /**
     * Retrieves the primary key from acl_sid, creating a new row if needed and the
     * allowCreate property is true.
     * @param sid to find or create
     * @return the primary key or null if not found
     * @throws IllegalArgumentException if the <tt>Sid</tt> is not a recognized
     * implementation.
     */
    private Long createOrRetrieveSidPrimaryKey(Sid sid) throws IllegalArgumentException {
        SidHelper sidHelper = new SidHelper(sid);
        return createOrRetrieveSidPrimaryKey(sidHelper.getSid(), sidHelper.getType());
    }

    /**
     * Retrieves the primary key from acl_sid, creating a new row if needed and the
     * allowCreate property is true.
     * @param sidName name of Sid to find or to create
     * @param sidType whether it's a user or granted authority like role
     * @return the primary key or null if not found
     */
    private Long createOrRetrieveSidPrimaryKey(String sidName, SidType sidType) {
        return sidRepository.findIdByTypeAndSid(sidType, sidName)
            .orElseGet(() -> saveSid(sidType, sidName));
    }

    /**
     * Retrieves the primary key from {@code acl_class}, creating a new row if needed and
     * the {@code allowCreate} property is {@code true}.
     * @param type to find or create an entry for (often the fully-qualified class name)
     * @param idType target id class
     * @return the primary key or null if not found
     */
    private Long createOrRetrieveClassPrimaryKey(String type, Class<?> idType) {
        return classRepository.findIdByType(type)
            .orElseGet(() -> saveClass(type, idType.getCanonicalName()));
    }

    private Long saveClass(String type, @Nullable String idClassName) {
        AclClass entity = AclClass.builder().className(type).classIdType(idClassName).build();
        return classRepository.save(entity).getId();
    }

    private void deleteEntries(ObjectIdentity oid) {
        AclObjectIdentity aclOid = getAclObjectIdentity(oid);
        List<AclEntry> entities = entryRepository.findAllByObjectIdentity(aclOid);
        entryRepository.deleteAll(entities);
    }

    private void deleteObjectIdentity(ObjectIdentity oid) {
        AclObjectIdentity aclOid = getAclObjectIdentity(oid);
        oidRepository.delete(aclOid);
    }

    private void updateEntries(MutableAcl acl) {
        long oidId = (Long) acl.getId();
        List<AccessControlEntry> entries = acl.getEntries();
        List<Sid> sids = entries.stream().map(AccessControlEntry::getSid).toList();
        Set<Long> sidIds = saveSids(sids);
        saveEntries(oidId, entries, sidIds);
    }

    private Set<Long> saveSids(List<Sid> sids) {
        Set<Pair<String, SidType>> sidAndTypePairs = sids.stream()
            .map(SidHelper::new)
            .map(sid -> Pair.of(sid.getSid(), sid.getType()))
            .collect(Collectors.toSet());
        Map<Pair<String, SidType>, AclSid> sidMap = sidRepository.findMapBySidAndTypePairIn(sidAndTypePairs);
        return sidAndTypePairs.stream().map(pair -> getAclSidId(sidMap, pair)).collect(Collectors.toSet());
    }

    private void saveEntries(long oidId, List<AccessControlEntry> entries, Set<Long> sidIds) {
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

    /**
     * Updates an existing acl_object_identity row, with new information presented in the
     * past MutableAcl object. Also, will create an acl_sid entry if needed for the Sid
     * that owns the MutableAcl.
     * @param acl to modify (a row must already exist in acl_object_identity)
     * @throws NotFoundException if the ACL could not be found to update.
     */
    private void updateObjectIdentity(MutableAcl acl) {
        Assert.notNull(acl.getOwner(), "Owner is required in this implementation");
        Long parentId = Optional.ofNullable(acl.getParentAcl())
            .map(Acl::getObjectIdentity)
            .flatMap(oidRepository::findIdByObjectIdentity)
            .orElse(null);
        Long ownerSid = createOrRetrieveSidPrimaryKey(acl.getOwner());
        updateObjectIdentity(acl.getId(), parentId, ownerSid, acl.isEntriesInheriting());
    }

    private void updateObjectIdentity(Serializable id, @Nullable Long parentId, long ownerSid, boolean entriesInheriting) throws NotFoundException {
        Assert.notNull(id, "id cannot be null");
        AclObjectIdentity entity = oidRepository.findById((Long) id)
            .orElseThrow(() -> new NotFoundException("Unable to locate ACL to update"));
        Optional.ofNullable(parentId).flatMap(oidRepository::findById).ifPresent(entity::setParentObject);
        sidRepository.findById(ownerSid).ifPresent(entity::setOwnerSid);
        entity.setEntriesInheriting(entriesInheriting);
        oidRepository.save(entity);
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
