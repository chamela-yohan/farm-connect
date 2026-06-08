package lk.farmconnect.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

// @JsonInclude ensures that 'data' or 'message' don't show up as "null" in JSON if they are empty
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        LocalDateTime timestamp
) {
    // Factory method for success with data
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<T>(true, null, data, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(true, message, null, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null, LocalDateTime.now());
    }

}
