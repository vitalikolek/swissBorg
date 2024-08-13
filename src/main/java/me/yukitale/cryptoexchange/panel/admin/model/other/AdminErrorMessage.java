package me.yukitale.cryptoexchange.panel.admin.model.other;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import me.yukitale.cryptoexchange.panel.common.model.ErrorMessage;

@Entity
@Table(name = "admin_error_messages")
public class AdminErrorMessage extends ErrorMessage {
}
