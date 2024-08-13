package me.yukitale.cryptoexchange.panel.worker.repository;

import me.yukitale.cryptoexchange.panel.worker.model.Worker;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkerRepository extends JpaRepository<Worker, Long> {

    Optional<Worker> findByUserId(long userId);

    Optional<Worker> findByUserEmail(String email);

    Optional<Worker> findByUserUsername(String username);

    List<Worker> findByOrderByIdDesc(Pageable pageable);

    @Transactional
    void deleteByUserId(long userId);

    //long findUsersCountByWorkerId(long workerId);
}
