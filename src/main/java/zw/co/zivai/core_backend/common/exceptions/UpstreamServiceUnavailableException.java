package zw.co.zivai.core_backend.common.exceptions;

public class UpstreamServiceUnavailableException extends RuntimeException {
    public UpstreamServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
