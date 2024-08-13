package me.yukitale.cryptoexchange.panel.worker.model.settings.other;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.yukitale.cryptoexchange.panel.common.model.SupportPreset;
import me.yukitale.cryptoexchange.panel.worker.model.Worker;

@Entity
@Table(name = "worker_support_presets")
@Getter
@Setter
@NoArgsConstructor
public class WorkerSupportPreset extends SupportPreset {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", nullable = false)
    private Worker worker;
}
