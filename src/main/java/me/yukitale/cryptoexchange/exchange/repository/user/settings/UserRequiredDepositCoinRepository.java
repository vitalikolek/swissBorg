package me.yukitale.cryptoexchange.exchange.repository.user.settings;

import me.yukitale.cryptoexchange.exchange.model.user.settings.UserRequiredDepositCoin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRequiredDepositCoinRepository extends JpaRepository<UserRequiredDepositCoin, Long> {

    boolean existsByUserIdAndDepositCoinIdAndDepositCoinWorkerId(long userId, long depositCoinId, long workerId);

    boolean existsByIdAndUserIdAndDepositCoinWorkerId(long userId, long depositCoinId, long workerId);

    void deleteAllByDepositCoinWorkerId(long workerId);

    List<UserRequiredDepositCoin> findByUserId(long userId);
}
