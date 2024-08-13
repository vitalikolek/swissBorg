package me.yukitale.cryptoexchange.panel.worker.model.settings.coins;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import me.yukitale.cryptoexchange.panel.common.model.DepositCoin;
import me.yukitale.cryptoexchange.panel.worker.model.Worker;

@Entity
@Table(name = "worker_deposit_coins")
@Getter
@Setter
public class WorkerDepositCoin extends DepositCoin {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", nullable = false)
    private Worker worker;
}
