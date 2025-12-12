package com.playvora.playvora_api.auth.services.impl;

import com.playvora.playvora_api.app.AppUserDetail;
import com.playvora.playvora_api.auth.dtos.AuthResponse;
import com.playvora.playvora_api.auth.entities.RefreshToken;
import com.playvora.playvora_api.auth.repo.RefreshTokenRepository;
import com.playvora.playvora_api.auth.services.IAuthenticationService;
import com.playvora.playvora_api.common.exception.BadRequestException;
import com.playvora.playvora_api.user.entities.User;
import com.playvora.playvora_api.user.repo.UserRepository;
import com.playvora.playvora_api.user.services.IJwtService;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.playvora.playvora_api.user.enums.AuthProvider;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;


@Service
@RequiredArgsConstructor
public class AuthenticationService implements IAuthenticationService {
    private final UserDetailsService userDetailsService;
    private final AuthenticationManager authenticationManager;
    private final IJwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;


   
    @Override
    public UserDetails authenticate(String email, String password) {
         authenticationManager.authenticate(
                 new UsernamePasswordAuthenticationToken(email, password)
         );
        return userDetailsService.loadUserByUsername(email);
    }

    @Override
    public String generateTokenForLocalLogin(UserDetails userDetails) {
        return jwtService.generateTokenForLocalLogin(userDetails);
    }

    @Override
    @Transactional
    public String generateRefreshTokenForLocalLogin(UserDetails userDetails) {
        String token = jwtService.generateLongLivedRefreshTokenForLocalLogin(userDetails);
        
        // Get User entity from UserDetails
        User user = null;
        if (userDetails instanceof AppUserDetail) {
            user = ((AppUserDetail) userDetails).getUser();
        } else {
            // Fallback: fetch user by email
            user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));
        }
        
        // Extract expiration date from token
        Date expirationDate = jwtService.extractExpiration(token);
        OffsetDateTime expiresAt = OffsetDateTime.ofInstant(
                expirationDate.toInstant(), 
                ZoneId.systemDefault()
        );
        
        // Revoke all existing refresh tokens for this user (only one token per user allowed)
        List<RefreshToken> activeTokens = refreshTokenRepository.findByUserAndRevokedFalse(user);
        activeTokens.forEach(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
        });
        
        // Save new refresh token
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(token)
                .expiresAt(expiresAt)
                .revoked(false)
                .build();
        
        refreshTokenRepository.save(refreshToken);
        
        return token;
    }

    @Override
    @Transactional
    public String generateRefreshTokenForOAuth2Login(User user) {
        String token = jwtService.generateLongLivedRefreshTokenForOAuth2Login(user);
        
        // Extract expiration date from token
        Date expirationDate = jwtService.extractExpiration(token);
        OffsetDateTime expiresAt = OffsetDateTime.ofInstant(
                expirationDate.toInstant(), 
                ZoneId.systemDefault()
        );
        
        // Revoke all existing refresh tokens for this user (only one token per user allowed)
        List<RefreshToken> activeTokens = refreshTokenRepository.findByUserAndRevokedFalse(user);
        activeTokens.forEach(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
        });
        
        // Save new refresh token
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(token)
                .expiresAt(expiresAt)
                .revoked(false)
                .build();
        
        refreshTokenRepository.save(refreshToken);
        
        return token;
    }

    @Override
    @Transactional
    public AuthResponse exchangeRefreshToken(String refreshTokenString) {
        // Find refresh token in database
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenString)
                .orElseThrow(() -> new BadRequestException("Invalid refresh token"));
        
        // Validate refresh token
        if (!refreshToken.isValid()) {
            throw new BadRequestException("Refresh token is expired or revoked");
        }
        
        // Validate JWT token itself
        if (jwtService.isTokenExpired(refreshTokenString)) {
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
            throw new BadRequestException("Refresh token is expired");
        }
        
        // Get user from refresh token
        User user = refreshToken.getUser();
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        
        // Generate new access token
        String newAccessToken = jwtService.generateTokenForLocalLogin(userDetails);
        
        // Optionally revoke old refresh token and generate a new one (refresh token rotation)
        // For now, we'll keep the same refresh token
        // If you want refresh token rotation, uncomment the following:
        /*
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
        String newRefreshToken = generateRefreshTokenForLocalLogin(userDetails);
        */
        
        return buildAuthResponse(newAccessToken, refreshTokenString);
    }

    @Override
    public AuthResponse buildAuthResponse(String accessToken, String refreshToken) {
        Long expiresInSeconds = jwtService.getAccessTokenExpirationSeconds();
        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .expires_in(expiresInSeconds)
                .build();
    }

    @Override
    @Transactional
    public void revokeRefreshToken(String refreshTokenString) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenString)
                .orElseThrow(() -> new BadRequestException("Invalid refresh token"));
        
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
    }

    @Override
    @Transactional
    public void revokeAllRefreshTokensForUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found"));
        
        List<RefreshToken> activeTokens = refreshTokenRepository.findByUserAndRevokedFalse(user);
        activeTokens.forEach(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
        });
    }

    
    @Override
    @Transactional
    public void AuthenticateWithOAuth2(
            ServletRequest request,
            ServletResponse response,
            AuthProvider authProvider,
            OAuth2User oauth2User,
            OAuth2AuthenticationToken oauth2AuthenticationToken
    ) {
        // This method is kept for backward compatibility but logic has moved to OAuth2AuthenticationSuccessHandler
        throw new UnsupportedOperationException("OAuth2 authentication is handled by OAuth2AuthenticationSuccessHandler");
    }
}
