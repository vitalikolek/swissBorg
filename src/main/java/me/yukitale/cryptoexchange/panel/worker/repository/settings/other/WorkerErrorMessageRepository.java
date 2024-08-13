package me.yukitale.cryptoexchange.panel.worker.repository.settings.other;

import me.yukitale.cryptoexchange.panel.common.model.ErrorMessage;
import me.yukitale.cryptoexchange.panel.worker.model.settings.other.WorkerErrorMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkerErrorMessageRepository extends JpaRepository<WorkerErrorMessage, Long> {

    Optional<WorkerErrorMessage> findByWorkerIdAndType(long workerId, ErrorMessage.ErrorMessageType type);

    List<WorkerErrorMessage> findAllByWorkerId(long id);

    void deleteAllByWorkerId(long workerId);
}
