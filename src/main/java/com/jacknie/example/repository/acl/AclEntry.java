package com.jacknie.example.repository.acl;

import lombok.*;
import org.hibernate.Hibernate;

import javax.persistence.*;
import java.util.Objects;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        AclEntry aclEntry = (AclEntry) o;
        return Objects.equals(id, aclEntry.id);
    }

    @Override
    public int hashCode() {
        return 0;
    }
}
