package me.yukitale.cryptoexchange.exchange.repository.user;

import me.yukitale.cryptoexchange.exchange.model.Coin;
import me.yukitale.cryptoexchange.exchange.model.user.UserAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

//todo: caching
@Repository
public interface UserAlertRepository extends JpaRepository<UserAlert, Long> {

    Optional<UserAlert> findFirstByUserId(long userId);

    @Transactional
    void deleteAllByCoin(Coin coin);
}
