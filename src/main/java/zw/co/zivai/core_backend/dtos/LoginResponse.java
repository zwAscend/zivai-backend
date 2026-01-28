package zw.co.zivai.core_backend.dtos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {
    private String token;
    private AuthUserDto user;
}
