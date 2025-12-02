package com.playvora.playvora_api.common.utils;

import org.springframework.beans.factory.annotation.Value;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.UUID;

public class CookiesUtils {
    @Value("${application.security.jwt.expiration}")
    private long accessTokenExpiration; // in milliseconds

    @Value("${application.security.jwt.refresh-token.expiration}")
    private long refreshTokenExpiration; // in milliseconds

    @Value("${application.security.jwt.refresh-token.long-lived-expiration:2592000000}") // 30 days in milliseconds
    private long longLivedRefreshExpiration;

    @Value("${application.security.jwt.password-reset.expiration}")
    private long passwordResetExpiration; // in milliseconds

    @Value("${application.security.jwt.account-verify.expiration}")
    private long accountVerifyExpiration; // in milliseconds

    @Value("${application.domain}")
    private String domain;

    @Value("${application.security.cookie.secure}")
    private boolean secure;

    @Value("${application.security.cookie.same-site}")
    private String sameSite;

    @Value("${spring.profiles.active}")
    private String appEnv;

    private void addCookie(HttpServletResponse response, String name, String value, int maxAge, boolean httpOnly) {
        Cookie cookie = new Cookie(name, value);

        cookie.setPath("/");
        cookie.setDomain(domain);
        cookie.setMaxAge(maxAge);
        cookie.setSecure(secure);
        cookie.setHttpOnly(httpOnly);
        cookie.setAttribute("SameSite", sameSite);
        response.addCookie(cookie);

    }

    private void clearCookie(HttpServletResponse response, String name, boolean httpOnly) {
        Cookie cookie = new Cookie(name, "");
        cookie.setHttpOnly(httpOnly);
        cookie.setSecure(secure);
        cookie.setPath("/");
        cookie.setDomain(domain);
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private String getCookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (name.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    public void addAccessTokenCookie(HttpServletResponse response, String token) {
        addCookie(response, "access_token", token, (int) (accessTokenExpiration / 1000), true);
    }

    public void addRefreshTokenCookie(HttpServletResponse response, String token) {
        addRefreshTokenCookie(response, token, false);
    }

    public void addRefreshTokenCookie(HttpServletResponse response, String token, boolean keepMeLoggedIn) {
        int maxAge = keepMeLoggedIn
                ? (int) (longLivedRefreshExpiration / 1000)
                : (int) (refreshTokenExpiration / 1000);
        addCookie(response, "refresh_token", token, maxAge, true);
    }

    public void addCsrfCookie(HttpServletResponse response, String csrfState) {
        addCookie(response, "csrfState", csrfState, 60000, true);
    }

    public void addDeviceIdCookie(HttpServletResponse response, String deviceIdCookie, String deviceId) {
        addCookie(response, deviceIdCookie, deviceId, (int)Duration.ofDays(365).toSeconds(), true);
    }

    public void addInsertIdCookie(HttpServletResponse response, String insertIdCookie, String insertId) {
        addCookie(response, insertIdCookie, insertId, (int) Duration.ofDays(365).toSeconds(), true);
    }

    public void addActivePortfolioId(HttpServletResponse response, UUID portfolioId) {
        addCookie(response, "active_portfolio_id", portfolioId.toString(), (int) Duration.ofDays(365).toSeconds(), false);
    }

    public void addResetTokenCookie(HttpServletResponse response, String token) {
        addCookie(response, "reset_token", token, (int)  (passwordResetExpiration / 1000), true);
    }

    public void addVerificationTokenCookie(HttpServletResponse response, String token) {
        addCookie(response, "verification_token", token, (int)  (accountVerifyExpiration / 1000), true);
    }

    public String getResetTokenFromCookie(HttpServletRequest request) {
        return getCookieValue(request, "reset_token");
    }

    public String getVerificationTokenFromCookie(HttpServletRequest request) {
        return getCookieValue(request, "verification_token");
    }

    public String getActivePortfolioId(HttpServletRequest request) {
        return getCookieValue(request, "active_portfolio_id");
    }

    public void clearTokenCookies(HttpServletResponse response) {
        clearCookie(response, "access_token", true);
        clearCookie(response, "refresh_token", true);
    }

    public void clearResetTokenCookie(HttpServletResponse response) {
        clearCookie(response, "reset_token", true);
    }

    public void clearVerificationTokenCookie(HttpServletResponse response) {
        clearCookie(response, "verification_token", true);
    }

    public void clearActivePortfolioId(HttpServletResponse response) {
        clearCookie(response, "active_portfolio_id", true);
    }
}
