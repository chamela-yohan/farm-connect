package lk.farmconnect.auth.repository;

import lk.farmconnect.auth.entity.RefreshToken;
import lk.farmconnect.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByToken(String refreshToken);

    Optional<RefreshToken> findByUser(User user);

    @Modifying
    int deleteByUser(User user);

}
