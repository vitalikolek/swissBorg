package me.yukitale.cryptoexchange.panel.worker.repository.settings.other;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import me.yukitale.cryptoexchange.panel.worker.model.settings.other.WorkerTelegramNotification;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkerTelegramNotificationRepository extends JpaRepository<WorkerTelegramNotification, Long> {

    Optional<WorkerTelegramNotification> findByWorkerIdAndType(long workerId, WorkerTelegramNotification.Type type);

    default boolean isEnabled(long workerId, WorkerTelegramNotification.Type type) {
        Optional<WorkerTelegramNotification> telegramNotification = findByWorkerIdAndType(workerId, type);
        return telegramNotification.isPresent() && telegramNotification.get().isEnabled();
    }
    
    List<WorkerTelegramNotification> findAllByWorkerId(long workerId);

    void deleteAllByWorkerId(long workerId);
}
