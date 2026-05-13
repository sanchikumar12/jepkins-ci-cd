package com.edulearn.auth.service;



import com.edulearn.auth.dto.AuthResponse;
import com.edulearn.auth.entity.User;
import com.edulearn.auth.entity.UserRole;

import java.util.List;
import java.util.Optional;

public interface AuthService {

    User register(String email, String fullName, String password, String role);

    String login(String email, String password);

    void forgotPassword(String email, String oldPassword, String newPassword);

    void sendPasswordResetOtp(String email);

    void verifyPasswordResetOtp(String email, String otp);

    void resetPasswordWithOtp(String email, String otp, String newPassword);

    AuthResponse authenticateWithGoogle(String googleIdToken, String role);
    
    

    void logout(String token);

    String validateToken(String token);

    String refreshToken(String token);

    Optional<User> getUserByEmail(String email);
    List<User> findAllByRole(UserRole role);
    List<User> getAllUser();

    Optional<User> getUserById(Long userId);

    void changePassword(Long userId, String oldPassword, String newPassword);

    User updateProfile(Long userId, String fullName, String bio, String mobile);

    void deleteUser(Long userId);
}
