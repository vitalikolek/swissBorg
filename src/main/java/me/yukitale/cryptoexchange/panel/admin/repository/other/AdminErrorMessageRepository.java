package me.yukitale.cryptoexchange.panel.admin.repository.other;

import me.yukitale.cryptoexchange.panel.admin.model.other.AdminErrorMessage;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import me.yukitale.cryptoexchange.panel.common.model.ErrorMessage;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdminErrorMessageRepository extends JpaRepository<AdminErrorMessage, Long> {

    @CacheEvict(value = "admin_error_messages", allEntries = true)
    @Override
    List<AdminErrorMessage> findAll();

    @Caching(evict = {
            @CacheEvict(value = "admin_error_message_types", allEntries = true),
            @CacheEvict(value = "admin_error_messages", allEntries = true)
    })
    @Override
    <T extends AdminErrorMessage> T save(T value);

    @Cacheable(value = "admin_error_message_types", key = "#type")
    Optional<AdminErrorMessage> findByType(ErrorMessage.ErrorMessageType type);
}
