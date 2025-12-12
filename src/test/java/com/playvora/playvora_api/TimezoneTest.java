package com.playvora.playvora_api;

import com.playvora.playvora_api.user.entities.User;
import com.playvora.playvora_api.user.repo.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class TimezoneTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    public void testOffsetDateTimeStorage() {
        // Create a user with specific datetime
        OffsetDateTime testDateTime = OffsetDateTime.of(2025, 12, 10, 15, 30, 45, 123456789, ZoneOffset.UTC);

        User user = User.builder()
                .email("test@example.com")
                .password("password")
                .firstName("Test")
                .lastName("User")
                .createdAt(testDateTime)
                .updatedAt(testDateTime)
                .build();

        // Save the user
        User savedUser = userRepository.save(user);

        // Retrieve and verify
        User retrievedUser = userRepository.findById(savedUser.getId()).orElseThrow();

        System.out.println("Original createdAt: " + testDateTime);
        System.out.println("Retrieved createdAt: " + retrievedUser.getCreatedAt());
        System.out.println("Original offset: " + testDateTime.getOffset());
        System.out.println("Retrieved offset: " + retrievedUser.getCreatedAt().getOffset());

        // Verify the datetime and timezone are preserved
        assertThat(retrievedUser.getCreatedAt()).isEqualTo(testDateTime);
        assertThat(retrievedUser.getCreatedAt().getOffset()).isEqualTo(ZoneOffset.UTC);
    }
}