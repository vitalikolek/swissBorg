package me.yukitale.cryptoexchange.panel.common.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.yukitale.cryptoexchange.panel.admin.model.other.AdminErrorMessage;

@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
public abstract class ErrorMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private ErrorMessageType type;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    @AllArgsConstructor
    @Getter
    public enum ErrorMessageType {

        TRADING("Spot Trading error"),
        STAKING("Staking error"),
        SWAP("Swap error"),
        TRANSFER("Transfer error"),
        SUPPORT("Support error"),
        P2P("P2P error"),
        WITHDRAW("Withdraw error"),
        WITHDRAW_VERIFICATION("Withdraw Verification error"),
        WITHDRAW_AML("Withdraw AML Error"),
        OTHER("Other Error");

        private final String title;
    }
}
