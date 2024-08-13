package me.yukitale.cryptoexchange.panel.worker.repository.settings.other;

import me.yukitale.cryptoexchange.panel.worker.model.settings.other.WorkerStakingPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkerStakingPlanRepository extends JpaRepository<WorkerStakingPlan, Long> {

    Optional<WorkerStakingPlan> findByIdAndWorkerId(long id, long workerId);

    List<WorkerStakingPlan> findAllByWorkerId(long workerId);

    void deleteAllByWorkerId(long workerId);

    boolean existsByIdAndWorkerId(long id, long workerId);
}
