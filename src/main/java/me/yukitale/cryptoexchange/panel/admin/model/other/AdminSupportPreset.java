package me.yukitale.cryptoexchange.panel.admin.model.other;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import me.yukitale.cryptoexchange.panel.common.model.SupportPreset;

@Entity
@Table(name = "admin_support_presets")
public class AdminSupportPreset extends SupportPreset {
}
