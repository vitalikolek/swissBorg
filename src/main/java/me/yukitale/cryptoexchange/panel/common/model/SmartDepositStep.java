package me.yukitale.cryptoexchange.panel.common.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
public abstract class SmartDepositStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private double amount;

    private String coinSymbol;

    private int type;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean fakeWithdrawPending;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean fakeWithdrawConfirmed;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean premium;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean walletConnect;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean verifModal;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean amlModal;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean fakeVerified;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean globalBan;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean changeWithdrawError;

    @Column(columnDefinition = "TEXT")
    private String withdrawError;

    public SmartDepositStepType getType() {
        return SmartDepositStepType.values()[this.type];
    }

    public void setType(SmartDepositStepType type) {
        this.type = type.ordinal();
    }

    public enum SmartDepositStepType {

        MIN_DEPOSIT,
        TOTAL_DEPOSITS_SUM;
    }
}
