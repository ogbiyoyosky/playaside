package com.playvora.playvora_api.user.entities;

import java.util.Set;
import java.util.HashSet;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Builder
@Table(name = "roles")
@EqualsAndHashCode(exclude = "userRoles")
@ToString(exclude = "userRoles")
public class Role {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(unique = true)
    private String name;

    @Column(nullable = true)
    private String description;

    @OneToMany(mappedBy = "role")
    @Builder.Default
    @JsonIgnore
    private Set<UserRole> userRoles = new HashSet<>();
}
