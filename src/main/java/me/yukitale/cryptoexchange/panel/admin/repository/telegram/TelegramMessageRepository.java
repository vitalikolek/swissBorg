package me.yukitale.cryptoexchange.panel.admin.repository.telegram;

import me.yukitale.cryptoexchange.panel.admin.model.telegram.TelegramMessage;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TelegramMessageRepository extends JpaRepository<TelegramMessage, Long> {

    @CacheEvict(value = "admin_telegram_messages", allEntries = true)
    @Override
    List<TelegramMessage> findAll();

    @Caching(evict = {
            @CacheEvict(value = "admin_telegram_messages", allEntries = true),
            @CacheEvict(value = "admin_telegram_message_types", allEntries = true),
    })
    @Override
    <T extends TelegramMessage> T save(T value);

    @Caching(evict = {
            @CacheEvict(value = "admin_telegram_messages", allEntries = true),
            @CacheEvict(value = "admin_telegram_message_types", allEntries = true),
    })
    @Override
    void deleteById(Long aLong);

    @Cacheable(value = "admin_telegram_message_types", key = "#type")
    Optional<TelegramMessage> findByType(TelegramMessage.MessageType type);
}
