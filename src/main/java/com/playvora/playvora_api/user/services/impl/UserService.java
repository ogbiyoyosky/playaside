package com.playvora.playvora_api.user.services.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.playvora.playvora_api.app.AppUserDetail;
import com.playvora.playvora_api.auth.dtos.AuthProviderAttribute;
import com.playvora.playvora_api.common.exception.BadRequestException;
import com.playvora.playvora_api.user.dtos.RegisterRequest;
import com.playvora.playvora_api.user.dtos.UpdateRequest;
import com.playvora.playvora_api.user.entities.Role;
import com.playvora.playvora_api.user.entities.User;
import com.playvora.playvora_api.user.entities.UserRole;
import com.playvora.playvora_api.user.repo.RoleRepository;
import com.playvora.playvora_api.user.repo.UserRepository;
import com.playvora.playvora_api.user.repo.UserRoleRepository;
import com.playvora.playvora_api.user.services.IUserService;
import com.playvora.playvora_api.wallet.services.IWalletService;

import lombok.RequiredArgsConstructor;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements IUserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final IWalletService walletService;

    @Override
    public User createUser(RegisterRequest registerRequest) {
        if (existsByEmail(registerRequest.getEmail())) {
            throw new BadRequestException("Email already exists");
        }
        
        // Load the default ROLE_USER from the database
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Default role ROLE_USER not found"));
        
        User user = User.builder()
                .email(registerRequest.getEmail())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .firstName(registerRequest.getFirstName())
                .lastName(registerRequest.getLastName())
                .nickname(registerRequest.getNickname())
                .build();

        user.setProviderId("local:email:" + registerRequest.getEmail());
        
        User savedUser = userRepository.save(user);
        
        // Create UserRole entry for the default role (global, not tied to a community)
        UserRole defaultUserRole = UserRole.builder()
                .user(savedUser)
                .role(userRole)
                .community(null)  // Global role
                .build();
        userRoleRepository.save(defaultUserRole);
        
        return savedUser;
    }

    @Override
    public User createUserFromOAuth2(AuthProviderAttribute authProviderAttribute) {

        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Default role ROLE_USER not found"));
        
        User user = User.builder()
                .email(authProviderAttribute.getEmail())
                .password(passwordEncoder.encode("oauth2-user-no-password"))
                .firstName(authProviderAttribute.getFirstName())
                .lastName(authProviderAttribute.getLastName())
                .nickname(authProviderAttribute.getNickname())
                .profilePictureUrl(authProviderAttribute.getProfilePictureUrl())
                .providerId(authProviderAttribute.getProviderId())
                .provider(authProviderAttribute.getProvider())
                .build();
        
        User savedUser = userRepository.save(user);
        
        // Create UserRole entry for the default role (global, not tied to a community)
        UserRole defaultUserRole = UserRole.builder()
                .user(savedUser)
                .role(userRole)
                .community(null)  // Global role
                .build();
        userRoleRepository.save(defaultUserRole);
        
        return savedUser;
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElse(null);
    }

    @Override
    public User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        
        if (principal instanceof AppUserDetail userDetail) {
            return userRepository.findByEmailWithRoles(userDetail.getUsername())
                    .orElseThrow(() -> new BadRequestException("User not found"));
        }
        
        throw new BadRequestException("Invalid authentication principal");
    }

    @Override
    @Transactional
    public User updateCurrentUser(UpdateRequest updateRequest) {
        User currentUser = getCurrentUser();

        String originalCountry = currentUser.getCountry();
        boolean shouldCreateWallet = false;

        // Safely handle Optional fields that might be null (when field is missing from JSON)
        if (updateRequest.getFirstName() != null && updateRequest.getFirstName().isPresent()) {
            currentUser.setFirstName(updateRequest.getFirstName().get());
        }
        if (updateRequest.getLastName() != null && updateRequest.getLastName().isPresent()) {
            currentUser.setLastName(updateRequest.getLastName().get());
        }
        if (updateRequest.getNickname() != null && updateRequest.getNickname().isPresent()) {
            currentUser.setNickname(updateRequest.getNickname().get());
        }
        if (updateRequest.getProfilePictureUrl() != null && updateRequest.getProfilePictureUrl().isPresent()) {
            currentUser.setProfilePictureUrl(updateRequest.getProfilePictureUrl().get());
        }
        if (updateRequest.getCountry() != null && updateRequest.getCountry().isPresent()) {
            String requestedCountry = updateRequest.getCountry().get();

            // If country is already set and a different value is requested, block the change
            if (originalCountry != null && !originalCountry.equals(requestedCountry)) {
                throw new BadRequestException("Country cannot be changed once it has been set");
            }

            // If this is the first time country is being set, allow it and mark for wallet creation
            if (originalCountry == null) {
                currentUser.setCountry(requestedCountry);
                shouldCreateWallet = true;
            }
        }

        User savedUser = userRepository.save(currentUser);

        if (shouldCreateWallet) {
            walletService.createWalletForUser(savedUser);
        }

        return savedUser;
    }
}
