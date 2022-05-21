package com.jacknie.example.repository.acl;

import com.jacknie.example.custom.SidType;
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
@Table(uniqueConstraints = @UniqueConstraint(columnNames = { "sid", "type" }))
public class AclSid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SidType type;

    @Column(nullable = false)
    private String sid;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        AclSid aclSid = (AclSid) o;
        return Objects.equals(id, aclSid.id);
    }

    @Override
    public int hashCode() {
        return 0;
    }
}
