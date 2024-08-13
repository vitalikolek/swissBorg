package me.yukitale.cryptoexchange.panel.worker.repository.settings.coins;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import me.yukitale.cryptoexchange.panel.worker.model.settings.other.WorkerCoinSettings;

import java.util.Optional;

@Repository
public interface WorkerCoinSettingsRepository extends JpaRepository<WorkerCoinSettings, Long> {

    Optional<WorkerCoinSettings> findByWorkerId(long workerId);
    
    void deleteAllByWorkerId(long workerId);
}
