package api.techhatch.com.service;

import api.techhatch.com.dto.request.LoginRequest;
import api.techhatch.com.dto.request.RegisterRequest;
import api.techhatch.com.dto.response.AuthResponse;
import api.techhatch.com.dto.response.OtpSentResponse;
import api.techhatch.com.exception.BadRequestException;
import api.techhatch.com.exception.DuplicateResourceException;
import api.techhatch.com.exception.OtpInvalidException;
import api.techhatch.com.exception.UnauthorizedException;
import api.techhatch.com.model.OtpVerification;
import api.techhatch.com.model.PendingRegistration;
import api.techhatch.com.model.Users;
import api.techhatch.com.repository.PendingRegistrationRepo;
import api.techhatch.com.repository.UserRepo;
import api.techhatch.com.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String EMAIL = "user@example.com";

    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private UserRepo userRepo;
    @Mock
    private PendingRegistrationRepo pendingRegistrationRepo;
    @Mock
    private BCryptPasswordEncoder encoder;
    @Mock
    private AuthenticationManager authManager;
    @Mock
    private OtpService otpService;
    @Mock
    private LoginAttemptService loginAttemptService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest(String role) {
        RegisterRequest request = new RegisterRequest();
        request.setEmail(EMAIL);
        request.setPassword("secret123");
        request.setRole(role);
        return request;
    }

    private LoginRequest loginRequest() {
        LoginRequest request = new LoginRequest();
        request.setEmail(EMAIL);
        request.setPassword("secret123");
        return request;
    }

    private Users activeUser() {
        return Users.builder()
                .id(5L)
                .email(EMAIL)
                .password("hashed")
                .role(Users.Role.CANDIDATE)
                .emailVerified(true)
                .accountStatus(Users.AccountStatus.ACTIVE)
                .build();
    }

    @Test
    void nobodyCanSignUpAsAdmin() {
        assertThrows(BadRequestException.class, () -> authService.register(registerRequest("ADMIN")));
        assertThrows(BadRequestException.class, () -> authService.register(registerRequest("BOSS")));

        verify(pendingRegistrationRepo, never()).save(any());
        verify(otpService, never()).generateAndSendOtp(anyString(), any());
    }

    @Test
    void registeredEmailCannotSignUpAgain() {
        when(userRepo.existsByEmail(EMAIL)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(registerRequest("CANDIDATE")));
    }

    @Test
    void newSignUpStoresAPendingRegistrationAndSendsOtp() {
        when(userRepo.existsByEmail(EMAIL)).thenReturn(false);
        when(pendingRegistrationRepo.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(encoder.encode("secret123")).thenReturn("hashed");

        OtpSentResponse response = authService.register(registerRequest("recruiter"));

        ArgumentCaptor<PendingRegistration> saved = ArgumentCaptor.forClass(PendingRegistration.class);
        verify(pendingRegistrationRepo).save(saved.capture());
        assertEquals(Users.Role.RECRUITER, saved.getValue().getRole());
        assertEquals("hashed", saved.getValue().getPassword());
        verify(otpService).generateAndSendOtp(EMAIL, OtpVerification.OtpPurpose.REGISTRATION);
        assertTrue(response.isOtpSent());
    }

    @Test
    void expiredPendingRegistrationIsReplacedInsteadOfBlockingTheUser() {
        PendingRegistration expired = PendingRegistration.builder()
                .email(EMAIL).password("old").role(Users.Role.CANDIDATE)
                .expiresAt(LocalDateTime.now().minusHours(1))
                .build();
        when(userRepo.existsByEmail(EMAIL)).thenReturn(false);
        when(pendingRegistrationRepo.findByEmail(EMAIL)).thenReturn(Optional.of(expired));
        when(encoder.encode("secret123")).thenReturn("hashed");

        OtpSentResponse response = authService.register(registerRequest("CANDIDATE"));

        verify(pendingRegistrationRepo).delete(expired);
        ArgumentCaptor<PendingRegistration> saved = ArgumentCaptor.forClass(PendingRegistration.class);
        verify(pendingRegistrationRepo).save(saved.capture());
        assertNotSame(expired, saved.getValue());
        assertTrue(saved.getValue().getExpiresAt().isAfter(LocalDateTime.now()));
        assertTrue(response.isOtpSent());
    }

    @Test
    void loginGivesATokenWithoutAnOtp() {
        when(userRepo.findUserByEmail(EMAIL)).thenReturn(Optional.of(activeUser()));
        when(jwtUtil.generateToken(EMAIL, "CANDIDATE", 5L)).thenReturn("the-token");

        AuthResponse response = authService.login(loginRequest());

        assertEquals("the-token", response.getToken());
        assertEquals("CANDIDATE", response.getRole());
        verify(otpService, never()).generateAndSendOtp(anyString(), any());
        verify(loginAttemptService).recordSuccess(EMAIL);
    }

    @Test
    void wrongPasswordIsCountedAndRefused() {
        when(authManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(UnauthorizedException.class, () -> authService.login(loginRequest()));

        verify(loginAttemptService).recordFailure(EMAIL);
        verify(jwtUtil, never()).generateToken(anyString(), anyString(), any());
    }

    @Test
    void forgotPasswordGivesTheSameAnswerForUnknownAndKnownEmail() {
        when(userRepo.findUserByEmail("nobody@example.com")).thenReturn(Optional.empty());
        when(userRepo.findUserByEmail(EMAIL)).thenReturn(Optional.of(activeUser()));

        OtpSentResponse unknown = authService.forgotPassword("nobody@example.com");
        OtpSentResponse known = authService.forgotPassword(EMAIL);

        assertEquals(unknown.getMessage(), known.getMessage());
        assertEquals(unknown.isOtpSent(), known.isOtpSent());
        verify(otpService, never()).generateAndSendOtp(eq("nobody@example.com"), any());
        verify(otpService).generateAndSendOtp(EMAIL, OtpVerification.OtpPurpose.PASSWORD_RESET);
    }

    @Test
    void resetPasswordStoresTheNewHashedPassword() {
        Users user = activeUser();
        when(userRepo.findUserByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(encoder.encode("newSecret1")).thenReturn("new-hash");

        authService.resetPassword(EMAIL, "123456", "newSecret1");

        verify(otpService).verifyOtp("123456", EMAIL, OtpVerification.OtpPurpose.PASSWORD_RESET);
        assertEquals("new-hash", user.getPassword());
        verify(userRepo).save(user);
        verify(loginAttemptService).recordSuccess(EMAIL);
    }

    @Test
    void resetPasswordWithAWrongOtpKeepsTheOldPassword() {
        Users user = activeUser();
        when(userRepo.findUserByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(otpService.verifyOtp("000000", EMAIL, OtpVerification.OtpPurpose.PASSWORD_RESET))
                .thenThrow(new OtpInvalidException("Invalid OTP. 2 attempts remaining"));

        assertThrows(OtpInvalidException.class, () -> authService.resetPassword(EMAIL, "000000", "newSecret1"));

        assertEquals("hashed", user.getPassword());
        verify(userRepo, never()).save(any());
    }

    @Test
    void resetPasswordForAnUnknownEmailLooksLikeAWrongOtp() {
        when(userRepo.findUserByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThrows(OtpInvalidException.class,
                () -> authService.resetPassword("nobody@example.com", "123456", "newSecret1"));
    }
}
