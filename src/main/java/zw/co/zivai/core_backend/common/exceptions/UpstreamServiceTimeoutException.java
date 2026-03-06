package zw.co.zivai.core_backend.common.exceptions;

public class UpstreamServiceTimeoutException extends RuntimeException {
    public UpstreamServiceTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
