package me.yukitale.cryptoexchange.exchange.repository.user;

import me.yukitale.cryptoexchange.exchange.model.user.UserBank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserBankRepository extends JpaRepository<UserBank, Long> {

    Optional<UserBank> findByIdAndUserId(long id, long userId);

    List<UserBank> findByUserWorkerIdOrderByIdDesc(long workerId);

    long countByUserId(long userId);
}
