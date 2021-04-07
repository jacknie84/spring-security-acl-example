package com.jacknie.example.service;

import com.jacknie.example.repository.acl.AclClassRepository;
import com.jacknie.example.repository.acl.AclEntryRepository;
import com.jacknie.example.repository.acl.AclObjectIdentityRepository;
import com.jacknie.example.repository.acl.AclSidRepository;
import com.jacknie.example.repository.community.CommunityMessage;
import com.jacknie.example.repository.community.CommunityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.acls.model.MutableAclService;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import static org.springframework.security.acls.domain.BasePermission.*;

public class CommunityServiceTest {

    protected final List<Permission> allPermissions = List.of(ADMINISTRATION, READ, WRITE, DELETE, CREATE);

    @Autowired
    protected CommunityService communityService;

    @Autowired
    protected MutableAclService mutableAclService;

    @Autowired
    protected CommunityRepository communityRepository;

    @Autowired
    protected AclClassRepository aclClassRepository;

    @Autowired
    protected AclEntryRepository aclEntryRepository;

    @Autowired
    protected AclObjectIdentityRepository aclObjectIdentityRepository;

    @Autowired
    protected AclSidRepository aclSidRepository;

    protected long createCommunityByAdmin(String subject, String username) {
        return adminSecurityContext(() -> communityService.createCommunity(subject, username));
    }

    protected void inviteCommunity(String owner, long id, String username) {
        virtualSecurityContext(owner, () -> communityService.inviteCommunity(id, username));
    }

    protected long newCommunityMessage(String username, long communityId, String title, String content) {
        CommunityMessage message = new CommunityMessage();
        message.setTitle(title);
        message.setContent(content);
        return virtualSecurityContext(username, () -> communityService.newCommunityMessage(communityId, message));
    }

    private <R> R adminSecurityContext(Supplier<R> supplier) {
        return virtualSecurityContext(
                "admin",
                "admin",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")),
                supplier
        );
    }

    private <R> R virtualSecurityContext(
            String principal,
            String credentials,
            List<GrantedAuthority> authorities,
            Supplier<R> supplier
    ) {
        SecurityContext originContext = SecurityContextHolder.getContext();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        Authentication authentication = new TestingAuthenticationToken(principal, credentials, authorities);
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        R result = supplier.get();
        SecurityContextHolder.setContext(originContext);
        return result;
    }

    private <R> R virtualSecurityContext(String principal, Supplier<R> supplier) {
        return virtualSecurityContext(principal, principal, Collections.emptyList(), () -> supplier.get());
    }

    private void virtualSecurityContext(String principal, Runnable runnable) {
        virtualSecurityContext(principal, principal, Collections.emptyList(), () -> {
            runnable.run();
            return null;
        });
    }

}
