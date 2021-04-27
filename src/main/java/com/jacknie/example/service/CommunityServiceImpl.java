package com.jacknie.example.service;

import com.jacknie.example.repository.community.Community;
import com.jacknie.example.repository.community.CommunityMessage;
import com.jacknie.example.repository.community.CommunityMessageRepository;
import com.jacknie.example.repository.community.CommunityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.acls.domain.*;
import org.springframework.security.acls.model.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
public class CommunityServiceImpl implements CommunityService {

    private static final String COMMUNITY = "com.jacknie.example.repository.community.Community";
    private static final String COMMUNITY_MESSAGE = "com.jacknie.example.repository.community.CommunityMessage";

    private final CommunityRepository communityRepository;
    private final CommunityMessageRepository messageRepository;
    private final MutableAclService aclService;

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public long createCommunity(String subject, String owner) {
        Community community = new Community();
        community.setSubject(subject);
        Long communityId = communityRepository.save(community).getId();
        MutableAcl acl = getMutableAcl(new ObjectIdentityImpl(community));
        CumulativePermission permission = new CumulativePermission();
        permission.set(BasePermission.ADMINISTRATION);
        permission.set(BasePermission.READ);
        permission.set(BasePermission.WRITE);
        permission.set(BasePermission.CREATE);
        permission.set(BasePermission.DELETE);
        acl.insertAce(acl.getEntries().size(), permission, new PrincipalSid(owner), true);
        acl.insertAce(acl.getEntries().size(), BasePermission.READ, new GrantedAuthoritySid("ROLE_USER"), true);
        aclService.updateAcl(acl);
        return communityId;
    }

    @Override
    @PreAuthorize("hasPermission(#communityId, '" + COMMUNITY + "', 'READ')")
    public Community getCommunity(long communityId) {
        return communityRepository.findById(communityId).orElseThrow();
    }

    @Override
    @PreAuthorize("hasPermission(#communityId, '" + COMMUNITY + "', 'ADMINISTRATION')")
    public void inviteCommunity(long communityId, String username) {
        MutableAcl acl = getMutableAcl(new ObjectIdentityImpl(Community.class.getName(), communityId));
        Sid sid = new PrincipalSid(username);
        CumulativePermission permission = new CumulativePermission();
        permission.set(BasePermission.READ);
        permission.set(BasePermission.CREATE);
        acl.insertAce(acl.getEntries().size(), permission, sid, true);
        for (CommunityMessage message : messageRepository.findAllByCommunityId(communityId)) {
            ObjectIdentity object = new ObjectIdentityImpl(CommunityMessage.class.getName(), message.getId());
            MutableAcl messageAcl = getMutableAcl(object);
            messageAcl.insertAce(messageAcl.getEntries().size(), BasePermission.READ, sid, true);
            aclService.updateAcl(messageAcl);
        }
        aclService.updateAcl(acl);
    }

    @Override
    @PreAuthorize("hasPermission(#communityId, '" + COMMUNITY + "', 'CREATE')")
    public long newCommunityMessage(long communityId, CommunityMessage message) {
        return communityRepository.findById(communityId)
                .map(community -> newCommunityMessage(community, message))
                .orElseThrow();
    }

    @Override
    @PreAuthorize("hasPermission(#messageId, '" + COMMUNITY_MESSAGE + "', 'READ')")
    public CommunityMessage getCommunityMessage(long messageId) {
        return messageRepository.findById(messageId).orElseThrow();
    }

    @Override
    @PreAuthorize("hasPermission(#messageId, '" + COMMUNITY_MESSAGE + "', 'WRITE')")
    public void updateCommunityMessage(long messageId, CommunityMessage message) {
        messageRepository.findById(messageId).ifPresent(origin -> updateCommunityMessage(origin, message));
    }

    @Override
    @PreAuthorize("hasPermission(#messageId, '" + COMMUNITY_MESSAGE + "', 'DELETE')")
    public void deleteCommunityMessage(long messageId) {
        messageRepository.deleteById(messageId);
        ObjectIdentity object = new ObjectIdentityImpl(COMMUNITY_MESSAGE, messageId);
        aclService.deleteAcl(object, true);
    }

    private MutableAcl getMutableAcl(ObjectIdentity object) {
        try {
            return (MutableAcl) aclService.readAclById(object);
        } catch (NotFoundException e) {
            return aclService.createAcl(object);
        }
    }

    private long newCommunityMessage(Community community, CommunityMessage message) {
        message.setCommunity(community);
        Long messageId = messageRepository.save(message).getId();
        MutableAcl communityAcl = getMutableAcl(new ObjectIdentityImpl(Community.class.getName(), community.getId()));
        MutableAcl messageAcl = getMutableAcl(new ObjectIdentityImpl(CommunityMessage.class.getName(), messageId));
        Sid sid = new PrincipalSid(SecurityContextHolder.getContext().getAuthentication());
        communityAcl.getEntries().stream()
                .filter(AccessControlEntry::isGranting)
                .map(AccessControlEntry::getSid)
                .filter(aceSid -> aceSid instanceof PrincipalSid)
                .distinct()
                .map(aceSid -> resolveSidPermissionEntry(aceSid, sid))
                .forEach(entry -> messageAcl.insertAce(messageAcl.getEntries().size(), entry.getValue(), entry.getKey(), true));
        aclService.updateAcl(messageAcl);
        return messageId;
    }

    private Map.Entry<Sid, Permission> resolveSidPermissionEntry(Sid aceSid, Sid ownerSid) {
        Permission permission = BasePermission.READ;
        if (aceSid.equals(ownerSid)) {
            CumulativePermission cumulativePermission = new CumulativePermission();
            cumulativePermission.set(BasePermission.READ);
            cumulativePermission.set(BasePermission.WRITE);
            cumulativePermission.set(BasePermission.DELETE);
            permission = cumulativePermission;
        }
        return Map.entry(aceSid, permission);
    }

    private void updateCommunityMessage(CommunityMessage origin, CommunityMessage target) {
        origin.setTitle(target.getTitle());
        origin.setContent(target.getContent());
        messageRepository.save(origin);
    }

}
