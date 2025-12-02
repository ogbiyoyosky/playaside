package com.playvora.playvora_api.user.services;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.playvora.playvora_api.user.entities.User;

import java.util.Date;

@Service
public interface IJwtService {
    String generateTokenForLocalLogin(UserDetails userDetails);
    String generateTokenForOAuth2Login(User user);
    String generateLongLivedRefreshTokenForLocalLogin(UserDetails userDetails);
    String generateShortLivedRefreshTokenForLocalLogin(UserDetails userDetails);
    String generateLongLivedRefreshTokenForOAuth2Login(User user);
    String generateShortLivedRefreshTokenForOAuth2Login(User user);
    String generateRefreshTokenForOAuth2Login(User user);
    Boolean isTokenValid(String token, UserDetails userDetails);
    Boolean isTokenExpired(String token);
    String extractUsername(String token);
    Date extractExpiration(String token);
    Long getAccessTokenExpirationSeconds();
}
