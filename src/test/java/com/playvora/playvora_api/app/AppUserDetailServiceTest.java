package com.playvora.playvora_api.app;

import com.playvora.playvora_api.user.entities.User;
import com.playvora.playvora_api.user.enums.AuthProvider;
import com.playvora.playvora_api.user.repo.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppUserDetailServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AppUserDetailService appUserDetailService;

    private User testUser;
    private final String TEST_EMAIL = "test@playvora.com";

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
                .userRoles(null)
                .build();
    }

    @Test
    @DisplayName("Should load user by email successfully")
    void testLoadUserByUsernameSuccess() {
        // Arrange
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));

        // Act
        UserDetails result = appUserDetailService.loadUserByUsername(TEST_EMAIL);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_EMAIL, result.getUsername());
        assertTrue(result.isEnabled());
        verify(userRepository, times(1)).findByEmail(TEST_EMAIL);
    }

    @Test
    @DisplayName("Should throw UsernameNotFoundException when user not found")
    void testLoadUserByUsernameNotFound() {
        // Arrange
        String nonExistentEmail = "notfound@playvora.com";
        when(userRepository.findByEmail(nonExistentEmail)).thenReturn(Optional.empty());

        // Act & Assert
        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> appUserDetailService.loadUserByUsername(nonExistentEmail)
        );

        assertTrue(exception.getMessage().contains(nonExistentEmail));
        verify(userRepository, times(1)).findByEmail(nonExistentEmail);
    }

    @Test
    @DisplayName("Should return UserDetails with correct authorities")
    void testLoadUserWithRoles() {
        // Arrange
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));

        // Act
        UserDetails result = appUserDetailService.loadUserByUsername(TEST_EMAIL);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getAuthorities());
        verify(userRepository, times(1)).findByEmail(TEST_EMAIL);
    }
}

