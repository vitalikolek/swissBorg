package me.yukitale.cryptoexchange.panel.admin.repository.other;

import me.yukitale.cryptoexchange.panel.admin.model.other.AdminSupportPreset;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminSupportPresetRepository extends JpaRepository<AdminSupportPreset, Long> {

    @CacheEvict(value = "admin_support_presets", allEntries = true)
    @Override
    List<AdminSupportPreset> findAll();

    @CacheEvict(value = "admin_support_presets", allEntries = true)
    @Override
    <T extends AdminSupportPreset> T save(T value);

    @CacheEvict(value = "admin_support_presets", allEntries = true)
    @Override
    void deleteById(Long aLong);
}
