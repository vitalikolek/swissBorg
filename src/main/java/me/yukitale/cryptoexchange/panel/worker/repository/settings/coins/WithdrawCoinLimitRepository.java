package me.yukitale.cryptoexchange.panel.worker.repository.settings.coins;

import me.yukitale.cryptoexchange.exchange.model.Coin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import me.yukitale.cryptoexchange.panel.worker.model.settings.coins.WithdrawCoinLimit;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface WithdrawCoinLimitRepository extends JpaRepository<WithdrawCoinLimit, Long> {

    List<WithdrawCoinLimit> findAllByWorkerId(long workerId);

    Optional<WithdrawCoinLimit> findByWorkerIdAndCoinId(long workerId, long coinId);

    boolean existsByIdAndWorkerId(long id, long workerId);

    @Transactional
    void deleteAllByWorkerId(long workerId);

    @Transactional
    void deleteAllByCoin(Coin coin);
}
