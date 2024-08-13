package me.yukitale.cryptoexchange.exchange.repository;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import me.yukitale.cryptoexchange.exchange.model.Coin;

import java.util.List;
import java.util.Optional;

@Repository
public interface CoinRepository extends JpaRepository<Coin, Long> {

    @Query("SELECT c.id FROM Coin c WHERE c.symbol = :symbol")
    long getCoinIdBySymbol(@Param("symbol") String symbol);

    default Coin findFirst() {
        return findAll().get(0);
    }

    long countByPositionEquals(long position);

    @Override
    @Cacheable(value = "all_coins")
    List<Coin> findAll();

    List<Coin> findAllByOrderByPosition();

    @Override
    @Caching(evict = {
            @CacheEvict(value = "all_coins", allEntries = true),
            @CacheEvict(value = "coins", key = "#result.id"),
            @CacheEvict(value = "coins", key = "#result.symbol")
    })
    <T extends Coin> T save(T coin);

    @Override
    @Caching(evict = {
            @CacheEvict(value = "all_coins", allEntries = true),
            @CacheEvict(value = "coins", keyGenerator = "coinKeyGenerator"),
            @CacheEvict(value = "coins", key = "#id")
    })
    void deleteById(Long id);

    boolean existsBySymbol(String symbol);

    @Cacheable(value = "coins", key = "#symbol")
    Optional<Coin> findBySymbol(String symbol);

    @Cacheable(value = "coins", key = "#id")
    Optional<Coin> findById(long id);

    default Coin findUSDT() {
        return findBySymbol("USDT").orElseThrow(RuntimeException::new);
    }
}
