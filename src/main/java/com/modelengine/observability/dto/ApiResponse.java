package com.modelengine.observability.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.modelengine.observability.exception.ErrorCode;

/**
 * Generic API response wrapper matching OpenAPI spec.
 *
 * @param <T> the type of response data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    private String status;
    private T data;
    private String errorCode;
    private String errorType;
    private String error;

    /**
     * Creates a success response with the given data.
     *
     * @param data the response data
     * @param <T>  the type of response data
     * @return a success ApiResponse
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .status("success")
                .data(data)
                .build();
    }

    /**
     * Creates an error response with the given error type and message.
     *
     * @param errorType the error type
     * @param error     the error message
     * @param <T>       the type of response data
     * @return an error ApiResponse
     */
    public static <T> ApiResponse<T> error(String errorType, String error) {
        return ApiResponse.<T>builder()
                .status("error")
                .errorType(errorType)
                .error(error)
                .build();
    }

    /**
     * Creates an error response with the given ErrorCode and message.
     *
     * @param errorCode the ErrorCode enum value
     * @param error     the error message
     * @param <T>       the type of response data
     * @return an error ApiResponse with errorCode set
     */
    public static <T> ApiResponse<T> error(ErrorCode errorCode, String error) {
        return ApiResponse.<T>builder()
                .status("error")
                .errorCode(errorCode.getCode())
                .errorType(errorCode.getErrorType())
                .error(error)
                .build();
    }
}
