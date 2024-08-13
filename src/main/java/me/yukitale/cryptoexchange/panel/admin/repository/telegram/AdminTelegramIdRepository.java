package me.yukitale.cryptoexchange.panel.admin.repository.telegram;

import me.yukitale.cryptoexchange.panel.admin.model.telegram.AdminTelegramId;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminTelegramIdRepository extends JpaRepository<AdminTelegramId, Long> {

    @CacheEvict(value = "admin_telegram_ids", allEntries = true)
    @Override
    List<AdminTelegramId> findAll();

    @CacheEvict(value = "admin_telegram_ids", allEntries = true)
    @Override
    <T extends AdminTelegramId> T save(T value);

    @CacheEvict(value = "admin_telegram_ids", allEntries = true)
    @Override
    void deleteById(Long aLong);

    boolean existsByTelegramId(long telegramId);
}
