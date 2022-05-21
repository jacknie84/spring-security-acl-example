package com.jacknie.example.custom;

import lombok.RequiredArgsConstructor;
import org.springframework.security.acls.domain.*;
import org.springframework.security.acls.jdbc.LookupStrategy;
import org.springframework.security.acls.model.*;
import org.springframework.security.util.FieldUtils;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.*;

@RequiredArgsConstructor
public class CustomLookupStrategy implements LookupStrategy {

    private final LookupOperations lookupOperations;
    private final AclCache aclCache;
    private final AclAuthorizationStrategy aclAuthorizationStrategy;
    private final PermissionGrantingStrategy grantingStrategy;
    private final Field fieldAces = FieldUtils.getField(AclImpl.class, "aces");
    private final Field fieldAcl = FieldUtils.getField(AccessControlEntryImpl.class, "acl");

    private PermissionFactory permissionFactory = new DefaultPermissionFactory();
    private int batchSize = 50;

    @Override
    public Map<ObjectIdentity, Acl> readAclsById(List<ObjectIdentity> oids, List<Sid> sids) {
        Assert.notEmpty(oids, "Objects to lookup required");
        // Map<ObjectIdentity, Acl>
        // contains FULLY loaded Acl objects
        Map<ObjectIdentity, Acl> result = new HashMap<>();
        Set<ObjectIdentity> currentBatchToLoad = new HashSet<>();
        for (int i = 0; i < oids.size(); i++) {
            ObjectIdentity oid = oids.get(i);
            // Check we don't already have this ACL in the results
            boolean aclFound = result.containsKey(oid);
            // Check cache for the present ACL entry
            if (!aclFound) {
                Acl acl = aclCache.getFromCache(oid);
                // Ensure any cached element supports all the requested SIDs
                // (they should always, as our base impl doesn't filter on SID)
                if (acl != null) {
                    Assert.state(acl.isSidLoaded(sids),
                            "Error: SID-filtered element detected when implementation does not perform SID filtering "
                                    + "- have you added something to the cache manually?");
                    result.put(acl.getObjectIdentity(), acl);
                    aclFound = true;
                }
            }
            // Load the ACL from the database
            if (!aclFound) {
                currentBatchToLoad.add(oid);
            }
            // Is it time to load from JDBC the currentBatchToLoad?
            if ((currentBatchToLoad.size() == batchSize) || ((i + 1) == oids.size())) {
                if (currentBatchToLoad.size() > 0) {
                    Map<ObjectIdentity, Acl> loadedBatch = lookupObjectIdentities(currentBatchToLoad, sids);
                    // Add loaded batch (all elements 100% initialized) to results
                    result.putAll(loadedBatch);
                    // Add the loaded batch to the cache
                    for (Acl loadedAcl : loadedBatch.values()) {
                        aclCache.putInCache((AclImpl) loadedAcl);
                    }
                    currentBatchToLoad.clear();
                }
            }
        }
        return result;
    }

    public final void setBatchSize(int batchSize) {
        Assert.isTrue(batchSize >= 1, "BatchSize must be >= 1");
        this.batchSize = batchSize;
    }

    public final void setPermissionFactory(PermissionFactory permissionFactory) {
        Assert.notNull(permissionFactory, "permissionFactory cannot be null");
        this.permissionFactory = permissionFactory;
    }

    private Map<ObjectIdentity, Acl> lookupObjectIdentities(Set<ObjectIdentity> oids, List<Sid> sids) {
        Assert.notEmpty(oids, "Must provide identities to lookup");

        // contains Acls with StubAclParents
        Map<Serializable, Acl> acls = new HashMap<>();

        // Make the "acls" map contain all requested objectIdentities
        // (including markers to each parent in the hierarchy)
        List<AclSource> sources = lookupOperations.findAclSourcesByObjectIdentityIn(oids);
        Set<Long> parentsToLookup = processAclSources(sids, acls, sources);

        // Lookup the parents, now that our JdbcTemplate has released the database
        // connection (SEC-547)
        if (parentsToLookup.size() > 0) {
            lookupPrimaryKeys(acls, parentsToLookup, sids);
        }

        // Finally, convert our "acls" containing StubAclParents into true Acls
        Map<ObjectIdentity, Acl> resultMap = new HashMap<>();
        for (Acl inputAcl : acls.values()) {
            Assert.isInstanceOf(AclImpl.class, inputAcl, "Map should have contained an AclImpl");
            Assert.isInstanceOf(Long.class, ((AclImpl) inputAcl).getId(), "Acl.getId() must be Long");
            Acl result = convert(acls, (Long) ((AclImpl) inputAcl).getId());
            resultMap.put(result.getObjectIdentity(), result);
        }

        return resultMap;
    }

