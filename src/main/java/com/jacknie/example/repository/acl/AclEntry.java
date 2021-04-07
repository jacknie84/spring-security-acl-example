package com.jacknie.example.repository.acl;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = { "aclObjectIdentity", "aceOrder" }))
public class AclEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer aceOrder;

    @Column(nullable = false)
    private Integer mask;

    @Column(nullable = false)
    private Boolean granting;

    @Column(nullable = false)
    private Boolean auditSuccess;

    @Column(nullable = false)
    private Boolean auditFailure;

    @ManyToOne
    @JoinColumn(name = "aclObjectIdentity", referencedColumnName = "id", nullable = false)
    private AclObjectIdentity objectIdentity;

    @ManyToOne
    @JoinColumn(name = "sid", referencedColumnName = "id", nullable = false)
    private AclSid sid;

}
