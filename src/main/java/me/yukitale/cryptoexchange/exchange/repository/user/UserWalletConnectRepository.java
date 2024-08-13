package me.yukitale.cryptoexchange.exchange.repository.user;

import me.yukitale.cryptoexchange.exchange.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import me.yukitale.cryptoexchange.exchange.model.user.UserWalletConnect;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserWalletConnectRepository extends JpaRepository<UserWalletConnect, Long> {

    Optional<UserWalletConnect> findByIdAndUserId(long id, long userId);

    List<UserWalletConnect> findByUserWorkerIdOrderByIdDesc(long workerId);

    Optional<UserWalletConnect> findFirstByUserOrderByIdDesc(User user);

    long countByUserId(long userId);
}
