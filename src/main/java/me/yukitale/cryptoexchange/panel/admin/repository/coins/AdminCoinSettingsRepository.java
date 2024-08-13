package me.yukitale.cryptoexchange.panel.admin.repository.coins;

import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import me.yukitale.cryptoexchange.panel.admin.model.coins.AdminCoinSettings;

@Repository
public interface AdminCoinSettingsRepository extends JpaRepository<AdminCoinSettings, Long> {

    @CachePut("admin_coin_settings")
    @Override
    <T extends AdminCoinSettings> T save(T value);

    @Cacheable("admin_coin_settings")
    default AdminCoinSettings findFirst() {
        if (count() == 0) {
            throw new RuntimeException("Admin coin settings not found");
        }
        return findAll().get(0);
    }
}
