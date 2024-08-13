package me.yukitale.cryptoexchange.panel.admin.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import me.yukitale.cryptoexchange.panel.admin.model.p2pfake.P2PFake;
import me.yukitale.cryptoexchange.panel.admin.repository.p2pfake.P2PFakeRepository;
import me.yukitale.cryptoexchange.utils.MyDecimal;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class P2PFakeService {

    @Autowired
    private P2PFakeRepository p2PFakeRepository;

    private Map<P2PFake, Double> p2PFakes;

    private long lastUpdate;

    public void save(P2PFake p2PFake) {
        this.p2PFakeRepository.save(p2PFake);

        this.p2PFakes = null;
        this.lastUpdate = 0;
    }

    public boolean deleteIfExists(long id) {
        boolean exists = p2PFakeRepository.existsById(id);
        if (exists) {
            p2PFakeRepository.deleteById(id);

            this.p2PFakes = null;
            this.lastUpdate = 0;
        }

        return exists;
    }

    public Map<P2PFake, MyDecimal> getP2PFakes(double price) {
        if (this.p2PFakes == null || System.currentTimeMillis() - lastUpdate > 15000) {
            List<P2PFake> p2PFakeList = p2PFakeRepository.findAll();
            Collections.shuffle(p2PFakeList);
            Map<P2PFake, Double> map = new LinkedHashMap<>();
            double lastValue = 0;
            for (P2PFake p2PFake : p2PFakeList) {
                lastValue += ThreadLocalRandom.current().nextDouble(5, 50);
                map.put(p2PFake, lastValue);
                if (map.size() == 15) {
                    break;
                }
            }

            this.p2PFakes = new LinkedHashMap<>();

            for (int i = map.size() - 1; i >= 0; i--) {
                Map.Entry<P2PFake, Double> entry = (Map.Entry<P2PFake, Double>) map.entrySet().toArray()[i];
                this.p2PFakes.put(entry.getKey(), entry.getValue());
            }

            lastUpdate = System.currentTimeMillis();
        }

        Map<P2PFake, MyDecimal> fakes = new LinkedHashMap<>();
        for (Map.Entry<P2PFake, Double> entry : this.p2PFakes.entrySet()) {
            fakes.put(entry.getKey(), new MyDecimal(entry.getValue() + price, true));
        }

        return fakes;
    }
}
