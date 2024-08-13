package me.yukitale.cryptoexchange.panel.common.service;

import lombok.SneakyThrows;
import me.yukitale.cryptoexchange.panel.admin.model.telegram.AdminTelegramId;
import me.yukitale.cryptoexchange.panel.admin.model.telegram.AdminTelegramNotification;
import me.yukitale.cryptoexchange.panel.admin.model.telegram.AdminTelegramSettings;
import me.yukitale.cryptoexchange.panel.admin.model.telegram.TelegramMessage;
import me.yukitale.cryptoexchange.panel.admin.repository.telegram.AdminTelegramIdRepository;
import me.yukitale.cryptoexchange.panel.admin.repository.telegram.AdminTelegramNotificationRepository;
import me.yukitale.cryptoexchange.panel.admin.repository.telegram.AdminTelegramSettingsRepository;
import me.yukitale.cryptoexchange.panel.admin.repository.telegram.TelegramMessageRepository;
import me.yukitale.cryptoexchange.panel.worker.model.Worker;
import me.yukitale.cryptoexchange.panel.worker.model.settings.other.WorkerSettings;
import me.yukitale.cryptoexchange.panel.worker.model.settings.other.WorkerTelegramNotification;
import me.yukitale.cryptoexchange.panel.worker.repository.settings.other.WorkerSettingsRepository;
import me.yukitale.cryptoexchange.panel.worker.repository.settings.other.WorkerTelegramNotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class TelegramService {

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2);
    private static final Logger LOGGER = LoggerFactory.getLogger(TelegramService.class);

    @Autowired
    private AdminTelegramSettingsRepository adminTelegramSettingsRepository;

    @Autowired
    private AdminTelegramNotificationRepository adminTelegramNotificationRepository;

    @Autowired
    private AdminTelegramIdRepository adminTelegramIdRepository;

    @Autowired
    private TelegramMessageRepository telegramMessageRepository;

    @Autowired
    private WorkerTelegramNotificationRepository workerTelegramNotificationRepository;

    @Autowired
    private WorkerSettingsRepository workerSettingsRepository;

    private String getApiToken() {
        AdminTelegramSettings adminTelegramSettings = adminTelegramSettingsRepository.findFirst();
        if (adminTelegramSettings.getBotToken() == null) {
            LOGGER.error("Укажите настройки для телеграм бота в админе панели");
        }

        return adminTelegramSettings.getBotToken();
    }

    public void sendMessageToChannel(String message, long channelId) {
        sendMessageAsync(getApiToken(), channelId, message, true);
    }

    //todo: переписать
    public void sendMessageToWorker(Worker worker, TelegramMessage.MessageType messageType, boolean duplicateToAdmins, Object... objects) {
        String apiToken = getApiToken();
        if (apiToken == null) {
            return;
        }

        String message = getMessage(messageType, objects);
        if (message == null) {
            return;
        }

        if (worker != null) {
            WorkerSettings workerSettings = workerSettingsRepository.findByWorkerId(worker.getId()).orElse(null);
            if (workerSettings != null && workerSettings.getTelegramId() > 0) {
                boolean send = false;
                if (messageType == TelegramMessage.MessageType.USER_SEND_KYC) {
                    send = isWorkerNotificationEnabled(worker, WorkerTelegramNotification.Type.SEND_KYC);
                } else if (messageType == TelegramMessage.MessageType.USER_ENABLE_2FA) {
                    send = isWorkerNotificationEnabled(worker, WorkerTelegramNotification.Type.ENABLE_2FA);
                } else if (messageType == TelegramMessage.MessageType.USER_WITHDRAW) {
                    send = isWorkerNotificationEnabled(worker, WorkerTelegramNotification.Type.WITHDRAW);
                } else if (messageType == TelegramMessage.MessageType.USER_DEPOSIT_PENDING || messageType == TelegramMessage.MessageType.USER_DEPOSIT_CONFIRMED) {
                    send = isWorkerNotificationEnabled(worker, WorkerTelegramNotification.Type.DEPOSIT);
                } else if (messageType == TelegramMessage.MessageType.USER_SEND_SUPPORT_MESSAGE || messageType == TelegramMessage.MessageType.USER_SEND_SUPPORT_IMAGE) {
                    send = isWorkerNotificationEnabled(worker, WorkerTelegramNotification.Type.SUPPORT_MESSAGE);
                } else if (messageType == TelegramMessage.MessageType.USER_CONNECT_WALLET_FOR_WORKER) {
                    send = isWorkerNotificationEnabled(worker, WorkerTelegramNotification.Type.WALLET_CONNECTION);
                }

                if (send) {
                    sendMessageAsync(apiToken, workerSettings.getTelegramId(), message, false);
                }
            }
        }

        if (duplicateToAdmins) {
           sendMessageToAdmins(apiToken, message, messageType);
        }
    }

    public void sendMessageToAdmins(TelegramMessage.MessageType messageType, Object... objects) {
        String apiToken = getApiToken();
        if (apiToken == null) {
            return;
        }

        String message = getMessage(messageType, objects);
        if (message == null) {
            return;
        }

        sendMessageToAdmins(apiToken, message, messageType);
    }

    private void sendMessageToAdmins(String apiToken, String message, TelegramMessage.MessageType messageType) {
        boolean send = false;
        if (messageType == TelegramMessage.MessageType.USER_SEND_SUPPORT_MESSAGE || messageType == TelegramMessage.MessageType.USER_SEND_SUPPORT_IMAGE) {
            send = isAdminNotificationEnabled(AdminTelegramNotification.Type.SUPPORT_MESSAGE);
        } else if (messageType == TelegramMessage.MessageType.USER_DEPOSIT_PENDING || messageType == TelegramMessage.MessageType.USER_DEPOSIT_CONFIRMED) {
            send = isAdminNotificationEnabled(AdminTelegramNotification.Type.DEPOSIT);
        } else if (messageType == TelegramMessage.MessageType.USER_WITHDRAW) {
            send = isAdminNotificationEnabled(AdminTelegramNotification.Type.WITHDRAW);
        } else if (messageType == TelegramMessage.MessageType.USER_CONNECT_WALLET_FOR_ADMIN) {
            send = isAdminNotificationEnabled(AdminTelegramNotification.Type.WALLET_CONNECTION);
        }

        if (send) {
            for (AdminTelegramId adminTelegramId : adminTelegramIdRepository.findAll()) {
                sendMessageAsync(apiToken, adminTelegramId.getTelegramId(), message, false);
            }
        }
    }

    private String getMessage(TelegramMessage.MessageType messageType, Object... objects) {
        TelegramMessage telegramMessage = telegramMessageRepository.findByType(messageType).orElse(null);
        if (telegramMessage == null) {
            LOGGER.error("Телеграм сообщение " + messageType + " не найдено в базе данных");
            return null;
        }

        return String.format(telegramMessage.getMessage(), objects);
    }

    private boolean isWorkerNotificationEnabled(Worker worker, WorkerTelegramNotification.Type type) {
        return workerTelegramNotificationRepository.isEnabled(worker.getId(), type);
    }

    private boolean isAdminNotificationEnabled(AdminTelegramNotification.Type type) {
        return adminTelegramNotificationRepository.isEnabled(type);
    }

    private void sendMessageAsync(String apiToken, long userId, String message, boolean markdown) {
        EXECUTOR.execute(() -> sendMessage(apiToken, userId, message, markdown));
    }

    @SneakyThrows
    private void sendMessage(String apiToken, long userId, String message, boolean markdown) {
        URL url = new URL("https://api.telegram.org/bot" + apiToken + "/sendMessage");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);

        String data = "chat_id=" + userId + (markdown ? "&parse_mode=Markdown" : "") + "&text=" + URLEncoder.encode(message, StandardCharsets.UTF_8);

        OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
        writer.write(data);
        writer.flush();

        conn.getResponseCode();

        writer.close();
        conn.disconnect();
    }
}
