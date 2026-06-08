package lk.farmconnect.common.exception;

import lk.farmconnect.common.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        // collect all field errors into a single message
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(error->error.getField()+": "+error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        ApiResponse<Void> response = ApiResponse.error(errorMessage);
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
        ApiResponse<Void> response = ApiResponse.error("An unexpected error occurred. Please try again later.");
        return ResponseEntity.badRequest().body(response);
    }

}
