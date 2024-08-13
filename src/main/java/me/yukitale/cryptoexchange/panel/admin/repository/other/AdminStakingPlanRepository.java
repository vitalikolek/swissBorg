package me.yukitale.cryptoexchange.panel.admin.repository.other;

import me.yukitale.cryptoexchange.panel.admin.model.other.AdminStakingPlan;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminStakingPlanRepository extends JpaRepository<AdminStakingPlan, Long> {

    @CacheEvict(value = "admin_staking_plans", allEntries = true)
    @Override
    List<AdminStakingPlan> findAll();

    @CacheEvict(value = "admin_staking_plans", allEntries = true)
    @Override
    <T extends AdminStakingPlan> T save(T value);

    @CacheEvict(value = "admin_staking_plans", allEntries = true)
    @Override
    void deleteById(Long aLong);
}
