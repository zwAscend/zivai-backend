package zw.co.zivai.core_backend.dtos.auth;

import java.util.Set;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthUserDto {
    private UUID id;
    private String email;
    private String phoneNumber;
    private String firstName;
    private String lastName;
    private String username;
    private Set<String> roles;
    private boolean isAdmin;
    private boolean isTeacher;
    private String role;
    private String studentId;
}
