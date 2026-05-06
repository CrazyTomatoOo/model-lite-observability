package com.modelengine.observability.exception;

import com.modelengine.observability.dto.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldHandleObservabilityExceptionWithBadRequest() {
        ObservabilityException ex = new ObservabilityException(ErrorCode.BAD_REQUEST, "Invalid input");

        ResponseEntity<ApiResponse<Void>> response = handler.handleObservabilityException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ApiResponse<Void> body = response.getBody();
        assertNotNull(body);
        assertEquals("error", body.getStatus());
        assertEquals("0108001", body.getErrorCode());
        assertEquals("BadRequest", body.getErrorType());
        assertEquals("Invalid input", body.getError());
    }

    @Test
    void shouldHandleObservabilityExceptionWithNotFound() {
        ObservabilityException ex = new ObservabilityException(ErrorCode.NOT_FOUND, "Resource not found");

        ResponseEntity<ApiResponse<Void>> response = handler.handleObservabilityException(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        ApiResponse<Void> body = response.getBody();
        assertNotNull(body);
        assertEquals("0108003", body.getErrorCode());
        assertEquals("NotFound", body.getErrorType());
    }

    @Test
    void shouldHandleObservabilityExceptionWithInternalError() {
        ObservabilityException ex = ObservabilityException.internalError("Something broke");

        ResponseEntity<ApiResponse<Void>> response = handler.handleObservabilityException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        ApiResponse<Void> body = response.getBody();
        assertNotNull(body);
        assertEquals("0108007", body.getErrorCode());
        assertEquals("InternalError", body.getErrorType());
        assertEquals("Something broke", body.getError());
    }

    @Test
    void shouldHandleObservabilityExceptionWithMetricsQueryFailed() {
        ObservabilityException ex = ObservabilityException.metricsQueryFailed("Prometheus timeout");

        ResponseEntity<ApiResponse<Void>> response = handler.handleObservabilityException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        ApiResponse<Void> body = response.getBody();
        assertNotNull(body);
        assertEquals("0108004", body.getErrorCode());
        assertEquals("MetricsQueryFailed", body.getErrorType());
    }

    @Test
    void shouldHandleObservabilityExceptionWithUnauthorized() {
        ObservabilityException ex = ObservabilityException.unauthorized("Token expired");

        ResponseEntity<ApiResponse<Void>> response = handler.handleObservabilityException(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        ApiResponse<Void> body = response.getBody();
        assertNotNull(body);
        assertEquals("0108002", body.getErrorCode());
        assertEquals("Unauthorized", body.getErrorType());
    }

    @Test
    void shouldHandleObservabilityExceptionWithFrameworkUnsupported() {
        ObservabilityException ex = ObservabilityException.frameworkUnsupported("Unsupported framework");

        ResponseEntity<ApiResponse<Void>> response = handler.handleObservabilityException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ApiResponse<Void> body = response.getBody();
        assertNotNull(body);
        assertEquals("0108005", body.getErrorCode());
        assertEquals("FrameworkUnsupported", body.getErrorType());
    }

    @Test
    void shouldHandleGenericException() {
        Exception ex = new RuntimeException("Unexpected");

        ResponseEntity<ApiResponse<Void>> response = handler.handleGenericException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        ApiResponse<Void> body = response.getBody();
        assertNotNull(body);
        assertEquals("0108007", body.getErrorCode());
        assertEquals("InternalError", body.getErrorType());
        assertEquals("An unexpected error occurred", body.getError());
    }

    @Test
    void shouldHandleMethodNotSupported() {
        HttpRequestMethodNotSupportedException ex =
                new HttpRequestMethodNotSupportedException("POST");

        ResponseEntity<ApiResponse<Void>> response = handler.handleMethodNotSupported(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        ApiResponse<Void> body = response.getBody();
        assertNotNull(body);
        assertEquals("0108003", body.getErrorCode());
        assertEquals("NotFound", body.getErrorType());
    }
}
