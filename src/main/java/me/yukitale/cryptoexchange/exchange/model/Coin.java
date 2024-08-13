package me.yukitale.cryptoexchange.exchange.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import java.util.List;

@Entity
@Table(name = "coins")
@Getter
@Setter
@NoArgsConstructor
public class Coin {

    private static final List<String> STABLE_COINS = List.of("USDT", "USDC", "BUSD");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long position;

    @Size(min = 2, max = 8)
    @Column(unique = true, nullable = false)
    private String symbol;

    @Size(max = 64)
    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String icon;

    private boolean memo;

    @Transient
    public boolean isStable() {
        return STABLE_COINS.contains(this.symbol.toUpperCase());
    }
}
