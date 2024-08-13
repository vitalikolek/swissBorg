package me.yukitale.cryptoexchange.exchange.model.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.yukitale.cryptoexchange.utils.StringUtil;
import org.springframework.format.annotation.DateTimeFormat;
import me.yukitale.cryptoexchange.common.types.CoinType;
import me.yukitale.cryptoexchange.panel.worker.model.Worker;
import me.yukitale.cryptoexchange.utils.MyDecimal;

import java.util.Date;

@Entity
@Table(name = "user_deposits")
@Getter
@Setter
@NoArgsConstructor
public class UserDeposit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(unique = true)
    private long txId;

    @Column(nullable = false)
    private CoinType coinType;

    private String hash;

    private double amount;

    private double price;

    private boolean completed;

    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean botReceived;

    @Column(columnDefinition = "VARCHAR(8) DEFAULT 'NO'")
    private String countryCode;

    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private Date date;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private UserTransaction transaction;

    @Column(name = "transaction_id", insertable = false, updatable = false)
    private long transactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "user_id", insertable = false, updatable = false)
    private long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id")
    private Worker worker;

    @Transient
    public String getFormattedDate() {
        long diff = (System.currentTimeMillis() - this.date.getTime()) / 1000L;
        if (diff < 60) {
            return diff + " сек. назад";
        } else if (diff > 86400) {
            return StringUtil.formatDate(this.date);
        } else if (diff > 3600) {
            return diff / 3600 + "ч. назад";
        } else {
            return diff / 60 + " мин. назад";
        }
    }

    @Transient
    public MyDecimal getFormattedAmount() {
        return new MyDecimal(this.amount);
    }

    @Transient
    public MyDecimal getFormattedPrice() {
        return new MyDecimal(this.price, true);
    }
}
