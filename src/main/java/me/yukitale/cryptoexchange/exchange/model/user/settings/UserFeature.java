package me.yukitale.cryptoexchange.exchange.model.user.settings;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.yukitale.cryptoexchange.exchange.model.user.User;

@Entity
@Table(name = "user_features",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"type", "user_id"})
        })
@Getter
@Setter
@NoArgsConstructor
public class UserFeature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private Type type;

    private boolean enabled;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @AllArgsConstructor
    @Getter
    public enum Type {

        TRADING(false),
        STAKING(true),
        SWAP(true),
        TRANSFER(true),
        SUPPORT(true),

        WALLET_CONNECT(false),
        PREMIUM(false),

        FAKE_WITHDRAW_PENDING(false),
        FAKE_WITHDRAW_CONFIRMED(false);

        private boolean defaultValue;
    }
}
