package com.hms.util;

import java.security.SecureRandom;
import java.util.regex.Pattern;

public class PasswordUtils {

    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL_CHARS = "@$!%*?&";
    private static final String ALL_CHARS = UPPERCASE + LOWERCASE + DIGITS + SPECIAL_CHARS;

    private static final int DEFAULT_PASSWORD_LENGTH = 12;
    private static final SecureRandom random = new SecureRandom();

    // Password policy pattern: min 8 chars, at least one uppercase, lowercase,
    // digit, and special char
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$");

    /**
     * Generates a secure random password meeting the password policy requirements.
     * Password will be 12 characters long with a mix of uppercase, lowercase,
     * digits, and special characters.
     * 
     * @return A randomly generated secure password
     */
    public static String generateSecurePassword() {
        return generateSecurePassword(DEFAULT_PASSWORD_LENGTH);
    }

    /**
     * Generates a secure random password of specified length.
     * 
     * @param length The desired password length (minimum 8)
     * @return A randomly generated secure password
     */
    public static String generateSecurePassword(int length) {
        if (length < 8) {
            throw new IllegalArgumentException("Password length must be at least 8 characters");
        }

        StringBuilder password = new StringBuilder(length);

        // Ensure at least one character from each required category
        password.append(UPPERCASE.charAt(random.nextInt(UPPERCASE.length())));
        password.append(LOWERCASE.charAt(random.nextInt(LOWERCASE.length())));
        password.append(DIGITS.charAt(random.nextInt(DIGITS.length())));
        password.append(SPECIAL_CHARS.charAt(random.nextInt(SPECIAL_CHARS.length())));

        // Fill the rest with random characters from all categories
        for (int i = 4; i < length; i++) {
            password.append(ALL_CHARS.charAt(random.nextInt(ALL_CHARS.length())));
        }

        // Shuffle the password to avoid predictable patterns
        return shuffleString(password.toString());
    }

    /**
     * Validates if a password meets the password policy requirements.
     * Requirements:
     * - Minimum 8 characters
     * - At least one uppercase letter
     * - At least one lowercase letter
     * - At least one digit
     * - At least one special character (@$!%*?&)
     * 
     * @param password The password to validate
     * @return true if password meets policy, false otherwise
     */
    public static boolean isValidPassword(String password) {
        if (password == null || password.isEmpty()) {
            return false;
        }
        return PASSWORD_PATTERN.matcher(password).matches();
    }

    /**
     * Shuffles the characters in a string randomly.
     * 
     * @param input The string to shuffle
     * @return The shuffled string
     */
    private static String shuffleString(String input) {
        char[] characters = input.toCharArray();
        for (int i = characters.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = characters[i];
            characters[i] = characters[j];
            characters[j] = temp;
        }
        return new String(characters);
    }
}
