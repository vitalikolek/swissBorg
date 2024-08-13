package me.yukitale.cryptoexchange.exchange.repository.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import me.yukitale.cryptoexchange.exchange.model.user.UserEmailConfirm;

import java.util.Optional;

@Repository
public interface UserEmailConfirmRepository extends JpaRepository<UserEmailConfirm, Long> {

    Optional<UserEmailConfirm> findByUserIdAndHash(long userId, String hash);
}
