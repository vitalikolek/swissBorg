package me.yukitale.cryptoexchange.panel.worker.service;

import me.yukitale.cryptoexchange.panel.admin.model.other.AdminSettings;
import me.yukitale.cryptoexchange.panel.admin.repository.other.AdminSettingsRepository;
import me.yukitale.cryptoexchange.panel.worker.repository.DomainRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import me.yukitale.cryptoexchange.panel.admin.model.other.AdminEmailSettings;
import me.yukitale.cryptoexchange.panel.admin.repository.other.AdminEmailSettingsRepository;
import me.yukitale.cryptoexchange.panel.worker.model.Domain;
import me.yukitale.cryptoexchange.panel.worker.model.Worker;

import java.util.Date;

@Service
public class DomainService {

    @Autowired
    private AdminEmailSettingsRepository adminEmailSettingsRepository;

    @Autowired
    private AdminSettingsRepository adminSettingsRepository;

    @Autowired
    private DomainRepository domainRepository;

    public void createDomain(Worker worker, String name, String exchangeName, String title, String icon) {
        AdminEmailSettings adminEmailSettings = adminEmailSettingsRepository.findFirst();
        AdminSettings adminSettings = adminSettingsRepository.findFirst();

        Domain domain = new Domain();

        domain.setWorker(worker);
        domain.setName(name);
        domain.setExchangeName(exchangeName);
        domain.setTitle(title);
        domain.setIcon(icon);
        domain.setHomeDesign(adminSettings.getHomeDesign());
        domain.setAdded(new Date());
        domain.setServer(adminEmailSettings.getServer());
        domain.setPort(adminEmailSettings.getPort());

        domainRepository.save(domain);
    }
}
