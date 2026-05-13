package com.edulearn.auth.service;

import com.edulearn.auth.entity.User;
import com.edulearn.auth.entity.UserRole;
import com.edulearn.auth.repository.UserRepository;
import com.edulearn.auth.util.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("AuthService Unit Tests")
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthServiceImpl authService;

    private BCryptPasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        passwordEncoder = new BCryptPasswordEncoder();
        ReflectionTestUtils.setField(authService, "passwordEncoder", passwordEncoder);
    }

    @Test
    @DisplayName("Register - Should successfully register a new user")
    void testRegisterSuccess() {
        String email = "john@example.com";
        String fullName = "John Doe";
        String password = "password123";
        String role = "STUDENT";

        User user = new User();
        user.setUserId(1L);
        user.setEmail(email);
        user.setFullName(fullName);
        user.setRole(UserRole.STUDENT);

        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(user);

        User result = authService.register(email, fullName, password, role);

        assertNotNull(result);
        assertEquals(email, result.getEmail());
        assertEquals(fullName, result.getFullName());
        assertEquals(UserRole.STUDENT, result.getRole());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Register - Should throw exception if email already exists")
    void testRegisterUserAlreadyExists() {
        String email = "john@example.com";
        when(userRepository.existsByEmail(email)).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                authService.register(email, "John Doe", "password123", "STUDENT"));

        assertEquals("User already exists with email: " + email, exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Login - Should successfully login and return JWT token")
    void testLoginSuccess() {
        String email = "john@example.com";
        String password = "password123";
        String token = "jwt_token_xyz";

        User user = new User();
        user.setUserId(1L);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(UserRole.STUDENT);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateToken(user)).thenReturn(token);

        String result = authService.login(email, password);

        assertNotNull(result);
        assertEquals(token, result);
        verify(userRepository, times(1)).findByEmail(email);
        verify(jwtTokenProvider, times(1)).generateToken(user);
    }

    @Test
    @DisplayName("Login - Should throw exception if user not found")
    void testLoginUserNotFound() {
        String email = "nonexistent@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                authService.login(email, "password123"));

        assertEquals("User not found with email: " + email, exception.getMessage());
    }

    @Test
    @DisplayName("Login - Should throw exception if password is incorrect")
    void testLoginInvalidPassword() {
        String email = "john@example.com";
        String correctPassword = "password123";
        String wrongPassword = "wrongpassword";

        User user = new User();
        user.setUserId(1L);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(correctPassword));
        user.setRole(UserRole.STUDENT);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                authService.login(email, wrongPassword));

        assertEquals("Invalid password", exception.getMessage());
    }

    @Test
    @DisplayName("ForgotPassword - Should update password hash for existing user")
    void testForgotPasswordSuccess() {
        String email = "john@example.com";
        String newPassword = "newPassword123";

        User user = new User();
        user.setUserId(1L);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("oldPassword"));
        user.setRole(UserRole.STUDENT);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.forgotPassword(email, "oldPassword", newPassword);

        verify(userRepository, times(1)).save(user);
        assertTrue(passwordEncoder.matches(newPassword, user.getPasswordHash()));
    }

    @Test
    @DisplayName("ForgotPassword - Should throw exception when user is not found")
    void testForgotPasswordUserNotFound() {
        String email = "missing@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                authService.forgotPassword(email, "oldPassword", "newPassword123"));

        assertEquals("User not found with email: " + email, exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("ForgotPassword - Should throw exception when old password is incorrect")
    void testForgotPasswordOldPasswordInvalid() {
        String email = "john@example.com";

        User user = new User();
        user.setUserId(1L);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("actualOldPassword"));
        user.setRole(UserRole.STUDENT);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                authService.forgotPassword(email, "wrongOldPassword", "newPassword123"));

        assertEquals("Old password is incorrect", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("GetUserByEmail - Should return user if found")
    void testGetUserByEmailSuccess() {
        String email = "john@example.com";
        User user = new User();
        user.setEmail(email);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        Optional<User> result = authService.getUserByEmail(email);

        assertTrue(result.isPresent());
        assertEquals(email, result.get().getEmail());
    }

    @Test
    @DisplayName("ValidateToken - Should validate token successfully")
    void testValidateTokenSuccess() {
        String token = "valid_jwt_token";
        String userId = "1";

        when(jwtTokenProvider.validateToken(token)).thenReturn(userId);

        String result = authService.validateToken(token);

        assertEquals(userId, result);
        verify(jwtTokenProvider, times(1)).validateToken(token);
    }
}