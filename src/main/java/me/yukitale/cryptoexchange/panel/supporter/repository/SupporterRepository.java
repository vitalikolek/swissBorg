package me.yukitale.cryptoexchange.panel.supporter.repository;

import me.yukitale.cryptoexchange.panel.supporter.model.Supporter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface SupporterRepository extends JpaRepository<Supporter, Long> {

    Optional<Supporter> findByUserId(long userId);

    @Transactional
    void deleteByUserId(long userId);
}
