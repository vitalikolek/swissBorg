package me.yukitale.cryptoexchange.panel.worker.model.settings.other;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.yukitale.cryptoexchange.panel.common.model.SmartDepositStep;
import me.yukitale.cryptoexchange.panel.worker.model.Worker;

@Entity
@Table(name = "worker_smart_deposit_steps")
@Getter
@Setter
@NoArgsConstructor
public class WorkerSmartDepositStep extends SmartDepositStep {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", nullable = false)
    private Worker worker;
}
