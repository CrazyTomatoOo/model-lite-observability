package com.modelengine.observability.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Standardized error codes for the Observability API.
 * Each code maps to a specific error type and HTTP status.
 */
@Getter
public enum ErrorCode {

    SUCCESS("0108000", null, HttpStatus.OK),
    BAD_REQUEST("0108001", "BadRequest", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED("0108002", "Unauthorized", HttpStatus.UNAUTHORIZED),
    NOT_FOUND("0108003", "NotFound", HttpStatus.NOT_FOUND),
    METRICS_QUERY_FAILED("0108004", "MetricsQueryFailed", HttpStatus.INTERNAL_SERVER_ERROR),
    FRAMEWORK_UNSUPPORTED("0108005", "FrameworkUnsupported", HttpStatus.BAD_REQUEST),
    FORBIDDEN("0108006", "Forbidden", HttpStatus.FORBIDDEN),
    INTERNAL_ERROR("0108007", "InternalError", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String errorType;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String errorType, HttpStatus httpStatus) {
        this.code = code;
        this.errorType = errorType;
        this.httpStatus = httpStatus;
    }

    /**
     * Resolves an error code string to the corresponding ErrorCode enum.
     *
     * @param code the error code string (e.g. "0108001")
     * @return the matching ErrorCode, or INTERNAL_ERROR if not found
     */
    public static ErrorCode fromCode(String code) {
        for (ErrorCode ec : values()) {
            if (ec.code.equals(code)) {
                return ec;
            }
        }
        return INTERNAL_ERROR;
    }
}
