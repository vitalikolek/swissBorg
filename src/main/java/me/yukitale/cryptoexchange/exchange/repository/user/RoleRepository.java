package me.yukitale.cryptoexchange.exchange.repository.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import me.yukitale.cryptoexchange.exchange.model.user.UserRole;
import me.yukitale.cryptoexchange.exchange.model.user.UserRoleType;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<UserRole, Long> {

  Optional<UserRole> findByName(UserRoleType name);

  Boolean existsByName(UserRoleType name);
}
