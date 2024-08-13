package me.yukitale.cryptoexchange.exchange.repository.user;

import me.yukitale.cryptoexchange.exchange.model.user.UserSupportDialog;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSupportDialogRepository extends JpaRepository<UserSupportDialog, Long>, JpaSpecificationExecutor<UserSupportDialog> {

    long countByUserWorkerId(long workerId);

    @Cacheable(value = "user_support_dialogs", key = "#userId")
    Optional<UserSupportDialog> findByUserId(long userId);

    Optional<UserSupportDialog> findByUserEmail(String email);

    @CachePut(value = "user_support_dialogs", key = "#result.user.id")
    @Override
    <T extends UserSupportDialog> T save(T result);

    @Transactional
    @CacheEvict(value = "user_support_dialogs", key = "#userId")
    void deleteByUserId(long userId);

    List<UserSupportDialog> findByOnlyWelcomeAndUserWorkerIdOrderByLastMessageDateDesc(boolean onlyWelcome, long workerId, Pageable pageable);

    List<UserSupportDialog> findByOnlyWelcomeOrderByLastMessageDateDesc(boolean onlyWelcome, Pageable pageable);

    List<UserSupportDialog> findByOnlyWelcomeAndUserWorkerIdAndSupportUnviewedMessagesGreaterThanOrderByLastMessageDateDesc(boolean onlyWelcome, long workerId, int supportUnviewedMessages, Pageable pageable);

    List<UserSupportDialog> findByOnlyWelcomeAndSupportUnviewedMessagesGreaterThanOrderByLastMessageDateDesc(boolean onlyWelcome, int supportUnviewedMessages, Pageable pageable);

    @Query("SELECT usd " +
            "FROM UserSupportDialog usd JOIN usd.user u " +
            "WHERE (u.support.id = :supportId OR u.support.id IS NULL) AND u.roleType = 0 AND usd.supportUnviewedMessages > 0 " +
            "ORDER BY CASE WHEN u.support.id = :supportId THEN 0 ELSE 1 END, " +
            "usd.lastMessageDate DESC")
    List<UserSupportDialog> findUnviewedDialogsWithCustomSorting(@Param("supportId") long supportId, Pageable pageable);

    @Query("SELECT usd " +
            "FROM UserSupportDialog usd JOIN usd.user u " +
            "WHERE (u.support.id = :supportId OR u.support.id IS NULL) AND u.roleType = 0 " +
            "ORDER BY CASE WHEN u.support.id = :supportId THEN 0 ELSE 1 END, " +
            "usd.supportUnviewedMessages DESC, usd.lastMessageDate DESC")
    List<UserSupportDialog> findDialogsWithCustomSorting(@Param("supportId") long supportId, Pageable pageable);

    long countByOnlyWelcomeAndUserWorkerId(boolean onlyWelcome, long workerId);

    long countByOnlyWelcomeAndUserWorkerIdAndSupportUnviewedMessagesGreaterThan(boolean onlyWelcome, long workerId, int supportUnviewedMessages);

    long countByOnlyWelcomeAndSupportUnviewedMessagesGreaterThan(boolean onlyWelcome, int supportUnviewedMessages);

    long countByOnlyWelcome(boolean onlyWelcome);

    @Query("SELECT count(usd) " +
            "FROM UserSupportDialog usd JOIN usd.user u " +
            "WHERE (u.support.id = :supportId OR u.support.id IS NULL) AND u.roleType = 0 ")
    long countDialogsWithCustomSorting(@Param("supportId") long supportId);

    @Query("SELECT count(usd) " +
            "FROM UserSupportDialog usd JOIN usd.user u " +
            "WHERE (u.support.id = :supportId OR u.support.id IS NULL) AND u.roleType = 0 AND usd.supportUnviewedMessages > 0")
    long countUnviewedDialogsWithCustomSorting(@Param("supportId") long supportId);
}
