package me.yukitale.cryptoexchange.panel.admin.repository.p2pfake;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import me.yukitale.cryptoexchange.panel.admin.model.p2pfake.P2PFake;

import java.util.List;


@Repository
public interface P2PFakeRepository extends JpaRepository<P2PFake, Long> {

    @CacheEvict(value = "admin_p2p_fakes", allEntries = true)
    @Override
    List<P2PFake> findAll();

    @CacheEvict(value = "admin_p2p_fakes", allEntries = true)
    @Override
    <T extends P2PFake> T save(T value);

    @CacheEvict(value = "admin_p2p_fakes", allEntries = true)
    @Override
    void deleteById(Long aLong);
}
