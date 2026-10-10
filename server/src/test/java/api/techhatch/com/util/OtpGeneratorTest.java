package api.techhatch.com.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class OtpGeneratorTest {

    @Test
    void otpIsAlwaysSixDigits() {
        for (int i = 0; i < 500; i++) {
            String otp = OtpGenerator.generateOtp();
            assertTrue(otp.matches("\\d{6}"), "not six digits: " + otp);
        }
    }

    @Test
    void otpExpiresInTenMinutes() {
        LocalDateTime expiry = OtpGenerator.calculateExpiryTime();

        assertTrue(expiry.isAfter(LocalDateTime.now().plusMinutes(9)));
        assertTrue(expiry.isBefore(LocalDateTime.now().plusMinutes(11)));
    }
}
