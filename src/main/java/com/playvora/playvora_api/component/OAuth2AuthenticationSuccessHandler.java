package com.playvora.playvora_api.component;

import com.playvora.playvora_api.auth.dtos.AuthProviderAttribute;
import com.playvora.playvora_api.auth.services.IAuthenticationService;
import com.playvora.playvora_api.user.entities.User;
import com.playvora.playvora_api.user.enums.AuthProvider;
import com.playvora.playvora_api.user.services.IJwtService;
import com.playvora.playvora_api.user.services.IUserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@RequiredArgsConstructor
@Component
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final IUserService userService;
    private final IJwtService jwtService;
    private final IAuthenticationService authenticationService;
    private final RedirectUriValidator redirectUriValidator;

    @Value("${application.frontend.url}")
    private String frontendUrl;

    private static final String REDIRECT_URI_COOKIE_NAME = "redirect_uri";

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;

        AuthProvider provider = AuthProvider.valueOf(oauthToken.getAuthorizedClientRegistrationId().toUpperCase());
        OAuth2User oauth2User = oauthToken.getPrincipal();

        String email = oauth2User.getAttribute("email");
        String firstName = oauth2User.getAttribute("given_name");
        String lastName = oauth2User.getAttribute("family_name");
        String profilePictureUrl = oauth2User.getAttribute("picture");
        
        // Try to get nickname from various possible attributes
        String nickname = oauth2User.getAttribute("nickname");
        if (nickname == null) {
            nickname = oauth2User.getAttribute("given_name");
        }
        if (nickname == null) {
            nickname = oauth2User.getAttribute("name");
        }


        String oauthProviderId = oauth2User.getAttribute("sub");
        if (oauthProviderId == null) {
            oauthProviderId = oauth2User.getAttribute("id");
        }
        if (oauthProviderId == null) {
            oauthProviderId = oauth2User.getName();
        }


        String providerId = provider.name().toLowerCase() + ":" + oauthProviderId;

        User user = userService.findByEmail(email);
        if (user == null) {
            AuthProviderAttribute authProviderAttribute = AuthProviderAttribute.builder()
                    .email(email)
                    .firstName(firstName)
                    .lastName(lastName)
                    .nickname(nickname)
                    .profilePictureUrl(profilePictureUrl)
                    .providerId(providerId)
                    .provider(provider)
                    .build();
            user = userService.createUserFromOAuth2(authProviderAttribute);
        }

        String accessToken = jwtService.generateTokenForOAuth2Login(user);
        // Save refresh token to database
        String refreshToken = authenticationService.generateRefreshTokenForOAuth2Login(user);

        String targetUrl = determineTargetUrl(request);

        String redirectUrl = UriComponentsBuilder.fromUriString(targetUrl)
                .queryParam("accessToken", accessToken)
                .queryParam("refreshToken", refreshToken)
                .build()
                .toUriString();

        clearRedirectUriCookie(request, response);

        log.debug("Redirecting OAuth2 user to {}", redirectUrl);
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    protected String determineTargetUrl(HttpServletRequest request) {
        String redirectUri = getRedirectUriFromCookie(request);

        if (redirectUri != null && redirectUriValidator.isAuthorizedRedirectUri(redirectUri)) {
            return redirectUri;
        }

        // Fallback to default frontend callback URL
        return UriComponentsBuilder.fromUriString(frontendUrl)
                .path("/callback")
                .build()
                .toUriString();
    }

    private String getRedirectUriFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (REDIRECT_URI_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private void clearRedirectUriCookie(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return;
        }
        for (Cookie cookie : cookies) {
            if (REDIRECT_URI_COOKIE_NAME.equals(cookie.getName())) {
                Cookie toClear = new Cookie(REDIRECT_URI_COOKIE_NAME, null);
                toClear.setPath("/");
                toClear.setHttpOnly(true);
                toClear.setSecure(true);
                toClear.setMaxAge(0);
                toClear.setAttribute("SameSite", "Lax");
                response.addCookie(toClear);
                break;
            }
        }
    }
}
