package me.yukitale.cryptoexchange.exchange.repository.user;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import me.yukitale.cryptoexchange.exchange.model.user.User;
import me.yukitale.cryptoexchange.exchange.model.user.UserSupportMessage;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface UserSupportMessageRepository extends JpaRepository<UserSupportMessage, Long> {

    long countByUserWorkerId(long workerId);

    @CacheEvict(value = "user_support_messages", key = "#result.user.id")
    @Override
    <T extends UserSupportMessage> T save(T result);

    @Cacheable(value = "user_support_messages", key = "#userId")
    List<UserSupportMessage> findByUserIdOrderByIdDesc(long userId);

    @Transactional
    @CacheEvict(value = "user_support_messages", key = "#userId")
    void deleteAllByUserId(long userId);

    @CacheEvict(value = "user_support_messages", key = "#userId")
    default void deleteByIdAndUserId(long id, long userId) {
        deleteById(id);
    }

    Optional<UserSupportMessage> findByIdAndUserWorkerId(long id, long workerId);

    @Modifying
    @Transactional
    @Query("UPDATE UserSupportMessage m SET m.userViewed = true WHERE m.user.id = :userId")
    @CacheEvict(value = "user_support_messages", key = "#userId")
    void markUserViewedToTrueByUserId(@Param("userId") long userId);

    @Modifying
    @Transactional
    @Query("UPDATE UserSupportMessage  m SET m.supportViewed = true WHERE m.user.id = :userId")
    @CacheEvict(value = "user_support_messages", key = "#userId")
    void markSupportViewedToTrueByUserId(@Param("userId") long userId);

    //dialogs parsing start
    @Query("SELECT message.user, message.created, message.supportViewed, message.userViewed FROM UserSupportMessage message " +
            "ORDER BY message.created DESC")
    List<Object[]> findAllOrderByCreateDesc();
    //dialogs parsing end
}
