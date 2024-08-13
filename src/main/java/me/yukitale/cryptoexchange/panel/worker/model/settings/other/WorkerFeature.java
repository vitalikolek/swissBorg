package me.yukitale.cryptoexchange.panel.worker.model.settings.other;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.yukitale.cryptoexchange.panel.common.model.Feature;
import me.yukitale.cryptoexchange.panel.worker.model.Worker;

@Entity
@Table(name = "worker_features",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"type", "worker_id"})
        })
@Getter
@Setter
@NoArgsConstructor
public class WorkerFeature extends Feature {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", nullable = false)
    private Worker worker;
}
