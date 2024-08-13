package me.yukitale.cryptoexchange.exchange.repository.user.settings;

import me.yukitale.cryptoexchange.exchange.model.user.settings.UserFeature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

//todo: проверить
@Repository
public interface UserFeatureRepository extends JpaRepository<UserFeature, Long> {

    List<UserFeature> findAllByUserId(long userId);

    Optional<UserFeature> findByUserIdAndType(long userId, UserFeature.Type type);

    List<UserFeature> findAllByUserWorkerIdAndType(long workerId, UserFeature.Type type);
}

/*
@Repository
public interface UserFeatureRepository extends JpaRepository<UserFeature, Long> {

    @Caching(evict = {
            @CacheEvict(value = "user_features", key = "#result.user.id"),
            @CacheEvict(value = "user_feature_types", key = "#result.user.id + '-' + #result.type.name"),
            @CacheEvict(value = "user_worker_feature_types", key = "#result.user.worker == null ? 'none' : (#result.user.worker.id + '-' + #result.type.name)")
    })
    @Override
    <T extends UserFeature> T save(T result);

    @Cacheable("user_features", key = "#userId")
    List<UserFeature> findAllByUserId(long userId);

    @Cacheable(value = "user_feature_types", key = "#userId + '-' + #type.name")
    Optional<UserFeature> findByUserIdAndType(long userId, UserFeature.Type type);

    @Cacheable(value = "user_worker_feature_types", key = "#workerId + '-' + #type.name")
    List<UserFeature> findAllByUserWorkerIdAndType(long workerId, UserFeature.Type type);
}

 */