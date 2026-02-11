package zw.co.zivai.core_backend.services.auth;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.dtos.auth.AuthUserDto;
import zw.co.zivai.core_backend.dtos.auth.LoginRequest;
import zw.co.zivai.core_backend.dtos.auth.LoginResponse;
import zw.co.zivai.core_backend.exceptions.BadRequestException;
import zw.co.zivai.core_backend.models.lms.User;
import zw.co.zivai.core_backend.repositories.user.UserRepository;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public LoginResponse login(LoginRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank()
            || request.getPassword() == null || request.getPassword().isBlank()) {
            throw new BadRequestException("Email and password are required");
        }

        User user = userRepository.findByEmailAndDeletedAtIsNull(request.getEmail().trim().toLowerCase())
            .orElseThrow(() -> new BadRequestException("Invalid credentials"));

        if (!user.isActive()) {
            throw new BadRequestException("Account is inactive");
        }

        String passwordHash = user.getPasswordHash();
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new BadRequestException("Account has no password set");
        }

        if (!passwordEncoder.matches(request.getPassword(), passwordHash)) {
            throw new BadRequestException("Invalid credentials");
        }

        Set<String> roles = user.getRoles().stream()
            .map(role -> role.getCode())
            .collect(Collectors.toSet());

        boolean isTeacher = roles.contains("teacher");
        boolean isAdmin = roles.contains("admin");
        boolean isStudent = roles.contains("student");

        AuthUserDto userDto = AuthUserDto.builder()
            .id(user.getId())
            .email(user.getEmail())
            .phoneNumber(user.getPhoneNumber())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .username(user.getUsername())
            .roles(roles)
            .isAdmin(isAdmin)
            .isTeacher(isTeacher)
            .role(isStudent ? "student" : (isTeacher ? "teacher" : (isAdmin ? "admin" : null)))
            .studentId(isStudent ? user.getId().toString() : null)
            .build();

        return LoginResponse.builder()
            .token(generateToken())
            .user(userDto)
            .build();
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
