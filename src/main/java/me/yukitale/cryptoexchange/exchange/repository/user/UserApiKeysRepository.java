package me.yukitale.cryptoexchange.exchange.repository.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import me.yukitale.cryptoexchange.exchange.model.user.UserApiKey;

import java.util.List;

@Repository
public interface UserApiKeysRepository extends JpaRepository<UserApiKey, Long> {

    List<UserApiKey> findAllByUserId(long userId);
}
