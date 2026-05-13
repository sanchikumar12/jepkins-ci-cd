package com.edulearn.auth.service;



import com.edulearn.auth.dto.AuthResponse;
import com.edulearn.auth.entity.User;
import com.edulearn.auth.entity.UserRole;
import com.edulearn.auth.repository.UserRepository;
import com.edulearn.auth.util.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthServiceImpl implements AuthService {
    private static final String GOOGLE_PROVIDER = "google";
    private static final SecureRandom OTP_RANDOM = new SecureRandom();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtDecoder googleJwtDecoder;

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.security.oauth2.client.registration.google.client-id:}")
    private String googleClientId;

    @Value("${spring.mail.username:}")
    private String mailFrom;

    @Value("${password.reset.otp.expiration-minutes:10}")
    private long otpExpirationMinutes;

    private final Map<String, PasswordResetOtp> passwordResetOtps = new ConcurrentHashMap<>();

    @Override
    public User register(String email, String fullName, String password, String role) {
        // Check if user already exists
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("User already exists with email: " + email);
        }

        // Create new user
        User user = new User();
        user.setEmail(email);
        user.setFullName(fullName);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(UserRole.valueOf(role.toUpperCase()));
        user.setProvider("local");

        return userRepository.save(user);
    }

    @Override
    public String login(String email, String password) {
        Optional<User> user = userRepository.findByEmail(email);

        if (user.isEmpty()) {
            throw new RuntimeException("User not found with email: " + email);
        }

        // Verify password
        if (!passwordEncoder.matches(password, user.get().getPasswordHash())) {
            throw new RuntimeException("Invalid password");
        }

        // Generate JWT token
        return jwtTokenProvider.generateToken(user.get());
    }

    @Override
    public void forgotPassword(String email, String oldPassword, String newPassword) {
        if (email == null || email.isBlank()) {
            throw new RuntimeException("Email is required");
        }
        if (oldPassword == null || oldPassword.isBlank()) {
            throw new RuntimeException("Old password is required");
        }
        if (newPassword == null || newPassword.isBlank()) {
            throw new RuntimeException("New password is required");
        }
        if (newPassword.length() < 6) {
            throw new RuntimeException("New password must be at least 6 characters");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new RuntimeException("Old password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    public void sendPasswordResetOtp(String email) {
        String normalizedEmail = normalizeEmail(email);
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + normalizedEmail));

        String otp = String.format("%06d", OTP_RANDOM.nextInt(1_000_000));
        passwordResetOtps.put(normalizedEmail, new PasswordResetOtp(otp, Instant.now().plusSeconds(otpExpirationMinutes * 60)));
        sendOtpEmail(user, otp);
    }

    @Override
    public void verifyPasswordResetOtp(String email, String otp) {
        String normalizedEmail = normalizeEmail(email);
        PasswordResetOtp resetOtp = getValidPasswordResetOtp(normalizedEmail, otp);
        resetOtp.setVerified(true);
    }

    @Override
    public void resetPasswordWithOtp(String email, String otp, String newPassword) {
        String normalizedEmail = normalizeEmail(email);
        PasswordResetOtp resetOtp = getValidPasswordResetOtp(normalizedEmail, otp);
        if (!resetOtp.isVerified()) {
            throw new RuntimeException("Please verify OTP before resetting password");
        }
        if (newPassword == null || newPassword.isBlank()) {
            throw new RuntimeException("New password is required");
        }
        if (newPassword.length() < 6) {
            throw new RuntimeException("New password must be at least 6 characters");
        }

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + normalizedEmail));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        passwordResetOtps.remove(normalizedEmail);
    }

    @Override
    public AuthResponse authenticateWithGoogle(String googleIdToken, String role) {
        Jwt jwt = decodeGoogleToken(googleIdToken);
        validateGoogleAudience(jwt);

        String email = jwt.getClaimAsString("email");
        Boolean emailVerified = jwt.getClaim("email_verified");
        if (email == null || email.isBlank() || !Boolean.TRUE.equals(emailVerified)) {
            throw new RuntimeException("Google account email is not verified.");
        }

        String fullName = jwt.getClaimAsString("name");
        if (fullName == null || fullName.isBlank()) {
            fullName = email.substring(0, email.indexOf('@'));
        }

        User user;
        boolean isNewUser = false;
        Optional<User> existingUser = userRepository.findByEmail(email);
        if (existingUser.isPresent()) {
            user = existingUser.get();
            if (user.getProvider() == null || user.getProvider().isBlank()) {
                user.setProvider(GOOGLE_PROVIDER);
                userRepository.save(user);
            }
        } else {
            if (role == null || role.isBlank()) {
                throw new RuntimeException("No account found for this Google email. Please register first.");
            }
            user = new User();
            user.setEmail(email);
            user.setFullName(fullName);
            user.setRole(resolveRole(role));
            user.setProvider(GOOGLE_PROVIDER);
            // Keep a random password hash to satisfy non-null schema for OAuth users.
            user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
            user = userRepository.save(user);
            isNewUser = true;
        }

        String token = jwtTokenProvider.generateToken(user);
        AuthResponse response = new AuthResponse();
        response.setSuccess(true);
        response.setMessage(isNewUser ? "Google registration successful" : "Google login successful");
        response.setToken(token);
        response.setUserId(user.getUserId());
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setRole(user.getRole().toString());
        response.setMobile(user.getMobile());
        response.setBio(user.getBio());
        return response;
    }

    @Override
    public void logout(String token) {
        // In a real app, you might add the token to a blacklist
        System.out.println("User logged out. Token invalidated: " + token);
    }

    @Override
    public String validateToken(String token) {
        return jwtTokenProvider.validateToken(token);
    }

    @Override
    public String refreshToken(String token) {
        // Validate current token first
        String userId = jwtTokenProvider.validateToken(token);

        Optional<User> user = userRepository.findByUserId(Long.parseLong(userId));
        if (user.isEmpty()) {
            throw new RuntimeException("User not found");
        }

        // Generate new token
        return jwtTokenProvider.generateToken(user.get());
    }

    @Override
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public Optional<User> getUserById(Long userId) {
        return userRepository.findByUserId(userId);
    }

    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        Optional<User> user = userRepository.findByUserId(userId);

        if (user.isEmpty()) {
            throw new RuntimeException("User not found");
        }

        // Verify old password
        if (!passwordEncoder.matches(oldPassword, user.get().getPasswordHash())) {
            throw new RuntimeException("Old password is incorrect");
        }

        // Update password
        user.get().setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user.get());
    }

    @Override
    public User updateProfile(Long userId, String fullName, String bio, String mobile) {
        Optional<User> user = userRepository.findByUserId(userId);

        if (user.isEmpty()) {
            throw new RuntimeException("User not found");
        }

        User updatedUser = user.get();
        updatedUser.setFullName(fullName);
        updatedUser.setBio(bio);
        updatedUser.setMobile(mobile);

        return userRepository.save(updatedUser);
    }

    @Override
    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("User not found");
        }
        userRepository.deleteById(userId);
    }

	@Override
	public List<User> getAllUser() {
		// TODO Auto-generated method stub
		return userRepository.findAll();
	}

	@Override
	public List<User> findAllByRole(UserRole role) {
		// TODO Auto-generated method stub
		return  userRepository.findAllByRole(role) ;
	}

    private Jwt decodeGoogleToken(String token) {
        try {
            return googleJwtDecoder.decode(token);
        } catch (JwtException ex) {
            throw new RuntimeException("Invalid Google token.");
        }
    }

    private void validateGoogleAudience(Jwt jwt) {
        if (googleClientId == null || googleClientId.isBlank()) {
            throw new RuntimeException("Google client ID is not configured on server.");
        }

        Object audienceClaim = jwt.getClaims().get("aud");
        boolean audienceMatched = false;

        if (audienceClaim instanceof String audience) {
            audienceMatched = googleClientId.equals(audience);
        } else if (audienceClaim instanceof Collection<?> audiences) {
            audienceMatched = audiences.stream().anyMatch(a -> googleClientId.equals(String.valueOf(a)));
        }

        if (!audienceMatched) {
            throw new RuntimeException("Google token audience mismatch.");
        }
    }

    private UserRole resolveRole(String role) {
        if (role == null || role.isBlank()) {
            return UserRole.STUDENT;
        }
        try {
            return UserRole.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("Invalid role: " + role);
        }
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new RuntimeException("Email is required");
        }
        return email.trim().toLowerCase();
    }

    private PasswordResetOtp getValidPasswordResetOtp(String email, String otp) {
        if (otp == null || otp.isBlank()) {
            throw new RuntimeException("OTP is required");
        }

        PasswordResetOtp resetOtp = passwordResetOtps.get(email);
        if (resetOtp == null) {
            throw new RuntimeException("OTP not found or expired. Please request a new OTP.");
        }
        if (Instant.now().isAfter(resetOtp.getExpiresAt())) {
            passwordResetOtps.remove(email);
            throw new RuntimeException("OTP expired. Please request a new OTP.");
        }
        if (!resetOtp.getOtp().equals(otp.trim())) {
            throw new RuntimeException("Invalid OTP");
        }
        return resetOtp;
    }

    private void sendOtpEmail(User user, String otp) {
        if (mailFrom == null || mailFrom.isBlank()) {
            throw new RuntimeException("Gmail sender is not configured. Set MAIL_USERNAME and MAIL_PASSWORD.");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(user.getEmail());
        message.setSubject("EduLearn Password Reset OTP");
        message.setText("Hi " + user.getFullName() + ",\n\n"
                + "Your EduLearn password reset OTP is: " + otp + "\n\n"
                + "This OTP will expire in " + otpExpirationMinutes + " minutes.\n"
                + "If you did not request this, please ignore this email.\n\n"
                + "EduLearn Team");

        try {
            mailSender.send(message);
        } catch (MailException ex) {
            passwordResetOtps.remove(user.getEmail().toLowerCase());
            throw new RuntimeException("Could not send OTP email. Please check Gmail SMTP configuration.");
        }
    }

    private static class PasswordResetOtp {
        private final String otp;
        private final Instant expiresAt;
        private boolean verified;

        private PasswordResetOtp(String otp, Instant expiresAt) {
            this.otp = otp;
            this.expiresAt = expiresAt;
        }

        private String getOtp() {
            return otp;
        }

        private Instant getExpiresAt() {
            return expiresAt;
        }

        private boolean isVerified() {
            return verified;
        }

        private void setVerified(boolean verified) {
            this.verified = verified;
        }
    }
}
