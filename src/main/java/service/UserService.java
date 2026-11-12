package service;

import exception.InvalidUserException;
import model.User;
import repository.JsonUserRepository;
import repository.UserRepository;
import security.PasswordUtil;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public class UserService implements UserOperations {
    private final UserRepository users;

    public UserService() {
        this(new JsonUserRepository());
    }

    public UserService(UserRepository users) {
        this.users = users;
    }

    @Override
    public boolean validateUser(User user) throws InvalidUserException {
        if (user == null) {
            throw new InvalidUserException("User details are required.");
        }
        try {
            AccountValidation.validateUsername(user.getUsername());
            AccountValidation.validateEmail(user.getEmail());
        } catch (AccountValidationException exception) {
            throw new InvalidUserException(exception.getMessage());
        }
        return true;
    }

    public User register(String username, String email, String password) throws InvalidUserException {
        try {
            AccountValidation.validateUsername(username);
            AccountValidation.validateEmail(email);
            AccountValidation.validatePassword(password);
        } catch (AccountValidationException exception) {
            throw new InvalidUserException(exception.getMessage());
        }

        User candidate = new User(
                UUID.randomUUID().toString(),
                username,
                email.trim().toLowerCase(Locale.ROOT),
                "STUDENT",
                PasswordUtil.hashPassword(password)
        );
        validateUser(candidate);
        try {
            return users.create(candidate);
        } catch (IllegalArgumentException exception) {
            throw new InvalidUserException("Username is already registered.");
        }
    }

    public Optional<User> findByUsername(String username) {
        return users.findByUsername(username);
    }

    @Override
    public boolean authenticateUser(String username, String password) {
        if (username == null || username.isBlank() || password == null) {
            return false;
        }
        return users.findByUsername(username.trim())
                .map(user -> PasswordUtil.verifyPassword(password, user.getPasswordHash()))
                .orElse(false);
    }
}
