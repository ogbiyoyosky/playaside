package com.playvora.playvora_api.match.dtos;

import com.playvora.playvora_api.user.dtos.UserResponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TeamResponse {
    private UUID id;
    private String name;
    private UserResponse captain;
    private String color;
    private LocalDateTime createdAt;
    private List<TeamPlayerResponse> players;
}
