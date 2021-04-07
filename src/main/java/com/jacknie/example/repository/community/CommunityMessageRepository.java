package com.jacknie.example.repository.community;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommunityMessageRepository extends JpaRepository<CommunityMessage, Long> {

    List<CommunityMessage> findAllByCommunityId(long communityId);

}
