package com.playvora.playvora_api.user.services.impl;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import io.jsonwebtoken.Claims;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import com.playvora.playvora_api.user.entities.User;
import com.playvora.playvora_api.user.services.IJwtService;

@Service
public class JwtService implements IJwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    // Access tokens should be short-lived (default 1 day = 86400000ms)
    private final Long JwtExpirationMs = 86400000L; // 1 day

    
    private final Long LongLivedRefreshTokenExpirationMs = 604800000L; // 7 days
    private final Long ShortLivedRefreshTokenExpirationMs = 86400000L; // 1 day


    @Override
    public String generateTokenForLocalLogin(UserDetails userDetails) {

        Map<String,Object> claims = new HashMap<>();
        claims.put("sub", userDetails.getUsername());
        claims.put("iat", System.currentTimeMillis());
        claims.put("exp", System.currentTimeMillis() + JwtExpirationMs);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + JwtExpirationMs))
                .signWith(getSecretKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    @Override
    public String generateRefreshTokenForOAuth2Login(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", user.getEmail());
        claims.put("iat", System.currentTimeMillis());
        claims.put("exp", System.currentTimeMillis() + LongLivedRefreshTokenExpirationMs);

        return Jwts.builder()
            .setClaims(claims)
            .setSubject(user.getEmail())
            .setIssuedAt(new Date(System.currentTimeMillis()))
            .setExpiration(new Date(System.currentTimeMillis() + LongLivedRefreshTokenExpirationMs))
            .signWith(getSecretKey(), SignatureAlgorithm.HS256)
            .compact();
    }

    private Key getSecretKey() {
        byte[] keyBytes = secretKey.getBytes();
        return Keys.hmacShaKeyFor(keyBytes );
    }

    @Override
    public String generateTokenForOAuth2Login(User user) {
        Map<String,Object> claims = new HashMap<>();
        claims.put("sub", user.getEmail());
        claims.put("iat", System.currentTimeMillis());
        claims.put("exp", System.currentTimeMillis() + JwtExpirationMs);
        
        return Jwts.builder()
            .setClaims(claims)
            .setSubject(user.getEmail())
            .setIssuedAt(new Date(System.currentTimeMillis()))
            .setExpiration(new Date(System.currentTimeMillis() + JwtExpirationMs))
            .signWith(getSecretKey(), SignatureAlgorithm.HS256)
            .compact();
    }

    @Override
    public String extractUsername(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSecretKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    @Override
    public Boolean isTokenValid(String token, UserDetails userDetails) {
        return extractUsername(token).equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    @Override
    public Boolean isTokenExpired(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSecretKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration()
                .before(new Date());
    }

    @Override
    public  String generateLongLivedRefreshTokenForLocalLogin(UserDetails userDetails) {
        Map<String,Object> claims = new HashMap<>();
        claims.put("sub", userDetails.getUsername());
        claims.put("iat", System.currentTimeMillis());
        claims.put("exp", System.currentTimeMillis() + LongLivedRefreshTokenExpirationMs);
        
        return Jwts.builder()
            .setClaims(claims)
            .setSubject(userDetails.getUsername())
            .setIssuedAt(new Date(System.currentTimeMillis()))
            .setExpiration(new Date(System.currentTimeMillis() + LongLivedRefreshTokenExpirationMs))
            .signWith(getSecretKey(), SignatureAlgorithm.HS256)
            .compact();
    }
    @Override
    public String generateShortLivedRefreshTokenForLocalLogin(UserDetails userDetails) {
        Map<String,Object> claims = new HashMap<>();
        claims.put("sub", userDetails.getUsername());
        claims.put("iat", System.currentTimeMillis());
        claims.put("exp", System.currentTimeMillis() + ShortLivedRefreshTokenExpirationMs);
        
        return Jwts.builder()
            .setClaims(claims)
            .setSubject(userDetails.getUsername())
            .setIssuedAt(new Date(System.currentTimeMillis()))
            .setExpiration(new Date(System.currentTimeMillis() + ShortLivedRefreshTokenExpirationMs))
            .signWith(getSecretKey(), SignatureAlgorithm.HS256)
            .compact();
    }

    @Override
    public String generateLongLivedRefreshTokenForOAuth2Login(User user) {
        Map<String,Object> claims = new HashMap<>();
        claims.put("sub", user.getEmail());
        claims.put("iat", System.currentTimeMillis());
        claims.put("exp", System.currentTimeMillis() + LongLivedRefreshTokenExpirationMs);
        
        return Jwts.builder()
            .setClaims(claims)  
            .setSubject(user.getEmail())
            .setIssuedAt(new Date(System.currentTimeMillis()))
            .setExpiration(new Date(System.currentTimeMillis() + LongLivedRefreshTokenExpirationMs))
            .signWith(getSecretKey(), SignatureAlgorithm.HS256)
            .compact();
    }
    @Override
    public String generateShortLivedRefreshTokenForOAuth2Login(User user) {
        Map<String,Object> claims = new HashMap<>();
        claims.put("sub", user.getEmail()); 
        claims.put("iat", System.currentTimeMillis());
        claims.put("exp", System.currentTimeMillis() + ShortLivedRefreshTokenExpirationMs);
        
        return Jwts.builder()
            .setClaims(claims)
            .setSubject(user.getEmail())
            .setIssuedAt(new Date(System.currentTimeMillis()))
            .setExpiration(new Date(System.currentTimeMillis() + ShortLivedRefreshTokenExpirationMs))
            .signWith(getSecretKey(), SignatureAlgorithm.HS256)
            .compact();
    }

    @Override
    public Date extractExpiration(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSecretKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getExpiration();
    }

    @Override
    public Long getAccessTokenExpirationSeconds() {
        return JwtExpirationMs / 1000; // Convert milliseconds to seconds
    }
}

