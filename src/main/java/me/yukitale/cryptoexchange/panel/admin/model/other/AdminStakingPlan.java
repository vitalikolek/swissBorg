package me.yukitale.cryptoexchange.panel.admin.model.other;

import jakarta.persistence.*;
import me.yukitale.cryptoexchange.panel.common.model.StakingPlan;

@Entity
@Table(name = "admin_staking_plans")
public class AdminStakingPlan extends StakingPlan {
}
