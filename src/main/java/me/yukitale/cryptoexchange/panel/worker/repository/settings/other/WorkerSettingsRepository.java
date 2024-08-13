package me.yukitale.cryptoexchange.panel.worker.repository.settings.other;

import me.yukitale.cryptoexchange.exchange.model.Coin;
import me.yukitale.cryptoexchange.panel.worker.model.settings.other.WorkerSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface WorkerSettingsRepository extends JpaRepository<WorkerSettings, Long> {

    Optional<WorkerSettings> findByWorkerId(long workerId);

    void deleteByWorkerId(long workerId);

    @Modifying
    @Transactional
    @Query("UPDATE WorkerSettings SET bonusCoin = NULL, bonusAmount = 0 WHERE bonusCoin = :coin")
    void deleteAllByCoin(@Param("coin") Coin coin);
}
