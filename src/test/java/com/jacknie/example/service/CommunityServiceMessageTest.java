package com.jacknie.example.service;

import com.jacknie.example.repository.community.CommunityMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
public class CommunityServiceMessageTest extends CommunityServiceTest {

    private static final String OWNER = "owner";
    private static final String USER1 = "user1";
    private static final String USER2 = "user2";

    private long communityId;

    @BeforeEach
    public void beforeEach() {
        this.communityId = createCommunityByAdmin(UUID.randomUUID().toString(), OWNER);
        inviteCommunity(OWNER, communityId, USER1);
        inviteCommunity(OWNER, communityId, USER2);
    }

    @Test
    @WithMockUser
    public void newMessageAccessDeniedExceptionTest() {
        CommunityMessage message = new CommunityMessage();
        message.setTitle("failed");
        message.setContent("failed");
        assertThrows(
                AccessDeniedException.class,
                () -> communityService.newCommunityMessage(communityId, message)
        );
    }

    @Test
    @WithMockUser(username = OWNER)
    public void newMessageTest() {
        CommunityMessage message = new CommunityMessage();
        message.setTitle("success");
        message.setContent("success");
        communityService.newCommunityMessage(communityId, message);
    }

    @Test
    @WithMockUser(username = USER2)
    public void getMessageTest() {
        long messageId = newCommunityMessage(USER1, communityId, "test message", "message content");
        communityService.getCommunityMessage(messageId);
    }

    @Test
    @WithMockUser(username = USER1)
    public void updateMessageAccessDeniedExceptionTest() {
        long messageId = newCommunityMessage(USER2, communityId, "test message", "message content");
        CommunityMessage message = new CommunityMessage();
        message.setTitle("updated test message");
        message.setContent("updated test content");
        assertThrows(
                AccessDeniedException.class,
                () -> communityService.updateCommunityMessage(messageId, message)
        );
    }

    @Test
    @WithMockUser(username = USER1)
    public void updateMessageTest() {
        long messageId = newCommunityMessage(USER1, communityId, "test message", "message content");
        CommunityMessage message = new CommunityMessage();
        message.setTitle("updated test message");
        message.setContent("updated test content");
        communityService.updateCommunityMessage(messageId, message);
    }

    @Test
    @WithMockUser(username = USER1)
    public void deleteMessageAccessDeniedExceptionTest() {
        long messageId = newCommunityMessage(USER2, communityId, "test message", "message content");
        assertThrows(
                AccessDeniedException.class,
                () -> communityService.deleteCommunityMessage(messageId)
        );
    }

    @Test
    @WithMockUser(username = USER1)
    public void deleteMessageTest() {
        long messageId = newCommunityMessage(USER1, communityId, "test message", "message content");
        communityService.deleteCommunityMessage(messageId);
    }

}
