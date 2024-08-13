package me.yukitale.cryptoexchange.exchange.repository.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import me.yukitale.cryptoexchange.exchange.model.user.UserKyc;

import java.util.List;

@Repository
public interface UserKycRepository extends JpaRepository<UserKyc, Long> {

    List<UserKyc> findAllByOrderByIdDesc();

    List<UserKyc> findAllByUserWorkerId(long workerId);
}
