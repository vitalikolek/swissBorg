package me.yukitale.cryptoexchange.panel.worker.model.settings.other;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import me.yukitale.cryptoexchange.panel.common.model.CoinSettings;
import me.yukitale.cryptoexchange.panel.worker.model.Worker;

@Entity
@Table(name = "worker_coin_settings")
@Getter
@Setter
public class WorkerCoinSettings extends CoinSettings {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", unique = true)
    private Worker worker;
}
