package service;

import exception.InvalidUserException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import repository.JsonUserRepository;
import storage.JsonStore;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AccountValidationTest {
    @TempDir
    Path directory;

    @Test
    void validatesUsernameEmailPasswordAndConfirmationLimits() {
        assertDoesNotThrow(() -> AccountValidation.validateRegistration(
                "student.name-1", "student@example.edu", "1234567890", "1234567890"));
        String maxEmail = "a".repeat(64) + "@"
                + "b".repeat(63) + "." + "c".repeat(63) + "." + "d".repeat(61);
        assertEquals(254, maxEmail.length());
        assertDoesNotThrow(() -> AccountValidation.validateEmail(maxEmail));
        assertDoesNotThrow(() -> AccountValidation.validateRegistration(
                "abc", "student@example.edu", "p".repeat(128), "p".repeat(128)));
        assertEquals("username", assertThrows(AccountValidationException.class,
                () -> AccountValidation.validateUsername("a".repeat(33))).getField());
        assertEquals("username", assertThrows(AccountValidationException.class,
                () -> AccountValidation.validateUsername("   ")).getField());
        assertEquals("email", assertThrows(AccountValidationException.class,
                () -> AccountValidation.validateEmail("e".repeat(255) + "@example.edu")).getField());
        assertEquals("password", assertThrows(AccountValidationException.class,
                () -> AccountValidation.validatePassword("p".repeat(9))).getField());
        assertEquals("password", assertThrows(AccountValidationException.class,
                () -> AccountValidation.validatePassword("p".repeat(129))).getField());
        assertEquals("confirmPassword", assertThrows(AccountValidationException.class,
                () -> AccountValidation.validateRegistration(
                        "student", "student@example.edu", "1234567890", "different")).getField());
    }

    @Test
    void duplicateUsernameIsCaseInsensitiveAndEmailIsNotUnique() throws Exception {
        UserService service = new UserService(new JsonUserRepository(new JsonStore(directory)));
        service.register("student.one", "same@example.edu", "1234567890");
        InvalidUserException duplicate = assertThrows(InvalidUserException.class,
                () -> service.register("STUDENT.ONE", "another@example.edu", "1234567890"));
        assertEquals("Username is already registered.", duplicate.getMessage());
        assertDoesNotThrow(() -> service.register("student.two", "same@example.edu", "1234567890"));
    }
}
