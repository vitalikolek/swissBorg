package me.yukitale.cryptoexchange.exchange.model.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.yukitale.cryptoexchange.common.types.CoinType;

@Entity
@Table(name = "user_addresses",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"coinType", "user_id"})
        })
@Getter
@Setter
@NoArgsConstructor
public class UserAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private long created;

    private String address;

    private String tag;

    @Column(nullable = false)
    private CoinType coinType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "user_id", insertable = false, updatable = false)
    private long userId;

    @Transient
    public boolean isExpired() {
        return this.created <= 1702598400000L || ((System.currentTimeMillis() - this.created) / 1000L / 86400L >= 30);
    }
}
