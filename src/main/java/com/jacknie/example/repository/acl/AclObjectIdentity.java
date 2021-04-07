package com.jacknie.example.repository.acl;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = { "objectIdClass", "objectIdIdentity" }))
public class AclObjectIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long objectIdIdentity;

    @Column(nullable = false)
    private Boolean entriesInheriting;

    @ManyToOne
    @JoinColumn(name = "parentObject", referencedColumnName = "id")
    private AclObjectIdentity parentObject;

    @ManyToOne
    @JoinColumn(name = "objectIdClass", referencedColumnName = "id")
    private AclClass objectIdClass;

    @ManyToOne
    @JoinColumn(name = "ownerSid", referencedColumnName = "id")
    private AclSid ownerSid;

}
