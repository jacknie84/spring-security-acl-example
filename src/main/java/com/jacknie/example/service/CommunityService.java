package com.jacknie.example.service;

import com.jacknie.example.repository.community.Community;
import com.jacknie.example.repository.community.CommunityMessage;

public interface CommunityService {

    long createCommunity(String subject, String owner);

    Community getCommunity(long communityId);

    void inviteCommunity(long communityId, String username);

    long newCommunityMessage(long communityId, CommunityMessage message);

    CommunityMessage getCommunityMessage(long messageId);

    void updateCommunityMessage(long messageId, CommunityMessage message);

    void deleteCommunityMessage(long messageId);
}
