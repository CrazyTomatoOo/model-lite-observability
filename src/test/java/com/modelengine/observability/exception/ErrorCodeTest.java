package com.modelengine.observability.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

class ErrorCodeTest {

    @Test
    void shouldMapSuccessCode() {
        assertEquals("0108000", ErrorCode.SUCCESS.getCode());
        assertNull(ErrorCode.SUCCESS.getErrorType());
        assertEquals(HttpStatus.OK, ErrorCode.SUCCESS.getHttpStatus());
    }

    @Test
    void shouldMapBadRequestCode() {
        assertEquals("0108001", ErrorCode.BAD_REQUEST.getCode());
        assertEquals("BadRequest", ErrorCode.BAD_REQUEST.getErrorType());
        assertEquals(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST.getHttpStatus());
    }

    @Test
    void shouldMapUnauthorizedCode() {
        assertEquals("0108002", ErrorCode.UNAUTHORIZED.getCode());
        assertEquals("Unauthorized", ErrorCode.UNAUTHORIZED.getErrorType());
        assertEquals(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED.getHttpStatus());
    }

    @Test
    void shouldMapNotFoundCode() {
        assertEquals("0108003", ErrorCode.NOT_FOUND.getCode());
        assertEquals("NotFound", ErrorCode.NOT_FOUND.getErrorType());
        assertEquals(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND.getHttpStatus());
    }

    @Test
    void shouldMapMetricsQueryFailedCode() {
        assertEquals("0108004", ErrorCode.METRICS_QUERY_FAILED.getCode());
        assertEquals("MetricsQueryFailed", ErrorCode.METRICS_QUERY_FAILED.getErrorType());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.METRICS_QUERY_FAILED.getHttpStatus());
    }

    @Test
    void shouldMapFrameworkUnsupportedCode() {
        assertEquals("0108005", ErrorCode.FRAMEWORK_UNSUPPORTED.getCode());
        assertEquals("FrameworkUnsupported", ErrorCode.FRAMEWORK_UNSUPPORTED.getErrorType());
        assertEquals(HttpStatus.BAD_REQUEST, ErrorCode.FRAMEWORK_UNSUPPORTED.getHttpStatus());
    }

    @Test
    void shouldMapForbiddenCode() {
        assertEquals("0108006", ErrorCode.FORBIDDEN.getCode());
        assertEquals("Forbidden", ErrorCode.FORBIDDEN.getErrorType());
        assertEquals(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN.getHttpStatus());
    }

    @Test
    void shouldMapInternalErrorCode() {
        assertEquals("0108007", ErrorCode.INTERNAL_ERROR.getCode());
        assertEquals("InternalError", ErrorCode.INTERNAL_ERROR.getErrorType());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR.getHttpStatus());
    }

    @Test
    void shouldResolveFromCode() {
        assertEquals(ErrorCode.SUCCESS, ErrorCode.fromCode("0108000"));
        assertEquals(ErrorCode.BAD_REQUEST, ErrorCode.fromCode("0108001"));
        assertEquals(ErrorCode.UNAUTHORIZED, ErrorCode.fromCode("0108002"));
        assertEquals(ErrorCode.NOT_FOUND, ErrorCode.fromCode("0108003"));
        assertEquals(ErrorCode.METRICS_QUERY_FAILED, ErrorCode.fromCode("0108004"));
        assertEquals(ErrorCode.FRAMEWORK_UNSUPPORTED, ErrorCode.fromCode("0108005"));
        assertEquals(ErrorCode.FORBIDDEN, ErrorCode.fromCode("0108006"));
        assertEquals(ErrorCode.INTERNAL_ERROR, ErrorCode.fromCode("0108007"));
    }

    @Test
    void shouldReturnInternalErrorForUnknownCode() {
        assertEquals(ErrorCode.INTERNAL_ERROR, ErrorCode.fromCode("9999999"));
    }

    @Test
    void shouldHaveEightEnumValues() {
        assertEquals(8, ErrorCode.values().length);
    }
}
