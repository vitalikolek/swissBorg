package me.yukitale.cryptoexchange.panel.worker.repository;

import me.yukitale.cryptoexchange.exchange.model.Coin;
import me.yukitale.cryptoexchange.panel.worker.model.Promocode;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface PromocodeRepository extends JpaRepository<Promocode, Long> {

    Optional<Promocode> findByIdAndWorkerId(long id, long workerId);

    List<Promocode> findByOrderByIdDesc(Pageable pageable);

    List<Promocode> findByWorkerId(long workerId);

    Optional<Promocode> findByNameIgnoreCase(String name);

    Optional<Promocode> findByNameIgnoreCaseAndWorkerId(String name, long workerId);

    Optional<Promocode> findByName(String name);

    boolean existsByIdAndWorkerId(long id, long workerId);

    boolean existsByName(String name);

    boolean existsByNameIgnoreCase(String name);

    long countByWorkerId(long workerId);

    void deleteAllByWorkerId(long workerId);

    @Transactional
    void deleteAllByCoin(Coin coin);
}
