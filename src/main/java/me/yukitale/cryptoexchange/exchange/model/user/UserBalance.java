package me.yukitale.cryptoexchange.exchange.model.user;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.yukitale.cryptoexchange.exchange.model.Coin;
import me.yukitale.cryptoexchange.utils.MyDecimal;

import jakarta.persistence.*;

//todo: проверить
@Entity
@Table(name = "user_balances",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"coin_symbol", "user_id"})
        })
@NoArgsConstructor
@Getter
@Setter
public class UserBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coin_symbol")
    private Coin coin;

    private double balance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Transient
    public MyDecimal getFormattedBalance() {
        return new MyDecimal(this.balance);
    }

    @Transient
    public double getInUsd(double pricePerOne) {
        return balance * pricePerOne;
    }
}
