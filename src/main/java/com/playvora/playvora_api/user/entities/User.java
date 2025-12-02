package com.playvora.playvora_api.user.entities;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.playvora.playvora_api.match.entities.MatchRegistration;
import com.playvora.playvora_api.user.enums.AuthProvider;
import com.playvora.playvora_api.wallet.entities.Wallet;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Table(name = "users")
@EqualsAndHashCode(exclude = {"userRoles", "matchRegistrations", "wallet"})
@ToString(exclude = {"userRoles", "matchRegistrations", "wallet"})
public class User {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;
        
    @Column(name = "nickname", nullable = true)
    private String nickname;

    @Column(name = "profile_picture_url")
    private String profilePictureUrl;

    @Column(name = "country")
    private String country;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AuthProvider provider = AuthProvider.LOCAL;

    @Column(name = "provider_id")
    private String providerId;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    @SQLRestriction("deleted_at IS NULL")
    @Builder.Default
    @JsonManagedReference
    private Set<UserRole> userRoles = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    @Builder.Default
    private Set<MatchRegistration> matchRegistrations = new HashSet<>();

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Wallet wallet;
}
