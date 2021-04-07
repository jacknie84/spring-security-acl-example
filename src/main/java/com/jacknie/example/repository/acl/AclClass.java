package com.jacknie.example.repository.acl;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
public class AclClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "class", nullable = false, unique = true)
    private String className;

}
