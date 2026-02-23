package com.example.sobzybackend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.regex.Pattern;

/**
 * Password hashing and verification service using PBKDF2
 * Production-ready implementation with salt and iterations
 */
@Slf4j
@Service
public class PasswordService {

    // PBKDF2 Configuration
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;

    // Password strength regex patterns
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[^a-zA-Z0-9]");

    /**
     * Hash a password using PBKDF2 with salt
     * Format: iterations:salt:hash
     */
    public String hashPassword(String password) {
        try {
            // Generate random salt
            byte[] salt = generateSalt();

            // Hash password with salt
            byte[] hash = pbkdf2(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);

            // Encode and format: iterations:salt:hash
            String saltBase64 = Base64.getEncoder().encodeToString(salt);
            String hashBase64 = Base64.getEncoder().encodeToString(hash);

            String hashedPassword = ITERATIONS + ":" + saltBase64 + ":" + hashBase64;
            log.debug("Password hashed successfully");
            return hashedPassword;

        } catch (Exception e) {
            log.error("Error hashing password", e);
            throw new RuntimeException("Failed to hash password", e);
        }
    }

    /**
     * Verify a password against a hash
     */
    public boolean verifyPassword(String password, String storedHash) {
        try {
            // Split stored hash into parts
            String[] parts = storedHash.split(":");
            if (parts.length != 3) {
                log.warn("Invalid hash format");
                return false;
            }

            int iterations = Integer.parseInt(parts[0]);
            byte[] salt = Base64.getDecoder().decode(parts[1]);
            byte[] hash = Base64.getDecoder().decode(parts[2]);

            // Hash the provided password with the same salt
            byte[] testHash = pbkdf2(password.toCharArray(), salt, iterations, hash.length * 8);

            // Compare hashes in constant time
            return slowEquals(hash, testHash);

        } catch (Exception e) {
            log.error("Error verifying password", e);
            return false;
        }
    }

    /**
     * Generate random salt
     */
    private byte[] generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        return salt;
    }

    /**
     * PBKDF2 key derivation
     */
    private byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLength)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
        return factory.generateSecret(spec).getEncoded();
    }

    /**
     * Constant-time comparison to prevent timing attacks
     */
    private boolean slowEquals(byte[] a, byte[] b) {
        int diff = a.length ^ b.length;
        for (int i = 0; i < a.length && i < b.length; i++) {
            diff |= a[i] ^ b[i];
        }
        return diff == 0;
    }

    /**
     * Validate password strength
     * Requirements:
     * - At least 8 characters
     * - At least one uppercase letter
     * - At least one lowercase letter
     * - At least one digit
     * - At least one special character
     */
    public boolean isPasswordStrong(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }

        boolean hasUppercase = UPPERCASE_PATTERN.matcher(password).find();
        boolean hasLowercase = LOWERCASE_PATTERN.matcher(password).find();
        boolean hasDigit = DIGIT_PATTERN.matcher(password).find();
        boolean hasSpecial = SPECIAL_CHAR_PATTERN.matcher(password).find();

        return hasUppercase && hasLowercase && hasDigit && hasSpecial;
    }

    /**
     * Get password strength score (0-100)
     */
    public int getPasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            return 0;
        }

        int score = 0;

        // Length score (max 40 points)
        int lengthScore = Math.min(password.length() * 4, 40);
        score += lengthScore;

        // Character variety (max 40 points)
        if (UPPERCASE_PATTERN.matcher(password).find()) score += 10;
        if (LOWERCASE_PATTERN.matcher(password).find()) score += 10;
        if (DIGIT_PATTERN.matcher(password).find()) score += 10;
        if (SPECIAL_CHAR_PATTERN.matcher(password).find()) score += 10;

        // Additional complexity (max 20 points)
        long uniqueChars = password.chars().distinct().count();
        score += Math.min((int)(uniqueChars / 2), 20);

        return Math.min(score, 100);
    }

    /**
     * Get password strength description
     */
    public String getPasswordStrengthDescription(int score) {
        if (score < 30) return "Weak";
        if (score < 60) return "Fair";
        if (score < 80) return "Good";
        return "Strong";
    }

    /**
     * Validate password meets minimum requirements
     */
    public PasswordValidationResult validatePassword(String password) {
        PasswordValidationResult result = new PasswordValidationResult();
        result.setValid(true);

        if (password == null || password.isEmpty()) {
            result.setValid(false);
            result.addError("Password is required");
            return result;
        }

        if (password.length() < 8) {
            result.setValid(false);
            result.addError("Password must be at least 8 characters long");
        }

        if (!UPPERCASE_PATTERN.matcher(password).find()) {
            result.setValid(false);
            result.addError("Password must contain at least one uppercase letter");
        }

        if (!LOWERCASE_PATTERN.matcher(password).find()) {
            result.setValid(false);
            result.addError("Password must contain at least one lowercase letter");
        }

        if (!DIGIT_PATTERN.matcher(password).find()) {
            result.setValid(false);
            result.addError("Password must contain at least one digit");
        }

        if (!SPECIAL_CHAR_PATTERN.matcher(password).find()) {
            result.setValid(false);
            result.addError("Password must contain at least one special character");
        }

        result.setStrengthScore(getPasswordStrength(password));
        result.setStrengthDescription(getPasswordStrengthDescription(result.getStrengthScore()));

        return result;
    }

    /**
     * Inner class for password validation result
     */
    @lombok.Data
    public static class PasswordValidationResult {
        private boolean valid;
        private java.util.List<String> errors = new java.util.ArrayList<>();
        private int strengthScore;
        private String strengthDescription;

        public void addError(String error) {
            this.errors.add(error);
        }
    }
}
