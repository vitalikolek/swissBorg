package me.yukitale.cryptoexchange.panel.admin.repository.coins;

import me.yukitale.cryptoexchange.panel.admin.model.coins.AdminDepositCoin;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import me.yukitale.cryptoexchange.common.types.CoinType;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdminDepositCoinRepository extends JpaRepository<AdminDepositCoin, Long> {

    @Override
    default List<AdminDepositCoin> findAll() {
        return findByOrderByPosition();
    }

    @Cacheable(value = "admin_deposit_coins")
    List<AdminDepositCoin> findByOrderByPosition();

    @Caching(evict = {
            @CacheEvict(value = "admin_deposit_coin_types", allEntries = true),
            @CacheEvict(value = "admin_deposit_coins", allEntries = true)
    })
    @Override
    <T extends AdminDepositCoin> T save(T value);

    @Cacheable(value = "admin_deposit_coin_types", key = "#type")
    Optional<AdminDepositCoin> findByType(CoinType type);
}
