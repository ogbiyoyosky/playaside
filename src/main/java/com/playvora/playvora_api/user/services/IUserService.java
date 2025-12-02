package com.playvora.playvora_api.user.services;

import org.springframework.stereotype.Service;

import com.playvora.playvora_api.auth.dtos.AuthProviderAttribute;
import com.playvora.playvora_api.user.dtos.RegisterRequest;
import com.playvora.playvora_api.user.dtos.UpdateRequest;
import com.playvora.playvora_api.user.entities.User;

@Service
public interface IUserService {
    User createUser(RegisterRequest registerRequest);
    User createUserFromOAuth2(AuthProviderAttribute authProviderAttribute);
    boolean existsByEmail(String email);
    User findByEmail(String email);
    User getCurrentUser();
    User updateCurrentUser(UpdateRequest updateRequest);
}
