package me.yukitale.cryptoexchange.panel.admin.model.coins;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import me.yukitale.cryptoexchange.panel.common.model.DepositCoin;

@Entity
@Table(name = "admin_deposit_coins")
public class AdminDepositCoin extends DepositCoin {
}
