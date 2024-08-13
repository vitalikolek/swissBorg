package me.yukitale.cryptoexchange.panel.admin.repository.telegram;

import me.yukitale.cryptoexchange.panel.admin.model.telegram.AdminTelegramNotification;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdminTelegramNotificationRepository extends JpaRepository<AdminTelegramNotification, Long> {

    @CacheEvict(value = "admin_telegram_notifications", allEntries = true)
    @Override
    List<AdminTelegramNotification> findAll();

    @Caching(evict = {
            @CacheEvict(value = "admin_telegram_notifications", allEntries = true),
            @CacheEvict(value = "admin_telegram_notification_types", allEntries = true),
    })
    @Override
    <T extends AdminTelegramNotification> T save(T value);

    @Caching(evict = {
            @CacheEvict(value = "admin_telegram_notifications", allEntries = true),
            @CacheEvict(value = "admin_telegram_notification_types", allEntries = true),
    })
    @Override
    void deleteById(Long aLong);

    @Cacheable(value = "admin_telegram_notification_types", key = "#type")
    Optional<AdminTelegramNotification> findByType(AdminTelegramNotification.Type type);

    default boolean isEnabled(AdminTelegramNotification.Type type) {
        Optional<AdminTelegramNotification> telegramNotification = findByType(type);
        return telegramNotification.isPresent() && telegramNotification.get().isEnabled();
    }
}
