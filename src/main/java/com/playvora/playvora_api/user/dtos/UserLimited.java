package com.playvora.playvora_api.user.dtos;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder    
public class UserLimited {
    private UUID id;
    private String firstName;
    private String nickname;
    private String lastName;
    private String profilePictureUrl;
    
}
