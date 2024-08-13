package me.yukitale.cryptoexchange.panel.common.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
public abstract class CoinSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private double minDepositAmount;

    @Column(columnDefinition = "DOUBLE DEFAULT 0.0")
    private double depositCommission;

    @Column(columnDefinition = "DOUBLE DEFAULT 1.0")
    private double withdrawCommission;

    private boolean verifRequirement;

    private boolean verifAml;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean useBtcVerifDeposit;

    private double minVerifAmount;

    private double minWithdrawAmount;
}
