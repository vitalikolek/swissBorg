package me.yukitale.cryptoexchange.panel.admin.repository.other;

import me.yukitale.cryptoexchange.panel.admin.model.other.AdminFeature;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdminFeatureRepository extends JpaRepository<AdminFeature, Long> {

    @CacheEvict(value = "admin_features", allEntries = true)
    @Override
    List<AdminFeature> findAll();

    @Caching(evict = {
            @CacheEvict(value = "admin_feature_types", allEntries = true),
            @CacheEvict(value = "admin_features", allEntries = true)
    })
    @Override
    <T extends AdminFeature> T save(T value);

    @Cacheable(value = "admin_feature_types", key = "#type")
    Optional<AdminFeature> findByType(AdminFeature.FeatureType type);
}
