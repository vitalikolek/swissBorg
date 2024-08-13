package me.yukitale.cryptoexchange.exchange.model.user.settings;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.yukitale.cryptoexchange.exchange.model.user.User;
import me.yukitale.cryptoexchange.panel.worker.model.settings.coins.WorkerDepositCoin;

@Entity
@Table(name = "user_required_deposit_coins")
@Getter
@Setter
@NoArgsConstructor
public class UserRequiredDepositCoin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deposit_coin_id")
    private WorkerDepositCoin depositCoin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}
