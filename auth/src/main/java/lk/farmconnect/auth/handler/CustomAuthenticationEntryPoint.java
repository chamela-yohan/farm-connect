package lk.farmconnect.auth.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lk.farmconnect.common.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public CustomAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }


    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        String errorMessage = "Authentication required. Please provide a valid JWT token.";

        if (authException != null && authException.getMessage() != null) {
            String cause = authException.getMessage().toLowerCase();
            if (cause.contains("expired")) {
                errorMessage = "JWT token has expired. Please log in again.";
            } else if (cause.contains("signature") || cause.contains("invalid") || cause.contains("format")) {
                errorMessage = "Invalid or tampered JWT token.";
            }
        }

        ApiResponse<Void> apiResponse = ApiResponse.error(errorMessage);

        // This will successfully serialize LocalDateTime!
        objectMapper.writeValue(response.getOutputStream(), apiResponse);
    }

    private static String getString(AuthenticationException authException) {
        String errorMessage = "Authentication required. Please provide a valid JWT token.";

        if (authException != null && authException.getMessage() != null) {
            String cause = authException.getMessage().toLowerCase();
            if (cause.contains("expired")) {
                errorMessage = "JWT token has expired. Please log in again.";
            } else if (cause.contains("signature") || cause.contains("invalid")) {
                errorMessage = "Invalid or tampered JWT token.";
            }
        }
        return errorMessage;
    }
}