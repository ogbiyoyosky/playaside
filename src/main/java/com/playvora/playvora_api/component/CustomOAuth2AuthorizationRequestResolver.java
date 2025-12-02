package com.playvora.playvora_api.component;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

@Component
public class CustomOAuth2AuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

   private final OAuth2AuthorizationRequestResolver defaultAuthorizationRequestResolver;

   public CustomOAuth2AuthorizationRequestResolver(ClientRegistrationRepository clientRegistrationRepository) {
       this.defaultAuthorizationRequestResolver = new DefaultOAuth2AuthorizationRequestResolver(
               clientRegistrationRepository, "/oauth2/authorization");
   }

   @Override
   public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
       OAuth2AuthorizationRequest authorizationRequest = this.defaultAuthorizationRequestResolver.resolve(request);
       return authorizationRequest != null ? customizeAuthorizationRequest(authorizationRequest) : null;
   }

   @Override
   public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
       OAuth2AuthorizationRequest authorizationRequest = this.defaultAuthorizationRequestResolver.resolve(request, clientRegistrationId);
       return authorizationRequest != null ? customizeAuthorizationRequest(authorizationRequest) : null;
   }

   private OAuth2AuthorizationRequest customizeAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest) {
       // Check if this is a Google OAuth request
       if (authorizationRequest.getAuthorizationUri().contains("accounts.google.com")) {
           return OAuth2AuthorizationRequest.from(authorizationRequest)
                   .additionalParameters(params -> {
                       params.put("prompt", "select_account");
                   })
                   .build();
       }
       return authorizationRequest;
   }
}