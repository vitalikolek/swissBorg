package me.yukitale.cryptoexchange.panel.common.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.yukitale.cryptoexchange.common.types.CoinType;
import me.yukitale.cryptoexchange.utils.MyDecimal;

@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
public abstract class DepositCoin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long position;

    @Column(nullable = false)
    private CoinType type;

    @Column(name = "symbol", length = 8, nullable = false)
    @Size(min = 2, max = 8)
    private String symbol;

    @Size(max = 64)
    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String icon;

    private double minReceiveAmount;

    private double minDepositAmount;

    @Column(columnDefinition = "DOUBLE DEFAULT 0")
    private double verifDepositAmount;
    
    private boolean enabled;

    @Transient
    public boolean isMinDepositAmount() {
        return !Double.isNaN(this.minDepositAmount) && this.minDepositAmount > 0;
    }

    public MyDecimal formattedMinReceiveAmount() {
        return new MyDecimal(this.minReceiveAmount);
    }


    public MyDecimal formattedVerifDepositAmount() {
        return new MyDecimal(this.verifDepositAmount);
    }
    
    public MyDecimal formattedMinDepositAmount() {
        return new MyDecimal(this.minDepositAmount);
    }
}
