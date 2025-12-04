package com.playvora.playvora_api.user.services;

import com.playvora.playvora_api.auth.dtos.AuthProviderAttribute;
import com.playvora.playvora_api.community.entities.CommunityMember;
import com.playvora.playvora_api.user.dtos.RegisterRequest;
import com.playvora.playvora_api.user.dtos.UpdateRequest;
import com.playvora.playvora_api.user.entities.User;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public interface IUserService {
    User createUser(RegisterRequest registerRequest);
    User createUserFromOAuth2(AuthProviderAttribute authProviderAttribute);
    boolean existsByEmail(String email);
    User findByEmail(String email);
    User getCurrentUser();
    User updateCurrentUser(UpdateRequest updateRequest);

    /**
     * Return all active community memberships for the given user.
     */
    List<CommunityMember> getUserCommunities(UUID userId);
}
