package com.jacknie.example.custom;

import lombok.RequiredArgsConstructor;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.jdbc.LookupStrategy;
import org.springframework.security.acls.model.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class CustomMutableAclService extends CustomAclService implements EnhancedMutableAclService {

    private final AclOperations aclOperations;
    private final LookupStrategy lookupStrategy;
    private final AclCache aclCache;

    @Override
    public MutableAcl createAcl(ObjectIdentity oid) throws AlreadyExistsException {
        Assert.notNull(oid, "Object Identity required");

        // Check this object identity hasn't already been persisted
        if (aclOperations.existsObjectIdentity(oid)) {
            throw new AlreadyExistsException("Object identity '" + oid + "' already exists");
        }

        // Need to retrieve the current principal, in order to know who "owns" this ACL
        // (can be changed later on)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        PrincipalSid sid = new PrincipalSid(auth);

        // Create the acl_object_identity row
        createObjectIdentity(oid, sid);

        // Retrieve the ACL via superclass (ensures cache registration, proper retrieval
        // etc)
        Acl acl = readAclById(oid);
        Assert.isInstanceOf(MutableAcl.class, acl, "MutableAcl should be been returned");

        return (MutableAcl) acl;
    }

    @Override
    public void deleteAcl(ObjectIdentity oid, boolean deleteChildren) throws ChildrenExistException {
        Assert.notNull(oid, "Object Identity required");
        Assert.notNull(oid.getIdentifier(), "Object Identity doesn't provide an identifier");

        if (deleteChildren) {
            traverseChildren(oid, child -> deleteAcl(child, true));
        }

        // Delete this ACL's ACEs in the acl_entry table
        aclOperations.deleteEntries(oid);

        // Delete this ACL's acl_object_identity row
        aclOperations.deleteObjectIdentity(oid);

        // Clear the cache
        aclCache.evictFromCache(oid);
    }

    @Override
    public MutableAcl updateAcl(MutableAcl acl) throws NotFoundException {
        Assert.notNull(acl.getId(), "Object Identity doesn't provide an identifier");

        // Update this ACL's ACEs in the acl_entry table
        updateEntries(acl);

        // Change the mutable columns in acl_object_identity
        updateObjectIdentity(acl);

        // Clear the cache, including children
        clearCacheIncludingChildren(acl.getObjectIdentity());

        // Retrieve the ACL via superclass (ensures cache registration, proper retrieval
        // etc)
        return (MutableAcl) readAclById(acl.getObjectIdentity());
    }

    @Override
    protected AclOperations getAclOperations() {
        return aclOperations;
    }

    @Override
    protected LookupStrategy getLookupStrategy() {
        return lookupStrategy;
    }

    private void clearCacheIncludingChildren(ObjectIdentity oid) {
        Assert.notNull(oid, "ObjectIdentity required");
        traverseChildren(oid, this::clearCacheIncludingChildren);
        aclCache.evictFromCache(oid);
    }

    private void traverseChildren(ObjectIdentity oid, Consumer<ObjectIdentity> action) {
        List<ObjectIdentity> children = findChildren(oid);
        if (children != null) {
            for (ObjectIdentity child : children) {
                action.accept(child);
            }
        }
    }


    /**
     * Creates an entry in the acl_object_identity table for the passed ObjectIdentity.
     * The Sid is also necessary, as acl_object_identity has defined the sid column as
     * non-null.
     * @param oid to represent an acl_object_identity for
     * @param owner for the SID column (will be created if there is no acl_sid entry for
     * this particular Sid already)
     */
    private void createObjectIdentity(ObjectIdentity oid, Sid owner) {
        Long sidId = createOrRetrieveSidPrimaryKey(owner, true);
        Long classId = createOrRetrieveClassPrimaryKey(oid.getType(), true, oid.getIdentifier().getClass());
        aclOperations.saveObjectIdentity(classId, oid, sidId, true);
    }

    /**
     * Retrieves the primary key from acl_sid, creating a new row if needed and the
     * allowCreate property is true.
     * @param sid to find or create
     * @param allowCreate true if creation is permitted if not found
     * @return the primary key or null if not found
     * @throws IllegalArgumentException if the <tt>Sid</tt> is not a recognized
     * implementation.
     */
    private Long createOrRetrieveSidPrimaryKey(Sid sid, boolean allowCreate) {
        SidHelper sidHelper = new SidHelper(sid);
        return createOrRetrieveSidPrimaryKey(sidHelper.getSid(), sidHelper.getType(), allowCreate);
    }

    /**
     * Retrieves the primary key from acl_sid, creating a new row if needed and the
     * allowCreate property is true.
     * @param sidName name of Sid to find or to create
     * @param sidType whether it's a user or granted authority like role
     * @param allowCreate true if creation is permitted if not found
     * @return the primary key or null if not found
     */
    private Long createOrRetrieveSidPrimaryKey(String sidName, SidType sidType, boolean allowCreate) {
        List<Long> sidIds = aclOperations.findSidIds(sidType, sidName);
        if (!sidIds.isEmpty()) {
            return sidIds.get(0);
        }
        if (allowCreate) {
            return aclOperations.saveSid(sidType, sidName);
        }
        return null;
    }

    /**
     * Retrieves the primary key from {@code acl_class}, creating a new row if needed and
     * the {@code allowCreate} property is {@code true}.
     * @param type to find or create an entry for (often the fully-qualified class name)
     * @param allowCreate true if creation is permitted if not found
     * @param idType target id class
     * @return the primary key or null if not found
     */
    private Long createOrRetrieveClassPrimaryKey(String type, boolean allowCreate, Class<?> idType) {
        List<Long> classIds = aclOperations.findClassIds(type);

        if (!classIds.isEmpty()) {
            return classIds.get(0);
        }

        if (allowCreate) {
            return aclOperations.isAclClassIdSupported()
                ? aclOperations.saveClass(type, idType.getCanonicalName())
                : aclOperations.saveClass(type);
        }

        return null;
    }

    private void updateEntries(MutableAcl acl) {
        long oidId = (Long) acl.getId();
        List<AccessControlEntry> entries = acl.getEntries();
        List<Sid> sids = entries.stream().map(AccessControlEntry::getSid).toList();
        Set<Long> sidIds = aclOperations.saveSids(sids);
        aclOperations.saveEntries(oidId, entries, sidIds);
    }

    /**
     * Updates an existing acl_object_identity row, with new information presented in the
     * passed MutableAcl object. Also will create an acl_sid entry if needed for the Sid
     * that owns the MutableAcl.
     * @param acl to modify (a row must already exist in acl_object_identity)
     * @throws NotFoundException if the ACL could not be found to update.
     */
    protected void updateObjectIdentity(MutableAcl acl) {
        Assert.notNull(acl.getOwner(), "Owner is required in this implementation");
        Long parentId = Optional.ofNullable(acl.getParentAcl())
            .map(Acl::getObjectIdentity)
            .flatMap(aclOperations::findObjectIdentityIdByObjectIdentity)
            .orElse(null);
        Long ownerSid = createOrRetrieveSidPrimaryKey(acl.getOwner(), true);
        aclOperations.updateObjectIdentity(acl.getId(), parentId, ownerSid, acl.isEntriesInheriting());
    }

}
