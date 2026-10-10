package api.techhatch.com.service;

import api.techhatch.com.exception.TooManyAttemptsException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Blocks login for an email after too many wrong passwords.
 * Kept in memory, so the counters reset when the server restarts.
 */
@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final int WINDOW_MINUTES = 15;
    private static final int LOCK_MINUTES = 15;
    private static final int MAX_TRACKED_EMAILS = 10000;

    private record Attempts(int count, LocalDateTime firstFailure, LocalDateTime lockedUntil) {}

    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();

    /**
     * @throws TooManyAttemptsException when the email is currently locked
     */
    public void checkNotLocked(String email) {
        Attempts current = attempts.get(key(email));
        if (current == null || current.lockedUntil() == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(current.lockedUntil())) {
            long minsLeft = Math.max(1, Duration.between(now, current.lockedUntil()).toMinutes() + 1);
            throw new TooManyAttemptsException(
                    "Too many failed login attempts. Please try again in " + minsLeft + " minutes");
        }

        //lock is over
        attempts.remove(key(email));
    }

    public void recordFailure(String email) {
        LocalDateTime now = LocalDateTime.now();

        if (attempts.size() > MAX_TRACKED_EMAILS) {
            removeExpired(now);
        }

        attempts.compute(key(email), (k, current) -> {
            //first failure, or the previous ones are too old to count
            if (current == null || now.isAfter(current.firstFailure().plusMinutes(WINDOW_MINUTES))) {
                return new Attempts(1, now, null);
            }
            int count = current.count() + 1;
            LocalDateTime lockedUntil = count >= MAX_ATTEMPTS ? now.plusMinutes(LOCK_MINUTES) : null;
            return new Attempts(count, current.firstFailure(), lockedUntil);
        });
    }

    public void recordSuccess(String email) {
        attempts.remove(key(email));
    }

    private void removeExpired(LocalDateTime now) {
        attempts.entrySet().removeIf(e -> {
            Attempts a = e.getValue();
            boolean windowOver = now.isAfter(a.firstFailure().plusMinutes(WINDOW_MINUTES));
            boolean lockOver = a.lockedUntil() == null || now.isAfter(a.lockedUntil());
            return windowOver && lockOver;
        });
    }

    private String key(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
