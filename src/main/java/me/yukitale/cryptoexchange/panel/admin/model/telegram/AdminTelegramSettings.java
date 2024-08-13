package me.yukitale.cryptoexchange.panel.admin.model.telegram;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "admin_telegram_settings")
@Getter
@Setter
@NoArgsConstructor
public class AdminTelegramSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Size(max = 48)
    private String botUsername;

    @Size(max = 128)
    private String botToken;

    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean channelNotification;

    private long channelId;

    private String channelMessage;

    @Transient
    public boolean isEnabled() {
        return StringUtils.isNotBlank(this.botUsername) && StringUtils.isNotBlank(this.botToken);
    }
}
