package com.playvora.playvora_api.auth.services;

import com.playvora.playvora_api.auth.dtos.AuthResponse;
import com.playvora.playvora_api.user.entities.User;
import com.playvora.playvora_api.user.enums.AuthProvider;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

@Component
public interface IAuthenticationService {
    UserDetails authenticate(String username, String password);
    String generateTokenForLocalLogin(UserDetails userDetails);
    String generateRefreshTokenForLocalLogin(UserDetails userDetails);
    String generateRefreshTokenForOAuth2Login(User user);
    AuthResponse exchangeRefreshToken(String refreshToken);
    AuthResponse buildAuthResponse(String accessToken, String refreshToken);
    void revokeRefreshToken(String refreshToken);
    void revokeAllRefreshTokensForUser(String email);
    void AuthenticateWithOAuth2(ServletRequest request, ServletResponse response, AuthProvider authProvider, OAuth2User oauth2User, OAuth2AuthenticationToken oauth2AuthenticationToken);
}
