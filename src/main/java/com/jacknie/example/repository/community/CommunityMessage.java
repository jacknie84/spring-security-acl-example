package com.jacknie.example.repository.community;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
public class CommunityMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String content;

    @ManyToOne
    @JoinColumn(name = "communityId", referencedColumnName = "id", nullable = false)
    private Community community;

}
