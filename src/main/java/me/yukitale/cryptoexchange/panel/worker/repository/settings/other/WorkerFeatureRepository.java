package me.yukitale.cryptoexchange.panel.worker.repository.settings.other;

import me.yukitale.cryptoexchange.panel.worker.model.settings.other.WorkerFeature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkerFeatureRepository extends JpaRepository<WorkerFeature, Long> {

    List<WorkerFeature> findAllByWorkerId(long workerId);

    void deleteAllByWorkerId(long workerId);
}
