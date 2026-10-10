package api.techhatch.com.util;

import api.techhatch.com.model.UserPrinciple;
import api.techhatch.com.model.Users;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    //base64 of a 48 byte test key, not used anywhere else
    private static final String SECRET = "dGVzdC1rZXktdGVzdC1rZXktdGVzdC1rZXktdGVzdC1rZXktdGVzdC1rZXktdGVzdA==";

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "SECRET_KEY", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "EXPIRATION_TIME", 60_000L);
    }

    private UserPrinciple principal(String email) {
        return new UserPrinciple(Users.builder().email(email).password("x").role(Users.Role.CANDIDATE).build());
    }

    @Test
    void tokenCarriesEmailAndRole() {
        String token = jwtUtil.generateToken("user@example.com", "RECRUITER", 7L);

        assertEquals("user@example.com", jwtUtil.getEmailFromToken(token));
        assertEquals("RECRUITER", jwtUtil.getRoleFromToken(token));
    }

    @Test
    void tokenIsValidOnlyForItsOwnUser() {
        String token = jwtUtil.generateToken("user@example.com", "CANDIDATE", 1L);

        assertTrue(jwtUtil.validateToken(token, principal("user@example.com")));
        assertFalse(jwtUtil.validateToken(token, principal("someone-else@example.com")));
    }

    @Test
    void expiredTokenIsRejected() {
        ReflectionTestUtils.setField(jwtUtil, "EXPIRATION_TIME", -1_000L);
        String token = jwtUtil.generateToken("user@example.com", "CANDIDATE", 1L);

        assertThrows(JwtException.class, () -> jwtUtil.getEmailFromToken(token));
    }

    @Test
    void changedTokenIsRejected() {
        String token = jwtUtil.generateToken("user@example.com", "CANDIDATE", 1L);
        String changed = token.substring(0, token.length() - 2) + (token.endsWith("aa") ? "bb" : "aa");

        assertThrows(JwtException.class, () -> jwtUtil.getEmailFromToken(changed));
    }

    @Test
    void garbageTokenIsRejected() {
        assertThrows(JwtException.class, () -> jwtUtil.getEmailFromToken("garbage.token.value"));
    }
}
