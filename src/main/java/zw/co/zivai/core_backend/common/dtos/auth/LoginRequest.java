package zw.co.zivai.core_backend.common.dtos.auth;

import lombok.Data;

@Data
public class LoginRequest {
    private String email;
    private String password;
}
