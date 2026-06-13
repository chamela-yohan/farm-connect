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

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        // Set HTTP Status to 401
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        // Determine the best message based on the exception
        String errorMessage = getString(authException);

        // Write our standard ApiResponse to the output stream
        ApiResponse<Void> apiResponse = ApiResponse.error(errorMessage);
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