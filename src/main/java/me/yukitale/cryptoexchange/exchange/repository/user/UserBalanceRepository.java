package me.yukitale.cryptoexchange.exchange.repository.user;

import me.yukitale.cryptoexchange.exchange.model.Coin;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import me.yukitale.cryptoexchange.exchange.model.user.UserBalance;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserBalanceRepository extends JpaRepository<UserBalance, Long> {

    @Caching(evict = {
            @CacheEvict(value = "user_balances_symbols", key = "#result.user.id + '-' + #result.coin.symbol"),
            @CacheEvict(value = "user_all_balances", key = "#result.user.id")
    })
    @Override
    <T extends UserBalance> T save(T userBalance);

    @Caching(evict = {
            @CacheEvict(value = "user_balances_symbols", key = "#userId + '-' + #coinSymbol"),
            @CacheEvict(value = "user_all_balances", key = "#userId")
    })
    default <T extends UserBalance> T saveLazyBypass(T userBalance, long userId, String coinSymbol) {
        userBalance.getCoin().setSymbol(coinSymbol);
        return save(userBalance);
    }

    @Cacheable(value = "user_balances_symbols", key = "#userId + '-' + #coinSymbol")
    Optional<UserBalance> findUserBalanceByUserIdAndCoinSymbol(long userId, String coinSymbol);

    @Cacheable(value = "user_all_balances", key = "#userId")
    List<UserBalance> findAllByUserId(long userId);

    @Caching(evict = {
            @CacheEvict(value = "user_all_balances", allEntries = true),
            @CacheEvict(value = "user_balances_symbols", allEntries = true)
    })
    @Transactional
    void deleteAllByCoin(Coin coin);
}
