package me.yukitale.cryptoexchange.panel.admin.model.other;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.yukitale.cryptoexchange.panel.common.model.SmartDepositStep;

@Entity
@Table(name = "admin_smart_deposit_steps")
@Getter
@Setter
@NoArgsConstructor
public class AdminSmartDepositStep extends SmartDepositStep {
}