    /**
     * Locates the primary key IDs specified in "findNow", adding AclImpl instances with
     * StubAclParents to the "acls" Map.
     * @param acls the AclImpls (with StubAclParents)
     * @param findNow Long-based primary keys to retrieve
     * @param sids SIDs
     */
    private void lookupPrimaryKeys(Map<Serializable, Acl> acls, Set<Long> findNow, List<Sid> sids) {
        Assert.notNull(acls, "ACLs are required");
        Assert.notEmpty(findNow, "Items to find now required");
        List<AclSource> sources = lookupOperations.findAclSourcesByObjectIdentityIdIn(findNow);
        Set<Long> parentsToLookup = processAclSources(sids, acls, sources);
        // Lookup the parents, now that our JdbcTemplate has released the database
        // connection (SEC-547)
        if (parentsToLookup.size() > 0) {
            lookupPrimaryKeys(acls, parentsToLookup, sids);
        }
    }

    private Set<Long> processAclSources(List<Sid> sids, Map<Serializable, Acl> acls, List<AclSource> sources) {
        Set<Long> parentIdsToLookup = new HashSet<>(); // Set of parent_id Longs

        for (AclSource source : sources) {
            // Convert current row into an Acl (albeit with a StubAclParent)
            convertCurrentResultIntoObject(acls, source);

            // Figure out if this row means we need to look up another parent
            Long parentId = source.getAclParentId();

            if (parentId != null) {
                // See if it's already in the "acls"
                if (acls.containsKey(parentId)) {
                    continue; // skip this while iteration
                }

                // Now try to find it in the cache
                MutableAcl cached = aclCache.getFromCache(parentId);
                if ((cached == null) || !cached.isSidLoaded(sids)) {
                    parentIdsToLookup.add(parentId);
                }
                else {
                    // Pop into the acls map, so our convert method doesn't
                    // need to deal with an unsynchronized AclCache
                    acls.put(cached.getId(), cached);
                }
            }
        }

        // Return the parents left to look up to the caller
        return parentIdsToLookup;
    }

