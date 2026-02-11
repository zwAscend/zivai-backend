package zw.co.zivai.core_backend.dtos.auth;

import lombok.Data;

@Data
public class LoginRequest {
    private String email;
    private String password;
}
