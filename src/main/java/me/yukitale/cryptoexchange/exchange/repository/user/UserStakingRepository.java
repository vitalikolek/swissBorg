package me.yukitale.cryptoexchange.exchange.repository.user;

import me.yukitale.cryptoexchange.exchange.model.Coin;
import me.yukitale.cryptoexchange.exchange.model.user.UserStaking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserStakingRepository extends JpaRepository<UserStaking, Long> {

    List<UserStaking> findByUserId(long userId);

    Optional<UserStaking> findByIdAndUserId(long id, long userId);

    @Transactional
    void deleteAllByCoin(Coin coin);
}
