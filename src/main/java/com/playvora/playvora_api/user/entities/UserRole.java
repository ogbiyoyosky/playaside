package com.playvora.playvora_api.user.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.playvora.playvora_api.community.entities.Community;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Builder
@Table(name = "user_roles")
@EqualsAndHashCode(exclude = {"user", "role", "community"})
@ToString(exclude = {"user", "community"})
public class UserRole {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = true)
    private Community community;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = OffsetDateTime.now();
        }
    }

    /**
     * Check if this role assignment is active (not soft deleted)
     */
    public boolean isActive() {
        return this.deletedAt == null;
    }

    /**
     * Soft delete this role assignment
     */
    public void softDelete() {
        this.deletedAt = OffsetDateTime.now();
    }
}
