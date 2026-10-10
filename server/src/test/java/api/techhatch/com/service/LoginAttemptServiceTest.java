package api.techhatch.com.service;

import api.techhatch.com.exception.TooManyAttemptsException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoginAttemptServiceTest {

    private final LoginAttemptService service = new LoginAttemptService();

    private void fail(String email, int times) {
        for (int i = 0; i < times; i++) {
            service.recordFailure(email);
        }
    }

    @Test
    void fourWrongPasswordsDoNotLock() {
        fail("user@example.com", 4);

        assertDoesNotThrow(() -> service.checkNotLocked("user@example.com"));
    }

    @Test
    void fiveWrongPasswordsLockTheEmail() {
        fail("user@example.com", 5);

        TooManyAttemptsException ex = assertThrows(TooManyAttemptsException.class,
                () -> service.checkNotLocked("user@example.com"));
        assertTrue(ex.getMessage().contains("Too many failed login attempts"));
    }

    @Test
    void lockIgnoresLetterCaseAndSpaces() {
        fail("User@Example.com", 5);

        assertThrows(TooManyAttemptsException.class, () -> service.checkNotLocked("  user@example.COM "));
    }

    @Test
    void lockIsPerEmail() {
        fail("user@example.com", 5);

        assertDoesNotThrow(() -> service.checkNotLocked("other@example.com"));
    }

    @Test
    void successResetsTheCounter() {
        fail("user@example.com", 4);
        service.recordSuccess("user@example.com");
        fail("user@example.com", 4);

        assertDoesNotThrow(() -> service.checkNotLocked("user@example.com"));
    }

    @Test
    void successLiftsALock() {
        fail("user@example.com", 5);
        service.recordSuccess("user@example.com");

        assertDoesNotThrow(() -> service.checkNotLocked("user@example.com"));
    }
}
