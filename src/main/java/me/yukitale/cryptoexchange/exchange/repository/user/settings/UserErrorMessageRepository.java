package me.yukitale.cryptoexchange.exchange.repository.user.settings;

import me.yukitale.cryptoexchange.exchange.model.user.settings.UserErrorMessage;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import me.yukitale.cryptoexchange.panel.common.model.ErrorMessage;

import java.util.List;
import java.util.Optional;

//todo: deleteById
@Repository
public interface UserErrorMessageRepository extends JpaRepository<UserErrorMessage, Long> {

    @Caching(evict = {
            @CacheEvict(value = "user_alerts", key = "#result.user.id"),
            @CacheEvict(value = "user_alert_types", key = "#result.user.id + '-' + #result.type.name")
    })
    @Override
    <T extends UserErrorMessage> T save(T result);

    @Cacheable(value = "user_alerts", key = "#userId")
    List<UserErrorMessage> findByUserId(long userId);

    @Cacheable(value = "user_alert_types", key = "#userId + '-' + #type.name")
    Optional<UserErrorMessage> findByUserIdAndType(long userId, ErrorMessage.ErrorMessageType type);
}
