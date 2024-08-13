package me.yukitale.cryptoexchange.exchange.model.user;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.yukitale.cryptoexchange.utils.StringUtil;
import org.springframework.format.annotation.DateTimeFormat;
import me.yukitale.cryptoexchange.utils.MyDecimal;

import java.util.Date;

@Entity
@Table(name = "user_trade_orders")
@Getter
@Setter
@NoArgsConstructor
public class UserTradeOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private Type type;

    @Column(columnDefinition = "TINYINT DEFAULT 1")
    private TradeType tradeType;

    @Column(nullable = false)
    private String coinSymbol;

    private double amount;

    private double price;

    private boolean closed;

    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private Date created;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public String getFormattedCreated() {
        return StringUtil.formatDate(this.created);
    }

    public MyDecimal getFormattedAmount() {
        return new MyDecimal(this.amount);
    }

    public MyDecimal getFormattedPrice() {
        return new MyDecimal(this.price, true);
    }

    public enum Type {

        BUY,
        SELL;
    }

    @AllArgsConstructor
    @Getter
    public enum TradeType {

        LIMIT("Limit"),
        MARKET("Market"),
        TRIGGER("Trigger");

        private final String title;
    }
}
