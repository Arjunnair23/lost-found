package service;

import java.util.regex.Pattern;

public final class AccountValidation {
    private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9._-]{3,32}");
    private static final Pattern EMAIL = Pattern.compile(
            "^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$"
    );

    private AccountValidation() {
    }

    public static void validateUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new AccountValidationException("username", "Username is required.");
        }
        if (!USERNAME.matcher(username).matches()) {
            throw new AccountValidationException(
                    "username",
                    "Username must be 3–32 characters using letters, numbers, dots, underscores, or hyphens."
            );
        }
    }

    public static void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new AccountValidationException("email", "Email is required.");
        }
        if (email.length() > 254) {
            throw new AccountValidationException("email", "Email must be no more than 254 characters.");
        }
        String normalized = email.trim();
        int at = normalized.indexOf('@');
        String localPart = at < 0 ? "" : normalized.substring(0, at);
        if (!EMAIL.matcher(normalized).matches()
                || localPart.length() > 64
                || localPart.startsWith(".")
                || localPart.endsWith(".")
                || localPart.contains("..")) {
            throw new AccountValidationException("email", "Enter a valid email address.");
        }
    }

    public static void validatePassword(String password) {
        if (password == null || password.length() < 10 || password.length() > 128) {
            throw new AccountValidationException("password", "Password must be 10–128 characters.");
        }
    }

    public static void validateRegistration(String username, String email, String password, String confirmation) {
        validateUsername(username);
        validateEmail(email);
        validatePassword(password);
        if (confirmation == null || !password.equals(confirmation)) {
            throw new AccountValidationException("confirmPassword", "Passwords do not match.");
        }
    }
}
