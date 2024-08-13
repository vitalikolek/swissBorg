package me.yukitale.cryptoexchange.exchange.repository.user;

import me.yukitale.cryptoexchange.exchange.model.user.UserLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface UserLogRepository extends JpaRepository<UserLog, Long> {

    List<UserLog> findByUserWorkerIdOrderByDateDesc(long workerId, Pageable pageable);

    List<UserLog> findByUserIdOrderByDateDesc(long userId, Pageable pageable);

    List<UserLog> findAllByUserId(long userId);

    List<UserLog> findAllByOrderByIdDesc();

    List<UserLog> findAllByOrderByIdDesc(Pageable pageable);

    List<UserLog> findAllByUserRoleTypeOrderByIdDesc(int roleType, Pageable pageable);

    long countByUserIdAndDateGreaterThan(long userId, Date startDate);
}
