package me.yukitale.cryptoexchange.panel.worker.model.settings.other;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.yukitale.cryptoexchange.exchange.model.Coin;
import me.yukitale.cryptoexchange.panel.common.types.KycAcceptTimer;
import me.yukitale.cryptoexchange.panel.worker.model.Worker;

@Entity
@Table(name = "worker_settings")
@Getter
@Setter
@NoArgsConstructor
public class WorkerSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private long telegramId;

    @Size(max = 512)
    private String supportWelcomeMessage;

    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean supportWelcomeEnabled;

    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean supportPresetsEnabled;

    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean promoFormEnabled;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean promoHideEnabled;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean showAddressAlways;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean showQrAlways;

    private double p2pOverPrice;

    @Size(max = 512)
    private String bonusText;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coin_symbol")
    private Coin bonusCoin;

    private double bonusAmount;

    @Column(columnDefinition = "TINYINT DEFAULT 0")
    private KycAcceptTimer kycAcceptTimer;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", unique = true)
    private Worker worker;
}
