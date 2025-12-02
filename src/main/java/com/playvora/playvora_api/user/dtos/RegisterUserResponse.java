package com.playvora.playvora_api.user.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class 
RegisterUserResponse {
    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private String nickname;
    private boolean enabled;
    private String country;
    private String profilePictureUrl;
}