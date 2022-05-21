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
@Table(uniqueConstraints = @UniqueConstraint(columnNames = { "objectIdClass", "objectIdIdentity" }))
public class AclObjectIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String objectIdIdentity;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        AclObjectIdentity that = (AclObjectIdentity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return 0;
    }
}
