package me.yukitale.cryptoexchange.panel.admin.repository.other;

import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import me.yukitale.cryptoexchange.panel.admin.model.other.AdminLegalSettings;

@Repository
public interface AdminLegalSettingsRepository extends JpaRepository<AdminLegalSettings, Long> {

    @CachePut("admin_legal_settings")
    @Override
    <T extends AdminLegalSettings> T save(T value);

    @Cacheable("admin_legal_settings")
    default AdminLegalSettings findFirst() {
        if (count() == 0) {
            throw new RuntimeException("Admin legal settings not found");
        }
        return findAll().get(0);
    }
}
