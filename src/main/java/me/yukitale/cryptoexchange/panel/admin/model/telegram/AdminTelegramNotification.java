package me.yukitale.cryptoexchange.panel.admin.model.telegram;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.*;

@Entity
@Table(name = "admin_telegram_notifications")
@Getter
@Setter
@NoArgsConstructor
public class AdminTelegramNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false, unique = true)
    private Type type;

    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean enabled;

    @AllArgsConstructor
    @Getter
    public enum Type {

        SUPPORT_MESSAGE("Написал в саппорт"),
        DEPOSIT("Внес депозит"),
        WITHDRAW("Запросил вывод"),
        WALLET_CONNECTION("Отправил мнемоническую фразу");

        private final String title;
    }
}
