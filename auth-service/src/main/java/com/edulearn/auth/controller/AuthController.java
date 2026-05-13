package com.edulearn.auth.controller;

import com.edulearn.auth.dto.AuthResponse;
import com.edulearn.auth.dto.ForgotPasswordRequest;
import com.edulearn.auth.dto.GoogleAuthRequest;
import com.edulearn.auth.dto.LoginRequest;
import com.edulearn.auth.dto.RegisterRequest;
import com.edulearn.auth.dto.UpdateProfileRequest;
import com.edulearn.auth.entity.User;
import com.edulearn.auth.entity.UserRole;
import com.edulearn.auth.service.AuthService;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
@Tag(name = "Authentication", description = "Authentication and Authorization API endpoints")
public class AuthController {

    @Autowired
    private AuthService authService;

    @GetMapping("/all")
    public ResponseEntity<List<User>> getAll() {
        List<User> users = authService.getAllUser();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/role")
    public ResponseEntity<List<User>> getUserByRole(@RequestParam String role) {
        UserRole roleEnum = UserRole.valueOf(role.toUpperCase());
        List<User> users = authService.findAllByRole(roleEnum);
        return ResponseEntity.ok(users);
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Create a new user account with email, password, and role")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "User registered successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid registration request")
    })
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        try {
            User user = authService.register(
                    request.getEmail(),
                    request.getFullName(),
                    request.getPassword(),
                    request.getRole());

            AuthResponse response = new AuthResponse();
            response.setSuccess(true);
            response.setMessage("User registered successfully");
            response.setUserId(user.getUserId());
            response.setEmail(user.getEmail());
            response.setFullName(user.getFullName());
            response.setRole(user.getRole().toString());
            response.setMobile(user.getMobile());
            response.setBio(user.getBio());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (RuntimeException e) {
            AuthResponse response = new AuthResponse();
            response.setSuccess(false);
            response.setMessage(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user with email and password, returns JWT token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Login successful, JWT token returned", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        try {
            String token = authService.login(request.getEmail(), request.getPassword());
            User user = authService.getUserByEmail(request.getEmail()).orElseThrow();

            AuthResponse response = new AuthResponse();
            response.setSuccess(true);
            response.setMessage("Login successful");
            response.setToken(token);
            response.setUserId(user.getUserId());
            response.setEmail(user.getEmail());
            response.setFullName(user.getFullName());
            response.setRole(user.getRole().toString());
            response.setMobile(user.getMobile());
            response.setBio(user.getBio());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            AuthResponse response = new AuthResponse();
            response.setSuccess(false);
            response.setMessage(e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Forgot password", description = "Reset password by verifying old password and setting a new password")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Password reset successful", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid forgot password request")
    })
    public ResponseEntity<AuthResponse> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        AuthResponse response = new AuthResponse();
        try {
            if (request.getEmail() == null || request.getEmail().isBlank()) {
                throw new RuntimeException("Email is required");
            }
            if (request.getOldPassword() == null || request.getOldPassword().isBlank()) {
                throw new RuntimeException("Old password is required");
            }
            if (request.getNewPassword() == null || request.getNewPassword().isBlank()) {
                throw new RuntimeException("New password is required");
            }
            if (request.getConfirmPassword() == null || request.getConfirmPassword().isBlank()) {
                throw new RuntimeException("Confirm password is required");
            }
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                throw new RuntimeException("New password and confirm password do not match");
            }

            authService.forgotPassword(request.getEmail(), request.getOldPassword(), request.getNewPassword());
            response.setSuccess(true);
            response.setMessage("Password reset successful. Please login with your new password.");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.setSuccess(false);
            response.setMessage(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @PostMapping("/forgot-password/send-otp")
    @Operation(summary = "Send forgot-password OTP", description = "Send a password reset OTP to the user's account email")
    public ResponseEntity<AuthResponse> sendForgotPasswordOtp(@RequestBody ForgotPasswordRequest request) {
        AuthResponse response = new AuthResponse();
        try {
            authService.sendPasswordResetOtp(request.getEmail());
            response.setSuccess(true);
            response.setMessage("OTP sent to your email. Please check your inbox.");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.setSuccess(false);
            response.setMessage(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @PostMapping("/forgot-password/verify-otp")
    @Operation(summary = "Verify forgot-password OTP", description = "Verify OTP before allowing password reset")
    public ResponseEntity<AuthResponse> verifyForgotPasswordOtp(@RequestBody ForgotPasswordRequest request) {
        AuthResponse response = new AuthResponse();
        try {
            authService.verifyPasswordResetOtp(request.getEmail(), request.getOtp());
            response.setSuccess(true);
            response.setMessage("OTP verified. You can now reset your password.");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.setSuccess(false);
            response.setMessage(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @PostMapping("/forgot-password/reset")
    @Operation(summary = "Reset password with OTP", description = "Reset password after OTP has been verified")
    public ResponseEntity<AuthResponse> resetForgotPassword(@RequestBody ForgotPasswordRequest request) {
        AuthResponse response = new AuthResponse();
        try {
            if (request.getNewPassword() == null || request.getNewPassword().isBlank()) {
                throw new RuntimeException("New password is required");
            }
            if (request.getConfirmPassword() == null || request.getConfirmPassword().isBlank()) {
                throw new RuntimeException("Confirm password is required");
            }
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                throw new RuntimeException("New password and confirm password do not match");
            }

            authService.resetPasswordWithOtp(request.getEmail(), request.getOtp(), request.getNewPassword());
            response.setSuccess(true);
            response.setMessage("Password reset successful. Please login with your new password.");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.setSuccess(false);
            response.setMessage(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @PutMapping("/users/{id}/profile")
    @Operation(summary = "Update user profile", description = "Update full name, mobile, and bio for a user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Profile updated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid update request"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<AuthResponse> updateProfile(@PathVariable Long id, @RequestBody UpdateProfileRequest request) {
        AuthResponse response = new AuthResponse();
        try {
            if (request.getFullName() == null || request.getFullName().isBlank()) {
                throw new RuntimeException("Full name is required");
            }

            User updatedUser = authService.updateProfile(
                    id,
                    request.getFullName().trim(),
                    request.getBio() != null ? request.getBio().trim() : "",
                    request.getMobile() != null ? request.getMobile().trim() : "");

            response.setSuccess(true);
            response.setMessage("Profile updated successfully");
            response.setUserId(updatedUser.getUserId());
            response.setEmail(updatedUser.getEmail());
            response.setFullName(updatedUser.getFullName());
            response.setRole(updatedUser.getRole().toString());
            response.setMobile(updatedUser.getMobile());
            response.setBio(updatedUser.getBio());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.setSuccess(false);
            response.setMessage(e.getMessage());
            HttpStatus status = e.getMessage() != null && e.getMessage().contains("not found")
                    ? HttpStatus.NOT_FOUND
                    : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(response);
        }
    }

    @PostMapping("/google")
    @Operation(summary = "Google authentication", description = "Authenticate using Google ID token, auto-register if user does not exist, and return JWT token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Google auth successful", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request or token")
    })
    public ResponseEntity<AuthResponse> googleAuth(@RequestBody GoogleAuthRequest request) {
        try {
            if (request.getToken() == null || request.getToken().isBlank()) {
                AuthResponse errorResponse = new AuthResponse();
                errorResponse.setSuccess(false);
                errorResponse.setMessage("Google token is required.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            AuthResponse response = authService.authenticateWithGoogle(request.getToken(), request.getRole());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            AuthResponse errorResponse = new AuthResponse();
            errorResponse.setSuccess(false);
            errorResponse.setMessage(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "Get user details by ID", description = "Fetch user profile information without authentication")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<AuthResponse> getUserById(@PathVariable Long id) {
        return authService.getUserById(id)
                .map(user -> {
                    AuthResponse response = new AuthResponse();
                    response.setSuccess(true);
                    response.setMessage("User retrieved successfully");
                    response.setUserId(user.getUserId());
                    response.setEmail(user.getEmail());
                    response.setFullName(user.getFullName());
                    response.setRole(user.getRole().toString());
                    response.setMobile(user.getMobile());
                    response.setBio(user.getBio());
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    AuthResponse response = new AuthResponse();
                    response.setSuccess(false);
                    response.setMessage("User not found with id: " + id);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
                });
    }

    @GetMapping("/test")
    String getresponce() {
        return "hellow";
    }

    @GetMapping("/validate")
    @Operation(summary = "Validate JWT token", description = "Verify if the provided JWT token is valid")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token is valid", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "401", description = "Invalid or expired token")
    })
    public ResponseEntity<AuthResponse> validateToken(@RequestParam String token) {
        try {
            String userId = authService.validateToken(token);

            AuthResponse response = new AuthResponse();
            response.setSuccess(true);
            response.setMessage("Token is valid");
            response.setUserId(Long.parseLong(userId));

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            AuthResponse response = new AuthResponse();
            response.setSuccess(false);
            response.setMessage(e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh JWT token", description = "Generate a new JWT token from an existing valid token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token refreshed successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "401", description = "Invalid or expired token")
    })
    public ResponseEntity<AuthResponse> refreshToken(@RequestParam String token) {
        try {
            String newToken = authService.refreshToken(token);

            AuthResponse response = new AuthResponse();
            response.setSuccess(true);
            response.setMessage("Token refreshed successfully");
            response.setToken(newToken);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            AuthResponse response = new AuthResponse();
            response.setSuccess(false);
            response.setMessage(e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }
}
