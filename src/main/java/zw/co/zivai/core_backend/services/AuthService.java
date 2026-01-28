package zw.co.zivai.core_backend.services;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.dtos.AuthUserDto;
import zw.co.zivai.core_backend.dtos.LoginRequest;
import zw.co.zivai.core_backend.dtos.LoginResponse;
import zw.co.zivai.core_backend.exceptions.BadRequestException;
import zw.co.zivai.core_backend.models.lms.User;
import zw.co.zivai.core_backend.repositories.UserRepository;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;

    // Temporary embedded credentials (to be replaced by real auth)
    private static final Map<String, String> PASSWORDS = Map.of(
        "teacher@zivai.local", "TempPass123!",
        "student@zivai.local", "TempPass123!",
        "admin@zivai.local", "TempPass123!"
    );

    public LoginResponse login(LoginRequest request) {
        if (request.getEmail() == null || request.getPassword() == null) {
            throw new BadRequestException("Email and password are required");
        }

        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new BadRequestException("Invalid credentials"));

        String expected = PASSWORDS.get(request.getEmail());
        if (expected == null || !expected.equals(request.getPassword())) {
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
            .token("dev-token")
            .user(userDto)
            .build();
    }
}
