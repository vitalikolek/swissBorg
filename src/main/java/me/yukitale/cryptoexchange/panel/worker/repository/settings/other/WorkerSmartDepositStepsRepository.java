package me.yukitale.cryptoexchange.panel.worker.repository.settings.other;

import me.yukitale.cryptoexchange.panel.worker.model.settings.other.WorkerSmartDepositStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkerSmartDepositStepsRepository extends JpaRepository<WorkerSmartDepositStep, Long> {

    Optional<WorkerSmartDepositStep> findByIdAndWorkerId(long id, long workerId);

    List<WorkerSmartDepositStep> findAllByWorkerId(long workerId);

    void deleteAllByWorkerId(long workerId);

    boolean existsByIdAndWorkerId(long id, long workerId);

    long countByWorkerId(long workerId);
}
