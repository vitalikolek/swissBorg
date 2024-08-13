package me.yukitale.cryptoexchange.panel.worker.model.settings.other;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.yukitale.cryptoexchange.panel.worker.model.Worker;

@Entity
@Table(name = "worker_record_settings")
@Getter
@Setter
@NoArgsConstructor
public class WorkerRecordSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(unique = true)
    private long emailEnd;

    private boolean fakeWithdrawPending;

    private boolean fakeWithdrawConfirmed;

    private boolean premium;

    private boolean walletConnect;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean fakeVerified;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", nullable = false)
    private Worker worker;
}
