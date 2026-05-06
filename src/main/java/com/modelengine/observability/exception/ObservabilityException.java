package com.modelengine.observability.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Domain exception carrying an ErrorCode for standardized error responses.
 */
@Getter
public class ObservabilityException extends RuntimeException {

    private final ErrorCode errorCode;

    public ObservabilityException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ObservabilityException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorType() {
        return errorCode.getErrorType();
    }

    public HttpStatus getHttpStatus() {
        return errorCode.getHttpStatus();
    }

    // --- Convenience factory methods ---

    public static ObservabilityException badRequest(String message) {
        return new ObservabilityException(ErrorCode.BAD_REQUEST, message);
    }

    public static ObservabilityException notFound(String message) {
        return new ObservabilityException(ErrorCode.NOT_FOUND, message);
    }

    public static ObservabilityException unauthorized(String message) {
        return new ObservabilityException(ErrorCode.UNAUTHORIZED, message);
    }

    public static ObservabilityException metricsQueryFailed(String message) {
        return new ObservabilityException(ErrorCode.METRICS_QUERY_FAILED, message);
    }

    public static ObservabilityException frameworkUnsupported(String message) {
        return new ObservabilityException(ErrorCode.FRAMEWORK_UNSUPPORTED, message);
    }

    public static ObservabilityException internalError(String message) {
        return new ObservabilityException(ErrorCode.INTERNAL_ERROR, message);
    }
}
