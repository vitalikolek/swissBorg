package me.yukitale.cryptoexchange.panel.admin.model.coins;

import jakarta.persistence.*;
import me.yukitale.cryptoexchange.panel.common.model.CoinSettings;

@Entity
@Table(name = "admin_coin_settings")
public class AdminCoinSettings extends CoinSettings {
}
