package api.techhatch.com.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid Email format")
    private String email;

    @NotBlank(message = "Please provide a valid OTP")
    @Size(max = 6,min = 6, message = "Please provide a valid OTP")
    private String otpCode;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be 6+ characters")
    private String newPassword;
}
