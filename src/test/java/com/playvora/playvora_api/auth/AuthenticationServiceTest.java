package com.playvora.playvora_api.auth;

import com.playvora.playvora_api.app.AppUserDetail;
import com.playvora.playvora_api.auth.services.impl.AuthenticationService;
import com.playvora.playvora_api.user.entities.User;
import com.playvora.playvora_api.user.enums.AuthProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthenticationService authenticationService;

    private User testUser;
    private UserDetails userDetails;
    private final String TEST_EMAIL = "test@playvora.com";
    private final String TEST_PASSWORD = "password123";

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .email(TEST_EMAIL)
                .password("encodedPassword")
                .firstName("Test")
                .lastName("User")
                .provider(AuthProvider.LOCAL)
                .enabled(true)
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .updatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .userRoles(new HashSet<>())
                .build();

        userDetails = new AppUserDetail(testUser);
    }

    @Test
    @DisplayName("Should authenticate user successfully with valid credentials")
    void testAuthenticateSuccess() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken(TEST_EMAIL, TEST_PASSWORD));
        when(userDetailsService.loadUserByUsername(TEST_EMAIL)).thenReturn(userDetails);

        // Act
        UserDetails result = authenticationService.authenticate(TEST_EMAIL, TEST_PASSWORD);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_EMAIL, result.getUsername());
        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userDetailsService, times(1)).loadUserByUsername(TEST_EMAIL);
    }

    @Test
    @DisplayName("Should throw exception when authentication fails")
    void testAuthenticateFailure() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        // Act & Assert
        assertThrows(BadCredentialsException.class, () -> {
            authenticationService.authenticate(TEST_EMAIL, "wrongpassword");
        });

        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userDetailsService, never()).loadUserByUsername(anyString());
    }

    @Test
    @DisplayName("Should generate JWT token for authenticated user")
    void testGenerateToken() {
        // Act
        String token = authenticationService.generateTokenForLocalLogin(userDetails);

        // Assert
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.split("\\.").length == 3); // JWT has 3 parts separated by dots
    }

    @Test
    @DisplayName("Should generate different tokens for same user at different times")
    void testGenerateDifferentTokens() throws InterruptedException {
        // Act
        String token1 = authenticationService.generateTokenForLocalLogin(userDetails);
        Thread.sleep(10); // Small delay to ensure different timestamp
        String token2 = authenticationService.generateTokenForLocalLogin(userDetails);

        // Assert
        assertNotNull(token1);
        assertNotNull(token2);
        assertNotEquals(token1, token2);
    }
}

