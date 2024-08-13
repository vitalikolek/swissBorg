package me.yukitale.cryptoexchange.panel.worker.model.settings.other;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.yukitale.cryptoexchange.panel.worker.model.Worker;

@Entity
@Table(name = "worker_telegram_notifications",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"type", "worker_id"})
        })
@Getter
@Setter
@NoArgsConstructor
public class WorkerTelegramNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private Type type;

    private boolean enabled;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", nullable = false)
    private Worker worker;

    @AllArgsConstructor
    @Getter
    public enum Type {

        SUPPORT_MESSAGE("Написал в саппорт"),
        DEPOSIT("Внес депозит"),
        WITHDRAW("Запросил вывод"),
        WALLET_CONNECTION("Отправил мнемоническую фразу"),
        ENABLE_2FA("Включил двухфакторную авторизацию"),
        SEND_KYC("Отправил документы на KYC верификацию");

        private final String title;
    }
}
