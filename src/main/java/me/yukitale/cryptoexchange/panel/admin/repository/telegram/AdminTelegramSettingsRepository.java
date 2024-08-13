package me.yukitale.cryptoexchange.panel.admin.repository.telegram;

import me.yukitale.cryptoexchange.panel.admin.model.telegram.AdminTelegramSettings;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminTelegramSettingsRepository extends JpaRepository<AdminTelegramSettings, Long> {

    @CacheEvict(value = "admin_telegram_settings", allEntries = true)
    @Override
    <T extends AdminTelegramSettings> T save(T adminSettings);

    @Cacheable("admin_telegram_settings")
    default AdminTelegramSettings findFirst() {
        if (count() == 0) {
            throw new RuntimeException("Admin telegram settings not found");
        }
        return findAll().get(0);
    }
}
