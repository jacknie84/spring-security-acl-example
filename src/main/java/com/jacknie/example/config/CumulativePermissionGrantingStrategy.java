package com.jacknie.example.config;

import org.springframework.security.acls.domain.AuditLogger;
import org.springframework.security.acls.domain.DefaultPermissionGrantingStrategy;
import org.springframework.security.acls.model.AccessControlEntry;
import org.springframework.security.acls.model.Permission;

public class CumulativePermissionGrantingStrategy extends DefaultPermissionGrantingStrategy {

    public CumulativePermissionGrantingStrategy(AuditLogger auditLogger) {
        super(auditLogger);
    }

    @Override
    protected boolean isGranted(AccessControlEntry ace, Permission p) {
        if (ace.isGranting() && ace.getPermission().getMask() != 0) {
            return (ace.getPermission().getMask() & p.getMask()) != 0;
        } else {
            return super.isGranted(ace, p);
        }
    }
}
