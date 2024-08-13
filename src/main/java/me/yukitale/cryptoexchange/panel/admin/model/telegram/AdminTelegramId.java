package me.yukitale.cryptoexchange.panel.admin.model.telegram;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.*;

@Entity
@Table(name = "admin_telegram_ids")
@Getter
@Setter
@NoArgsConstructor
public class AdminTelegramId {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false, unique = true)
    private long telegramId;
}
