package zw.co.zivai.core_backend.exceptions;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ErrorResponse {
    private int status;
    private String error;
    private String message;
    private LocalDateTime timestamp;
}
