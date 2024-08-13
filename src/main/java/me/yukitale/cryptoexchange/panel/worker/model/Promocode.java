package me.yukitale.cryptoexchange.panel.worker.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.yukitale.cryptoexchange.utils.StringUtil;
import org.springframework.format.annotation.DateTimeFormat;
import me.yukitale.cryptoexchange.exchange.model.Coin;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;

import java.util.Date;

@Entity
@Table(name = "worker_promocodes")
@Getter
@Setter
@NoArgsConstructor
public class Promocode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Size(min = 2, max = 64)
    @Column(unique = true, nullable = false)
    private String name;

    @Size(min = 1, max = 256)
    private String text;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coin_symbol")
    private Coin coin;

    private double minAmount;

    private double maxAmount;

    private double bonusAmount;

    private int activations;

    private int deposits;

    private double depositsPrice;

    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private Date created;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", nullable = true)
    private Worker worker;

    public Promocode(String name, String text, Coin coin, double minAmount, double maxAmount, double bonusAmount, Worker worker) {
        this.name = name;
        this.text = text;
        this.coin = coin;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.bonusAmount = bonusAmount;
        this.created = new Date();
        this.worker = worker;
    }

    @Transient
    public String getFormattedDate() {
        long lastActivity = created.getTime();
        long diff = (System.currentTimeMillis() - lastActivity) / 1000L;
        if (diff < 60) {
            return diff + " сек. назад";
        } else if (diff > 86400) {
            return StringUtil.formatDate(new Date(lastActivity));
        } else if (diff > 3600) {
            return diff / 3600 + "ч. назад";
        } else {
            return diff / 60 + " мин. назад";
        }
    }

    @Transient
    public boolean isRandom() {
        return this.maxAmount > this.minAmount;
    }
}
