package me.yukitale.cryptoexchange.exchange.model.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import me.yukitale.cryptoexchange.utils.MyDecimal;
import me.yukitale.cryptoexchange.utils.StringUtil;

import java.util.Date;

@Entity
@Table(name = "user_transactions")
@Getter
@Setter
@NoArgsConstructor
public class UserTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private Type type;

    @Column(nullable = false)
    private Status status;

    private String address;

    private String memo;

    @Size(max = 6)
    private String network;

    @Column(nullable = false)
    private String coinSymbol;

    private double amount;

    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private Date date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public MyDecimal formattedAmount() {
        return new MyDecimal(this.amount);
    }

    public String formattedDate() {
        return StringUtil.formatDate(date);
    }

    @AllArgsConstructor
    @Getter
    public enum Type {

        DEPOSIT("Deposit", true),
        BONUS("Bonus", true),
        PROMO("Promo", true),
        TRANSFER_OUT("Transfer (OUT)", false),
        TRANSFER_IN("Transfer (IN)", true),
        STAKE("Stake", false),
        UNSTAKE("Unstake", true),
        WITHDRAW("Withdraw", false);

        private final String title;
        private final boolean incrementBalance;
    }

    @AllArgsConstructor
    @Getter
    public enum Status {

        IN_PROCESSING("In processing"),
        CANCELED("Canceled"),
        COMPLETED("Completed");

        private final String title;
    }
}
