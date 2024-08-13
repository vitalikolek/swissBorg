package me.yukitale.cryptoexchange.exchange.repository.user;

import me.yukitale.cryptoexchange.exchange.model.user.UserTransaction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserTransactionRepository extends JpaRepository<UserTransaction, Long> {

    Optional<UserTransaction> findByIdAndUserIdAndUserWorkerId(long id, long userId, long workerId);

    Optional<UserTransaction> findByIdAndUserId(long id, long userId);

    List<UserTransaction> findByTypeOrderByIdDesc(UserTransaction.Type type, Pageable pageable);

    List<UserTransaction> findByTypeAndUserWorkerIdOrderByIdDesc(UserTransaction.Type type, long workerId);

    List<UserTransaction> findByUserId(long userId);

    List<UserTransaction> findByUserIdOrderByIdDesc(long userId);
}
