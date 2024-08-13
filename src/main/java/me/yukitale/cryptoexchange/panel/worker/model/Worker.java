package me.yukitale.cryptoexchange.panel.worker.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.yukitale.cryptoexchange.exchange.model.user.User;
import me.yukitale.cryptoexchange.panel.worker.model.settings.coins.WithdrawCoinLimit;
import me.yukitale.cryptoexchange.panel.worker.model.settings.coins.WorkerDepositCoin;
import me.yukitale.cryptoexchange.panel.worker.model.settings.other.*;

import jakarta.persistence.*;
import me.yukitale.cryptoexchange.utils.MyDecimal;

import java.util.List;

@Entity
@Table(name = "workers")
@Getter
@Setter
@NoArgsConstructor
public class Worker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private long usersCount;

    private long depositsCount;

    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean supportOwn;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean smartDepositEnabled;

    private double deposits;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @OneToOne(mappedBy = "worker", fetch = FetchType.LAZY)
    private WorkerSettings settings;

    @OneToOne(mappedBy = "worker", fetch = FetchType.LAZY)
    private WorkerCoinSettings coinSettings;

    @OneToMany(mappedBy = "worker", fetch = FetchType.LAZY)
    private List<WorkerFeature> features;

    @OneToMany(mappedBy = "worker", fetch = FetchType.LAZY)
    private List<WorkerErrorMessage> errorMessages;

    @OneToMany(mappedBy = "worker", fetch = FetchType.LAZY)
    private List<WorkerStakingPlan> stakingPlans;

    @OneToMany(mappedBy = "worker", fetch = FetchType.LAZY)
    private List<WorkerTelegramNotification> telegramNotifications;

    @OneToMany(mappedBy = "worker", fetch = FetchType.LAZY)
    private List<Domain> domains;

    @OneToMany(mappedBy = "worker", fetch = FetchType.LAZY)
    private List<Promocode> promocodes;

    @OneToMany(mappedBy = "worker", fetch = FetchType.LAZY)
    private List<FastPump> fastPumps;

    @OneToMany(mappedBy = "worker", fetch = FetchType.LAZY)
    private List<StablePump> stablePumps;

    @OneToMany(mappedBy = "worker", fetch = FetchType.LAZY)
    private List<WorkerDepositCoin> depositCoins;

    @OneToMany(mappedBy = "worker", fetch = FetchType.LAZY)
    private List<WithdrawCoinLimit> withdrawCoinLimits;

    @OneToMany(mappedBy = "worker", fetch = FetchType.LAZY)
    private List<User> users;

    @Transient
    public MyDecimal formattedDeposits() {
        return new MyDecimal(this.deposits, true);
    }
}
