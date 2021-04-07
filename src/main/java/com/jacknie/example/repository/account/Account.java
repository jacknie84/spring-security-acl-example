package com.jacknie.example.repository.account;

import lombok.Data;

import javax.persistence.*;
import java.util.Collections;
import java.util.Set;

@Data
@Entity
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @ElementCollection
    @CollectionTable(name = "AccountRole", joinColumns = @JoinColumn(name = "accountId", referencedColumnName = "id"))
    @Enumerated(EnumType.STRING)
    private Set<AccountRole> roles = Collections.emptySet();

}
