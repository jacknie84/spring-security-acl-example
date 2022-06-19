package com.jacknie.example.service;

import com.jacknie.example.repository.community.Community;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.Acl;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class CommunityServiceCommunityTest extends CommunityServiceTest {

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    public void createCommunityTest() throws Exception {
        final String username = "testCommChief";
        final long communityId = communityService.createCommunity("Test Subject1", username);
        assertTrue(aclSidRepository.existsBySid(username));
        assertTrue(aclClassRepository.existsByClassName(Community.class.getName()));
        assertTrue(aclObjectIdentityRepository.existsByObjectIdIdentity(Long.toString(communityId)));
        ObjectIdentity object = new ObjectIdentityImpl(Community.class.getName(), communityId);
        Acl acl = mutableAclService.readAclById(object);
        assertTrue(acl.isGranted(allPermissions, List.of(new PrincipalSid(username)), false));
    }

    @Test
    @WithMockUser
    public void createCommunityAccessDeniedExceptionTest() {
        assertThrows(AccessDeniedException.class, () -> communityService.createCommunity("Test Subject2", "test"));
    }

    @Test
    @WithMockUser
    public void getCommunityTest() {
        final long communityId = createCommunityByAdmin("Test Subject3", "owner");
        communityService.getCommunity(communityId);
    }

    @Test
    @WithMockUser(roles = "NOT_USER")
    public void getCommunityAccessDeniedExceptionTest() {
        final long communityId = createCommunityByAdmin("Test Subject4", "owner");
        assertThrows(AccessDeniedException.class, () -> communityService.getCommunity(communityId));
    }

    @Test
    @WithMockUser(username = "testCommChief")
    public void inviteCommunityTest() {
        final String owner = "testCommChief";
        final String user = "invitedUser";
        final long communityId = createCommunityByAdmin("Test Subject5", owner);
        communityService.inviteCommunity(communityId, user);
        Acl acl = mutableAclService.readAclById(new ObjectIdentityImpl(Community.class.getName(), communityId));
        List<Permission> permissions = List.of(BasePermission.READ, BasePermission.CREATE);
        List<Sid> sids = List.of(new PrincipalSid(user));
        assertTrue(acl.isGranted(permissions, sids, false));
    }

    @Test
    @WithMockUser
    public void inviteCommunityAccessDeniedExceptionTest() {
        final long communityId = createCommunityByAdmin("Test Subject6", "otherUser");
        assertThrows(AccessDeniedException.class, () -> communityService.inviteCommunity(communityId, "invitedUser"));
    }

}
