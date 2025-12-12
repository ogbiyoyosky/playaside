package com.playvora.playvora_api.auth.controllers;

import com.playvora.playvora_api.auth.dtos.AuthRequest;
import com.playvora.playvora_api.auth.dtos.AuthResponse;
import com.playvora.playvora_api.auth.dtos.PasswordResetConfirmRequest;
import com.playvora.playvora_api.auth.dtos.PasswordResetRequest;
import com.playvora.playvora_api.app.AppUserDetail;
import com.playvora.playvora_api.auth.dtos.RefreshTokenRequest;
import com.playvora.playvora_api.auth.services.IAuthenticationService;
import com.playvora.playvora_api.auth.services.IPasswordResetService;
import com.playvora.playvora_api.common.dto.ApiResponse;
import com.playvora.playvora_api.common.exception.BadRequestException;
import com.playvora.playvora_api.component.RedirectUriValidator;

import java.io.IOException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import jakarta.servlet.http.Cookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping(path ="/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication management APIs")
@Slf4j
public class AuthenticationController {
    private final IAuthenticationService authenticationService;
    private final IPasswordResetService passwordResetService;

    private final RedirectUriValidator redirectUriValidator;

    private static final String REDIRECT_URI_COOKIE_NAME = "redirect_uri";

    @PostMapping(path = "/login")
    @Operation(summary = "User login", description = "Authenticate user and return JWT token")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody AuthRequest authRequest) {
        UserDetails userDetails = authenticationService.authenticate(
                authRequest.getEmail(),
                authRequest.getPassword()
        );

        String accessToken = authenticationService.generateTokenForLocalLogin(userDetails);
        String refreshToken = authenticationService.generateRefreshTokenForLocalLogin(userDetails);

        AuthResponse authResponse = authenticationService.buildAuthResponse(accessToken, refreshToken);

        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(authResponse, "User logged in successfully"));
    }

    @PostMapping(path = "/password-reset/request")
    @Operation(summary = "Request password reset", description = "Generate a password reset token and store it in an HTTP-only cookie")
    public ResponseEntity<ApiResponse<Void>> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest request,
            HttpServletResponse response) {
        String token = passwordResetService.initiatePasswordReset(request.getEmail());

        if (token != null) {
            addTokenCookie(response, "password_reset_token", token, 1800); // 30 minutes
        }

        return ResponseEntity.ok(ApiResponse.success(null, "If the email exists, a reset token has been issued"));
    }

    @PostMapping(path = "/password-reset/confirm")
    @Operation(summary = "Confirm password reset", description = "Reset the password using the reset token from cookie or request body")
    public ResponseEntity<ApiResponse<Void>> confirmPasswordReset(
            @Valid @RequestBody PasswordResetConfirmRequest request,
            @CookieValue(name = "password_reset_token", required = false) String tokenFromCookie,
            HttpServletResponse response) {

        String token = (request.getToken() != null && !request.getToken().isBlank())
                ? request.getToken()
                : tokenFromCookie;

        passwordResetService.resetPassword(token, request.getNewPassword());
        clearCookie(response, "password_reset_token");

        return ResponseEntity.ok(ApiResponse.success(null, "Password reset successfully"));
    }

    private void addTokenCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);  // Prevent JavaScript access
        cookie.setSecure(true);     // Only send over HTTPS
        cookie.setPath("/");        // Available to entire application
        cookie.setMaxAge(maxAge);   // Expiration in seconds
        cookie.setAttribute("SameSite", "Strict"); // CSRF protection
        response.addCookie(cookie);
    }

    private void clearCookie(HttpServletResponse response, String name) {
        Cookie cookie = new Cookie(name, null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }

    private void addRedirectUriCookie(HttpServletResponse response, String redirectUri, int maxAgeSeconds) {
        Cookie cookie = new Cookie(REDIRECT_URI_COOKIE_NAME, redirectUri);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(maxAgeSeconds);
        // Lax is required so the cookie is sent on the OAuth provider redirect back to our domain
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }

    @GetMapping("/authorize/{provider}")
    @Operation(summary = "OAuth2 Authorization Request",
            description = "Redirects to the specified OAuth2 provider login page.")
    public void redirectToOAuth2(
            @PathVariable String provider,
            @RequestParam(name = "redirect_uri", required = false) String redirectUri,
            HttpServletResponse response) throws IOException {

        String targetUrl = redirectUri;
        if (targetUrl != null && !targetUrl.isBlank()) {
            if (!redirectUriValidator.isAuthorizedRedirectUri(targetUrl)) {
                throw new BadRequestException("Unauthorized redirect URI");
            }
            // Store redirect URI in a short-lived, HTTP-only cookie for retrieval after OAuth callback
            addRedirectUriCookie(response, targetUrl, 180);
        }

        // Spring Security OAuth2 expects lowercase provider names
        String normalizedProvider = provider.toLowerCase();
        response.sendRedirect("/oauth2/authorization/" + normalizedProvider);
    }

    @PostMapping(path = "/refresh")
    @Operation(summary = "Refresh access token", description = "Exchange refresh token for a new access token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
        AuthResponse authResponse = authenticationService.exchangeRefreshToken(
                refreshTokenRequest.getRefreshToken()
        );

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(authResponse, "Token refreshed successfully")
        );
    }

    @PostMapping(path = "/logout")
    @Operation(summary = "User logout", description = "Revoke all refresh tokens for the authenticated user")
    public ResponseEntity<ApiResponse<Void>> logout() {
        // Always revoke all refresh tokens for the current authenticated user
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof AppUserDetail userDetail) {
            authenticationService.revokeAllRefreshTokensForUser(userDetail.getUsername());
        } else {
            throw new BadRequestException("User not authenticated");
        }

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(null, "All refresh tokens revoked successfully")
        );
    }

}
