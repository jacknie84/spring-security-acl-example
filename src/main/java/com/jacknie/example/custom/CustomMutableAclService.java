package com.jacknie.example.custom;

import lombok.RequiredArgsConstructor;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.jdbc.LookupStrategy;
import org.springframework.security.acls.model.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.Assert;

import java.util.List;

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
        aclOperations.createObjectIdentity(oid, sid);

        // Retrieve the ACL via superclass (ensures cache registration, proper retrieval
        // etc)
        Acl acl = readAclById(oid);
        Assert.isInstanceOf(MutableAcl.class, acl, "MutableAcl should be been returned");

        return (MutableAcl) acl;
    }

    @Override
    public void deleteAcl(ObjectIdentity oid, boolean deleteChildren) throws ChildrenExistException {
        aclOperations.deleteAcl(oid, deleteChildren);

        // Clear the cache
        aclCache.evictFromCache(oid);
    }

    @Override
    public MutableAcl updateAcl(MutableAcl acl) throws NotFoundException {
        aclOperations.updateAcl(acl);

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

        List<ObjectIdentity> children = findChildren(oid);
        if (children != null) {
            for (ObjectIdentity child : children) {
                clearCacheIncludingChildren(child);
            }
        }

        aclCache.evictFromCache(oid);
    }

}
