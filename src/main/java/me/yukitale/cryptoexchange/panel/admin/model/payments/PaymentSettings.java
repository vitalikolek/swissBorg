package me.yukitale.cryptoexchange.panel.admin.model.payments;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;

import jakarta.persistence.*;

@Entity
@Table(name = "payment_settings")
@Getter
@Setter
@NoArgsConstructor
public class PaymentSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String westWalletPublicKey;

    private String westWalletPrivateKey;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean transakEnabled;
}
