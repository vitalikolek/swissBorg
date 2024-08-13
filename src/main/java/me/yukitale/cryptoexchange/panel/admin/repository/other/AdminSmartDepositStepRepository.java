package me.yukitale.cryptoexchange.panel.admin.repository.other;

import me.yukitale.cryptoexchange.panel.admin.model.other.AdminSmartDepositStep;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminSmartDepositStepRepository extends JpaRepository<AdminSmartDepositStep, Long> {

    @CacheEvict(value = "admin_smart_deposit_steps", allEntries = true)
    @Override
    List<AdminSmartDepositStep> findAll();

    @CacheEvict(value = "admin_smart_deposit_steps", allEntries = true)
    @Override
    <T extends AdminSmartDepositStep> T save(T value);

    @CacheEvict(value = "admin_smart_deposit_steps", allEntries = true)
    @Override
    void deleteById(Long aLong);
}
