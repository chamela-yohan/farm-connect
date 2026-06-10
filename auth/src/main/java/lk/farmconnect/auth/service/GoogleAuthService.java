package lk.farmconnect.auth.service;


import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import lk.farmconnect.user.User;
import lk.farmconnect.user.UserRepository;
import lk.farmconnect.user.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    @Value("${app.google.client-id}")
    private String googleClientId;

    @Transactional
    public User authenticateWithGoogle(String idTokenString, UserRepository userRepository) {
        try {
            GoogleIdTokenVerifier verifier =
                    new GoogleIdTokenVerifier.Builder(
                            new NetHttpTransport(),
                            GsonFactory.getDefaultInstance())
                            .setAudience(Collections.singletonList(googleClientId))
                            .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);

            if (idToken == null) {
                log.error("Invalid Google Id token");
                throw new RuntimeException("Invalid Google Id token");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String name = (String) payload.get("name");
            String pictureUrl = (String) payload.get("picture");

            log.info("Google auth successful for email: {}", email);

            return userRepository.findByEmail(email).orElseGet(() -> {
                log.info("Creating new user from Google auth: {}", email);
                User newUser = User.builder()
                        .email(email)
                        .name(name != null ? name : email.split("@")[0])
                        .profilePictureUrl(pictureUrl)
                        .role(UserRole.BUYER)
                        .mobileNumber("")
                        .build();
                return userRepository.save(newUser);
            });

        } catch (Exception e) {
            log.error("Google authentication failed", e);
            throw new RuntimeException("Google authentication failed: " + e.getMessage());
        }
    }


}
