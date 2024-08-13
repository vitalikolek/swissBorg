package me.yukitale.cryptoexchange.panel.worker.repository;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import me.yukitale.cryptoexchange.panel.worker.model.Domain;
import me.yukitale.cryptoexchange.panel.worker.model.Worker;

import java.util.List;
import java.util.Optional;

@Repository
public interface DomainRepository extends JpaRepository<Domain, Long> {

    @CacheEvict(value = "domains", key = "#result.name")
    @Override
    <T extends Domain> T save(T domain);

    @CacheEvict(value = "domains", allEntries = true)
    @Override
    void deleteById(Long id);

    @Query("SELECT d.worker FROM Domain d WHERE d.name = :name")
    Optional<Worker> findWorkerByName(@Param("name") String name);

    @Cacheable(value = "domains", key = "#name")
    Optional<Domain> findByName(String name);

    Optional<Domain> findByIdAndWorkerId(long id, long workerId);

    List<Domain> findAllByWorkerId(long workerId);

    boolean existsByName(String name);

    boolean existsByIdAndWorkerId(long id, long workerId);

    @CacheEvict(value = "domains", allEntries = true)
    void deleteAllByWorkerId(long workerId);

    List<Domain> findByWorkerIdIsNullOrderByIdDesc();

    List<Domain> findByWorkerIdIsNotNullOrderByIdDesc();
}
