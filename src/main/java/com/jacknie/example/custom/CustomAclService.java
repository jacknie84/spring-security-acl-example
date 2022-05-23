package com.jacknie.example.custom;

import org.springframework.security.acls.jdbc.LookupStrategy;
import org.springframework.security.acls.model.*;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class CustomAclService implements AclService {

    @Override
    public List<ObjectIdentity> findChildren(ObjectIdentity oid) {
        return getAclOperations().findChildren(oid);
    }

    @Override
    public Acl readAclById(ObjectIdentity oid) throws NotFoundException {
        return readAclById(oid, Collections.emptyList());
    }

    @Override
    public Acl readAclById(ObjectIdentity object, List<Sid> sids) throws NotFoundException {
        Map<ObjectIdentity, Acl> map = readAclsById(Collections.singletonList(object), sids);
        Assert.isTrue(map.containsKey(object),
                () -> "There should have been an Acl entry for ObjectIdentity " + object);
        return map.get(object);
    }

    @Override
    public Map<ObjectIdentity, Acl> readAclsById(List<ObjectIdentity> objects) throws NotFoundException {
        return readAclsById(objects, Collections.emptyList());
    }

    @Override
    public Map<ObjectIdentity, Acl> readAclsById(List<ObjectIdentity> oids, List<Sid> sids) throws NotFoundException {
        Map<ObjectIdentity, Acl> result = getLookupStrategy().readAclsById(oids, sids);
        // Check every requested object identity was found (throw NotFoundException if needed)
        for (ObjectIdentity oid : oids) {
            if (!result.containsKey(oid)) {
                throw new NotFoundException("Unable to find ACL information for object identity '" + oid + "'");
            }
        }
        return result;
    }

    /**
     * @return ACL 데이터 처리 명령 목록
     */
    protected abstract AclOperations getAclOperations();

    /**
     * @return ACL 데이터 검색
     */
    protected abstract LookupStrategy getLookupStrategy();
}
