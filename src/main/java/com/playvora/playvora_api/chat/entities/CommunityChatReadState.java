package com.playvora.playvora_api.chat.entities;

import com.playvora.playvora_api.community.entities.Community;
import com.playvora.playvora_api.user.entities.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Table(name = "community_chat_read_states",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "community_id"}))
public class CommunityChatReadState {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Column(name = "last_read_at")
    private OffsetDateTime lastReadAt;
}


