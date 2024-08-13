package me.yukitale.cryptoexchange.panel.worker.model.settings.coins;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.yukitale.cryptoexchange.exchange.model.Coin;
import me.yukitale.cryptoexchange.panel.worker.model.Worker;

@Entity
@Table(name = "worker_withdraw_coin_limits",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"coin_symbol", "worker_id"})
        })
@Getter
@Setter
@NoArgsConstructor
public class WithdrawCoinLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coin_symbol")
    private Coin coin;

    private double minAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", nullable = false)
    private Worker worker;
}
