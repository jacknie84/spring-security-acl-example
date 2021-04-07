package com.jacknie.example.repository.community;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Data
@Entity
public class Community {

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, unique = true)
    private String subject;

}
