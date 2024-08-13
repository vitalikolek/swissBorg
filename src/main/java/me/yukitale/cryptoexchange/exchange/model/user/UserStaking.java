package me.yukitale.cryptoexchange.exchange.model.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import me.yukitale.cryptoexchange.exchange.model.Coin;
import me.yukitale.cryptoexchange.utils.MyDecimal;
import me.yukitale.cryptoexchange.utils.StringUtil;

import java.util.Date;

@Entity
@Table(name = "user_stakings")
@Getter
@Setter
@NoArgsConstructor
public class UserStaking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coin_symbol")
    private Coin coin;

    private double amount;

    @Column(nullable = false)
    private String title;

    private double percent;

    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private Date startDate;

    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private Date endDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Transient
    public boolean isEnded() {
        return this.endDate.getTime() < System.currentTimeMillis();
    }

    @Transient
    public String formattedEndDate() {
        return StringUtil.formatDate(this.endDate);
    }

    @Transient
    public MyDecimal formattedProfit() {
        return new MyDecimal(this.amount * (this.percent / 100D * ((double) (System.currentTimeMillis() - this.startDate.getTime()) / 1000L / 86400L)));
    }

    @Transient
    public MyDecimal formattedProfitUsd(double course) {
        return new MyDecimal(course * (this.amount * (this.percent / 100D * ((double) (System.currentTimeMillis() - this.startDate.getTime()) / 1000L / 86400L))), true);
    }
}
