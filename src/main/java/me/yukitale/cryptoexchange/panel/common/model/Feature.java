package me.yukitale.cryptoexchange.panel.common.model;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
public abstract class Feature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private FeatureType type;

    private boolean enabled;

    @AllArgsConstructor
    @Getter
    public enum FeatureType {

        FAKE_WITHDRAW_PENDING(false, "Enable Fake Withdraw Pending Modal for NEW users", "Если включено, то при вывод средств у <strong>новых</strong> пользователей будет со статусом \"In processing\" и соответствующим модальным окном."),
        FAKE_WITHDRAW_CONFIRMED(false, "Enable Fake Withdraw Confirmed Modal for NEW users", "Если включено, то при вывод средств у <strong>новых</strong> пользователей будет со статусом \"Completed\" и соответствующим модальным окном."),
        TRADING(true, "Enable Trading for NEW users", "Если включено, то <strong>новые</strong> пользователи смогут создавать сделки в Trading."),
        SWAP(true, "Enable Swap for NEW users", "Если включено, то у <strong>новых</strong> пользователей будет возможность менять монеты в Swap. <br>"),
        STAKING(true, "Enable Staking for NEW users", "Если включено, то у <strong>новых</strong> пользователей будет возможность стейкать монеты в Staking. <br>"),
        TRANSFER(true, "Enable Transfer for NEW users", "<span class=\"text-gray-600\">Если включено, то <strong>новые</strong> пользователи смогут совершать трансфер средств внутри биржи.</span>"),
        PREMIUM(false, "Enable Premium for NEW users", "<span class=\"text-gray-600\">Если включено, то <strong>новые</strong> пользователи по умолчанию будут иметь статус Premium аккаунта.</span>"),
        WALLET_CONNECT(false, "Enable Wallet connect for NEW users", "<span class=\"text-gray-600\">Если включено, то у <strong>новых</strong> пользователей по умолчанию будет отображаться кнопка для перехода на страницу Wallet connect.</span>");

        private final boolean defaultValue;
        private final String title;
        private final String description;
    }
}
