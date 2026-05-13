package com.edulearn.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ForgotPasswordRequest {
    private String email;
    private String otp;
    private String oldPassword;
    private String newPassword;
    private String confirmPassword;
}
