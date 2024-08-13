package me.yukitale.cryptoexchange.exchange.model.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.yukitale.cryptoexchange.exchange.model.Coin;

@Entity
@Table(name = "user_alerts")
@Getter
@Setter
@NoArgsConstructor
public class UserAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private Type type;

    @Column(columnDefinition = "TEXT", length = 1000)
    @Size(max = 1000)
    private String message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coin_symbol")
    private Coin coin;

    private double amount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public enum Type {

        NOTIFICATION,
        ALERT,
        BONUS,
        WALLET_CONNECT;
    }
}
