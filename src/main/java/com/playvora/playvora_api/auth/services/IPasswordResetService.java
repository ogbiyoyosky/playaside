package com.playvora.playvora_api.auth.services;

public interface IPasswordResetService {
    /**
     * Initiates the password reset process for the provided email.
     *
     * @param email email address to reset
     * @return encoded token string if the user exists, otherwise null
     */
    String initiatePasswordReset(String email);

    /**
     * Resets the user password using a previously issued token.
     *
     * @param token      encoded reset token
     * @param newPassword plaintext new password
     */
    void resetPassword(String token, String newPassword);
}