    private void convertCurrentResultIntoObject(Map<Serializable, Acl> acls, AclSource source) {
        long id = source.getAclId();

        // If we already have an ACL for this ID, just create the ACE
        Acl acl = acls.get(id);

        if (acl == null) {
            ObjectIdentity oid = new ObjectIdentityImpl(source.getType(), source.getIdentifier());
            Sid owner = source.getAclSidType().createSid(source.getAclSid());

            Acl parentAcl = null;
            if (source.getAclParentId() != null) {
                parentAcl = new StubAclParent(source.getAclParentId());
            }

            acl = new AclImpl(
                    oid,
                    id,
                    aclAuthorizationStrategy,
                    grantingStrategy,
                    parentAcl,
                    null,
                    source.isEntriesInheriting(),
                    owner
            );

            acls.put(id, acl);
        }

        // Add an extra ACE to the ACL (ORDER BY maintains the ACE list order)
        // It is permissible to have no ACEs in an ACL (which is detected by a null
        // ACE_SID)
        if (source.getAceSid() != null) {
            long aceId = source.getAceId();
            Sid recipient = source.getAceSidType().createSid(source.getAceSid());

            int mask = source.getMask();
            Permission permission = permissionFactory.buildFromMask(mask);

            AccessControlEntryImpl ace = new AccessControlEntryImpl(
                    aceId,
                    acl,
                    recipient,
                    permission,
                    source.isGranting(),
                    source.isAuditSuccess(),
                    source.isAuditFailure()
            );

            // Field acesField = FieldUtils.getField(AclImpl.class, "aces");
            List<AccessControlEntryImpl> aces = readAces((AclImpl) acl);

            // Add the ACE if it doesn't already exist in the ACL.aces field
            if (!aces.contains(ace)) {
                aces.add(ace);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private List<AccessControlEntryImpl> readAces(AclImpl acl) {
        try {
            ReflectionUtils.makeAccessible(this.fieldAces);
            return (List<AccessControlEntryImpl>) this.fieldAces.get(acl);
        }
        catch (IllegalAccessException ex) {
            throw new IllegalStateException("Could not obtain AclImpl.aces field", ex);
        }
    }

    private void setAclOnAce(AccessControlEntryImpl ace, AclImpl acl) {
        try {
            ReflectionUtils.makeAccessible(this.fieldAcl);
            this.fieldAcl.set(ace, acl);
        }
        catch (IllegalAccessException ex) {
            throw new IllegalStateException("Could not or set AclImpl on AccessControlEntryImpl fields", ex);
        }
    }

    private void setAces(AclImpl acl, List<AccessControlEntryImpl> aces) {
        try {
            ReflectionUtils.makeAccessible(this.fieldAces);
            this.fieldAces.set(acl, aces);
        }
        catch (IllegalAccessException ex) {
            throw new IllegalStateException("Could not set AclImpl entries", ex);
        }
    }

    /**
     * The final phase of converting the <code>Map</code> of <code>AclImpl</code>
     * instances which contain <code>StubAclParent</code>s into proper, valid
     * <code>AclImpl</code>s with correct ACL parents.
     * @param inputMap the unconverted <code>AclImpl</code>s
     * @param currentIdentity the current<code>Acl</code> that we wish to convert (this
     * may be
     */
    private AclImpl convert(Map<Serializable, Acl> inputMap, Long currentIdentity) {
        Assert.notEmpty(inputMap, "InputMap required");
        Assert.notNull(currentIdentity, "CurrentIdentity required");

        // Retrieve this Acl from the InputMap
        Acl uncastAcl = inputMap.get(currentIdentity);
        Assert.isInstanceOf(AclImpl.class, uncastAcl, "The inputMap contained a non-AclImpl");

        AclImpl inputAcl = (AclImpl) uncastAcl;

        Acl parent = inputAcl.getParentAcl();

        if (parent instanceof StubAclParent stubAclParent) {
            // Lookup the parent
            parent = convert(inputMap, stubAclParent.getId());
        }

        // Now we have the parent (if there is one), create the true AclImpl
        AclImpl result = new AclImpl(
            inputAcl.getObjectIdentity(),
            inputAcl.getId(),
            this.aclAuthorizationStrategy,
            this.grantingStrategy,
            parent,
            null,
            inputAcl.isEntriesInheriting(),
            inputAcl.getOwner()
        );

        // Copy the "aces" from the input to the destination

        // Obtain the "aces" from the input ACL
        List<AccessControlEntryImpl> aces = readAces(inputAcl);

        // Create a list in which to store the "aces" for the "result" AclImpl instance
        List<AccessControlEntryImpl> acesNew = new ArrayList<>();

        // Iterate over the "aces" input and replace each nested
        // AccessControlEntryImpl.getAcl() with the new "result" AclImpl instance
        // This ensures StubAclParent instances are removed, as per SEC-951
        for (AccessControlEntryImpl ace : aces) {
            setAclOnAce(ace, result);
            acesNew.add(ace);
        }

        // Finally, now that the "aces" have been converted to have the "result" AclImpl
        // instance, modify the "result" AclImpl instance
        setAces(result, acesNew);

        return result;
    }

    private static class StubAclParent implements Acl {

        private final Long id;

        StubAclParent(Long id) {
            this.id = id;
        }

        Long getId() {
            return this.id;
        }

        @Override
        public List<AccessControlEntry> getEntries() {
            throw new UnsupportedOperationException("Stub only");
        }

        @Override
        public ObjectIdentity getObjectIdentity() {
            throw new UnsupportedOperationException("Stub only");
        }

        @Override
        public Sid getOwner() {
            throw new UnsupportedOperationException("Stub only");
        }

        @Override
        public Acl getParentAcl() {
            throw new UnsupportedOperationException("Stub only");
        }

        @Override
        public boolean isEntriesInheriting() {
            throw new UnsupportedOperationException("Stub only");
        }

        @Override
        public boolean isGranted(List<Permission> permission, List<Sid> sids, boolean administrativeMode)
                throws NotFoundException, UnloadedSidException {
            throw new UnsupportedOperationException("Stub only");
        }

        @Override
        public boolean isSidLoaded(List<Sid> sids) {
            throw new UnsupportedOperationException("Stub only");
        }

    }
}
