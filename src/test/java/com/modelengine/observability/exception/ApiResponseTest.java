package com.modelengine.observability.exception;

import com.modelengine.observability.dto.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

class ApiResponseTest {

    @Test
    void shouldCreateSuccessResponse() {
        ApiResponse<String> response = ApiResponse.success("test data");

        assertEquals("success", response.getStatus());
        assertEquals("test data", response.getData());
        assertNull(response.getErrorCode());
        assertNull(response.getErrorType());
        assertNull(response.getError());
    }

    @Test
    void shouldCreateErrorResponseWithErrorCode() {
        ApiResponse<Void> response = ApiResponse.error(ErrorCode.BAD_REQUEST, "Invalid input");

        assertEquals("error", response.getStatus());
        assertEquals("0108001", response.getErrorCode());
        assertEquals("BadRequest", response.getErrorType());
        assertEquals("Invalid input", response.getError());
        assertNull(response.getData());
    }

    @Test
    void shouldCreateErrorResponseWithNotFoundCode() {
        ApiResponse<Void> response = ApiResponse.error(ErrorCode.NOT_FOUND, "Resource not found");

        assertEquals("error", response.getStatus());
        assertEquals("0108003", response.getErrorCode());
        assertEquals("NotFound", response.getErrorType());
        assertEquals("Resource not found", response.getError());
    }

    @Test
    void shouldHandleNullErrorTypeForSuccess() {
        ApiResponse<String> response = ApiResponse.success("data");

        assertNull(response.getErrorCode());
        assertNull(response.getErrorType());
        assertNull(response.getError());
    }

    @Test
    void shouldCreateErrorWithLegacyStringSignature() {
        ApiResponse<Void> response = ApiResponse.error("CustomError", "Something went wrong");

        assertEquals("error", response.getStatus());
        assertNull(response.getErrorCode());
        assertEquals("CustomError", response.getErrorType());
        assertEquals("Something went wrong", response.getError());
    }

    @Test
    void shouldSerializeToJsonWithErrorCode() throws Exception {
        ApiResponse<Void> response = ApiResponse.error(ErrorCode.INTERNAL_ERROR, "Server error");

        String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(response);

        assertTrue(json.contains("\"errorCode\":\"0108007\""), "JSON should contain errorCode field");
        assertTrue(json.contains("\"status\":\"error\""), "JSON should contain status field");
        assertTrue(json.contains("\"errorType\":\"InternalError\""), "JSON should contain errorType field");
    }

    @Test
    void shouldSerializeSuccessWithoutErrorCode() throws Exception {
        ApiResponse<String> response = ApiResponse.success("ok");

        String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(response);

        assertTrue(json.contains("\"status\":\"success\""), "JSON should contain status field");
        assertTrue(json.contains("\"data\":\"ok\""), "JSON should contain data field");
    }

    @Test
    void shouldMatchHttpStatusMapping() {
        assertEquals(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST.getHttpStatus());
        assertEquals(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED.getHttpStatus());
        assertEquals(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND.getHttpStatus());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR.getHttpStatus());
        assertEquals(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN.getHttpStatus());
    }
}
