package me.yukitale.cryptoexchange.panel.worker.repository.settings.coins;

import me.yukitale.cryptoexchange.panel.worker.model.settings.coins.WorkerDepositCoin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkerDepositCoinRepository extends JpaRepository<WorkerDepositCoin, Long> {

    default List<WorkerDepositCoin> findAllByWorkerId(long workerId) {
        return findAllByWorkerIdOrderByPosition(workerId);
    }

    List<WorkerDepositCoin> findAllByWorkerIdOrderByPosition(long workerId);

    Optional<WorkerDepositCoin> findByIdAndWorkerId(long id, long workerId);

    void deleteAllByWorkerId(long workerId);
}
