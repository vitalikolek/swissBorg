package me.yukitale.cryptoexchange.exchange.repository.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import me.yukitale.cryptoexchange.common.types.CoinType;
import me.yukitale.cryptoexchange.exchange.model.user.UserAddress;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {

    long countByUserWorkerId(long workerId);

    List<UserAddress> findByUserId(long userId);

    Optional<UserAddress> findByAddressAndTagIgnoreCase(String address, String tag);

    Optional<UserAddress> findByAddressIgnoreCase(String address);

    Optional<UserAddress> findByUserIdAndCoinType(long userId, CoinType coinType);

    long countByUserIdAndCoinType(long userId, CoinType coinType);
}
