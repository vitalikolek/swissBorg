package me.yukitale.cryptoexchange.panel.admin.repository.payments;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import me.yukitale.cryptoexchange.panel.admin.model.payments.PaymentSettings;

@Repository
public interface PaymentSettingsRepository extends JpaRepository<PaymentSettings, Long> {

    @CacheEvict(value = "payment_settings", allEntries = true)
    <T extends PaymentSettings> T save(T paymentSettings);
    
    @Cacheable("payment_settings")
    default PaymentSettings findFirst() {
        if (count() == 0) {
            throw new RuntimeException("Transak settings not found");
        }
        return findAll().get(0);
    }
}
