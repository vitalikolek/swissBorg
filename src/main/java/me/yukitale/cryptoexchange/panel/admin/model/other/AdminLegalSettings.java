package me.yukitale.cryptoexchange.panel.admin.model.other;

import jakarta.persistence.*;
import me.yukitale.cryptoexchange.panel.common.model.LegalSettings;

@Entity
@Table(name = "admin_legal_settings")
public class AdminLegalSettings extends LegalSettings {
}
