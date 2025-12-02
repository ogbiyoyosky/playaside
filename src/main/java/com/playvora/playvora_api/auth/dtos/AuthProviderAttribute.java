package com.playvora.playvora_api.auth.dtos;

import com.playvora.playvora_api.user.enums.AuthProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuthProviderAttribute {
    private String email;
    private String firstName;
    private String lastName;
    private String nickname;
    private String country;
    private String profilePictureUrl;
    private String providerId;
    private AuthProvider provider;
}
