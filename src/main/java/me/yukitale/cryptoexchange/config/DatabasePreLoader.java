package me.yukitale.cryptoexchange.config;

import me.yukitale.cryptoexchange.common.types.CoinType;
import me.yukitale.cryptoexchange.exchange.model.Coin;
import me.yukitale.cryptoexchange.exchange.model.user.User;
import me.yukitale.cryptoexchange.exchange.model.user.UserRole;
import me.yukitale.cryptoexchange.exchange.model.user.UserRoleType;
import me.yukitale.cryptoexchange.exchange.model.user.UserSupportDialog;
import me.yukitale.cryptoexchange.exchange.repository.CoinRepository;
import me.yukitale.cryptoexchange.exchange.repository.user.RoleRepository;
import me.yukitale.cryptoexchange.exchange.repository.user.UserRepository;
import me.yukitale.cryptoexchange.exchange.repository.user.UserSupportDialogRepository;
import me.yukitale.cryptoexchange.exchange.repository.user.UserSupportMessageRepository;
import me.yukitale.cryptoexchange.exchange.security.service.UserDetailsServiceImpl;
import me.yukitale.cryptoexchange.exchange.security.xss.XSSUtils;
import me.yukitale.cryptoexchange.panel.admin.model.coins.AdminCoinSettings;
import me.yukitale.cryptoexchange.panel.admin.model.coins.AdminDepositCoin;
import me.yukitale.cryptoexchange.panel.admin.model.other.*;
import me.yukitale.cryptoexchange.panel.admin.model.p2pfake.P2PFake;
import me.yukitale.cryptoexchange.panel.admin.model.payments.PaymentSettings;
import me.yukitale.cryptoexchange.panel.admin.model.telegram.AdminTelegramNotification;
import me.yukitale.cryptoexchange.panel.admin.model.telegram.AdminTelegramSettings;
import me.yukitale.cryptoexchange.panel.admin.model.telegram.TelegramMessage;
import me.yukitale.cryptoexchange.panel.admin.repository.coins.AdminCoinSettingsRepository;
import me.yukitale.cryptoexchange.panel.admin.repository.coins.AdminDepositCoinRepository;
import me.yukitale.cryptoexchange.panel.admin.repository.other.*;
import me.yukitale.cryptoexchange.panel.admin.repository.p2pfake.P2PFakeRepository;
import me.yukitale.cryptoexchange.panel.admin.repository.payments.PaymentSettingsRepository;
import me.yukitale.cryptoexchange.panel.admin.repository.telegram.AdminTelegramNotificationRepository;
import me.yukitale.cryptoexchange.panel.admin.repository.telegram.AdminTelegramSettingsRepository;
import me.yukitale.cryptoexchange.panel.admin.repository.telegram.TelegramMessageRepository;
import me.yukitale.cryptoexchange.panel.common.model.ErrorMessage;
import me.yukitale.cryptoexchange.panel.common.types.HomePageDesign;
import me.yukitale.cryptoexchange.panel.common.types.KycAcceptTimer;
import me.yukitale.cryptoexchange.utils.IOUtil;
import me.yukitale.cryptoexchange.utils.JsonUtil;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DatabasePreLoader implements ApplicationRunner {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CoinRepository coinRepository;

    @Autowired
    private AdminDepositCoinRepository adminDepositCoinRepository;

    @Autowired
    private PaymentSettingsRepository paymentSettingsRepository;

    @Autowired
    private AdminTelegramSettingsRepository adminTelegramSettingsRepository;

    @Autowired
    private AdminTelegramNotificationRepository adminTelegramNotificationRepository;

    @Autowired
    private TelegramMessageRepository telegramMessageRepository;

    @Autowired
    private AdminEmailSettingsRepository adminEmailSettingsRepository;

    @Autowired
    private AdminErrorMessageRepository adminErrorMessageRepository;

    @Autowired
    private AdminFeatureRepository adminFeatureRepository;

    @Autowired
    private AdminStakingPlanRepository adminStakingPlanRepository;

    @Autowired
    private AdminSupportPresetRepository adminSupportPresetRepository;

    @Autowired
    private AdminSettingsRepository adminSettingsRepository;

    @Autowired
    private AdminLegalSettingsRepository adminLegalSettingsRepository;

    @Autowired
    private AdminCoinSettingsRepository adminCoinSettingsRepository;

    @Autowired
    private P2PFakeRepository p2PFakeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSupportMessageRepository userSupportMessageRepository;

    @Autowired
    private UserSupportDialogRepository userSupportDialogRepository;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        createRoles();

        createCoins();
        createDepositCoins();

        createTransakSettings();

        createSupportPresets();

        createTelegramSettings();
        createTelegramNotifications();
        createTelegramMessages();

        createEmailSettings();
        createErrorMessages();
        createFeatures();
        createStakingPlans();
        createLegals();
        createSettings();

        createCoinSettings();

        createP2PFakes();

        //for update 18.09.2023
        createDefaultUserSupportDialogs();
    }

    //for update 18.09.2023
    private void createDefaultUserSupportDialogs() {
        if (userSupportDialogRepository.count() == 0) {
            List<Object[]> messages = userSupportMessageRepository.findAllOrderByCreateDesc();

            Map<Long, Map<String, Object>> dialogs = new HashMap<>();

            for (Object[] message : messages) {
                User user = (User) message[0];
                Date lastMessageDate = (Date) message[1];
                boolean supportViewed = (boolean) message[2];
                boolean userViewed = (boolean) message[3];

                Map<String, Object> dialog = dialogs.get(user.getId());
                if (dialog == null) {
                    dialog = new HashMap<>() {{
                        put("user", user);
                        put("last_message_date", lastMessageDate);
                        put("messages", 1);
                        put("support_unviewed", supportViewed ? 0 : 1);
                        put("user_unviewed", userViewed ? 0 : 1);
                    }};

                    dialogs.put(user.getId(), dialog);
                } else {
                    dialog.put("messages", ((int) dialog.get("messages")) + 1);
                    dialog.put("support_unviewed", ((int) dialog.get("support_unviewed")) + (supportViewed ? 0 : 1));
                    dialog.put("user_unviewed", ((int) dialog.get("user_unviewed")) + (userViewed ? 0 : 1));
                }
            }

            for (Map<String, Object> value : dialogs.values()) {
                User user = (User) value.get("user");
                Date lastMessageDate = (Date) value.get("last_message_date");
                int messagesCount = (int) value.get("messages");
                int supportUnviewed = (int) value.get("support_unviewed");
                int userUnviewed = (int) value.get("user_unviewed");

                UserSupportDialog supportDialog = new UserSupportDialog();
                supportDialog.setUser(user);
                supportDialog.setUserUnviewedMessages(userUnviewed);
                supportDialog.setSupportUnviewedMessages(supportUnviewed);
                supportDialog.setTotalMessages(messagesCount);
                supportDialog.setLastMessageDate(lastMessageDate);

                userSupportDialogRepository.save(supportDialog);
            }
        }
    }

    private void createCoinSettings() {
        if (adminCoinSettingsRepository.count() == 0) {
            AdminCoinSettings adminCoinSettings = new AdminCoinSettings();
            adminCoinSettings.setMinVerifAmount(300);
            adminCoinSettings.setMinWithdrawAmount(100);
            adminCoinSettings.setMinDepositAmount(50);
            adminCoinSettings.setDepositCommission(1.0);
            adminCoinSettings.setWithdrawCommission(1.0);
            adminCoinSettings.setVerifRequirement(false);
            adminCoinSettings.setVerifAml(true);

            adminCoinSettingsRepository.save(adminCoinSettings);
        }
    }

    private void createRoles() {
        for (UserRoleType type : UserRoleType.values()) {
            if (!roleRepository.existsByName(type)) {
                UserRole userRole = new UserRole(type);
                roleRepository.save(userRole);
            }
        }

        //todo: update 21.10.2023
        if (userRepository.countByRoleType(UserRoleType.ROLE_USER.ordinal()) == userRepository.count()) {
            for (Long supporterId : userRepository.findSupporterIds()) {
                User user = userRepository.findById(supporterId).orElse(null);
                if (user != null) {
                    user.setRoleType(UserRoleType.ROLE_SUPPORTER.ordinal());
                    userRepository.save(user);

                    userDetailsService.removeCache(user.getEmail());
                }
            }

            for (Long workerId : userRepository.findWorkerIds()) {
                User user = userRepository.findById(workerId).orElse(null);
                if (user != null) {
                    user.setRoleType(UserRoleType.ROLE_WORKER.ordinal());
                    userRepository.save(user);

                    userDetailsService.removeCache(user.getEmail());
                }
            }

            for (Long adminId : userRepository.findAdminIds()) {
                User user = userRepository.findById(adminId).orElse(null);
                if (user != null) {
                    user.setRoleType(UserRoleType.ROLE_ADMIN.ordinal());
                    userRepository.save(user);

                    userDetailsService.removeCache(user.getEmail());
                }
            }
        }
    }

    private void createCoins() {
        if (coinRepository.count() == 0) {
            String coinsJson = IOUtil.readResource("/data_preload/coins.json");

            List<Map<String, Object>> coinsMap = JsonUtil.readJson(coinsJson, List.class);

            int position = 1;
            for (Map<String, Object> coinMap : coinsMap) {
                Coin coin = new Coin();

                coin.setSymbol((String) coinMap.get("SYMBOL"));
                coin.setTitle((String) coinMap.get("TITLE"));
                coin.setIcon((String) coinMap.get("ICON"));
                coin.setMemo((Boolean) coinMap.getOrDefault("MEMO", false));
                coin.setPosition(position);

                coinRepository.save(coin);

                position++;
            }
        }
    }

    private void createDepositCoins() {
        if (adminDepositCoinRepository.count() == 0) {
            String depositCoinsJson = IOUtil.readResource("/data_preload/deposit_coins.json");

            List<Map<String, Object>> coinsMap = JsonUtil.readJson(depositCoinsJson, List.class);

            for (Map<String, Object> coinMap : coinsMap) {
                AdminDepositCoin coin = new AdminDepositCoin();

                double minReceiveAmount = Double.parseDouble(String.valueOf(coinMap.get("MIN_RECEIVE_AMOUNT")));

                coin.setType(CoinType.valueOf((String) coinMap.get("COIN_TYPE")));
                coin.setSymbol((String) coinMap.get("SYMBOL"));
                coin.setTitle((String) coinMap.get("TITLE"));
                coin.setIcon((String) coinMap.get("ICON"));
                coin.setMinReceiveAmount(minReceiveAmount);
                coin.setMinDepositAmount(minReceiveAmount);
                coin.setEnabled(true);

                adminDepositCoinRepository.save(coin);
            }
        }
    }

    //todo: recreating api key or setting custom api key
    private void createSettings() {
        if (adminSettingsRepository.count() == 0) {
            AdminSettings adminSettings = new AdminSettings();

            adminSettings.setSiteName(new String(new byte[]{89, 117, 107, 105, 84, 97, 108, 101}));
            adminSettings.setSiteTitle(new String(new byte[]{89, 117, 107, 105, 67, 111, 100, 101, 46, 100, 101, 118, 32, 124, 32, 67, 114, 121, 112, 116, 111, 99, 117, 114, 114, 101, 110, 99, 121, 32, 116, 114, 97, 100, 105, 110, 103, 32, 97, 110, 100, 32, 105, 110, 118, 101, 115, 116, 32, 112, 108, 97, 116, 102, 111, 114, 109}));
            adminSettings.setSiteIcon("../assets/media/logos/favicon.ico");
            adminSettings.setHomeDesign(HomePageDesign.DESIGN_1.ordinal());
            adminSettings.setKycAcceptTimer(KycAcceptTimer.TIMER_DISABLED);

            adminSettings.setSupportWelcomeMessage("Welcome to {domain_name}, if you have any questions you can ask here, our 24/7 support team will respond within a minute. We are always happy to help you.");
            adminSettings.setSupportWelcomeEnabled(true);

            adminSettings.setSupportPresetsEnabled(true);

            adminSettings.setPromoFormEnabled(true);
            adminSettings.setPromoHideEnabled(false);

            adminSettings.setShowAddressAlways(false);
            adminSettings.setShowQrAlways(false);

            adminSettings.setWorkerTopStats(true);
            adminSettings.setApiKey(RandomStringUtils.random(32, true, true));

            adminSettingsRepository.save(adminSettings);
        } else {
            AdminSettings adminSettings = adminSettingsRepository.findFirst();

            if (StringUtils.isBlank(adminSettings.getApiKey())) {
                adminSettings.setApiKey(RandomStringUtils.random(32, true, true));

                adminSettingsRepository.save(adminSettings);
            }
        }
    }

    private void createLegals() {
        if (adminLegalSettingsRepository.count() == 0) {
            String aml = IOUtil.readResource("/data_preload/aml.html");
            String terms = IOUtil.readResource("/data_preload/terms.html");
            String privacyNotice = IOUtil.readResource("/data_preload/privacy-notice.html");
            String regulatory = IOUtil.readResource("/data_preload/regulatory.html");
            String benefits = IOUtil.readResource("/data_preload/benefits.html");
            String recovery = IOUtil.readResource("/data_preload/recovery.html");

            AdminLegalSettings adminLegalSettings = new AdminLegalSettings();

            adminLegalSettings.setAml(XSSUtils.sanitize(aml));
            adminLegalSettings.setTerms(XSSUtils.sanitize(terms));
            adminLegalSettings.setPrivacyNotice(XSSUtils.sanitize(privacyNotice));
            adminLegalSettings.setRegulatory(XSSUtils.sanitize(regulatory));
            adminLegalSettings.setBenefits(XSSUtils.sanitize(benefits));
            adminLegalSettings.setRecovery(XSSUtils.sanitize(recovery));

            adminLegalSettingsRepository.save(adminLegalSettings);
        }
    }

    private void createStakingPlans() {
        if (adminStakingPlanRepository.count() == 0) {
            String stakingPlansJson = IOUtil.readResource("/data_preload/staking_plans.json");

            List<Map<String, Object>> stakingPlans = JsonUtil.readJson(stakingPlansJson, List.class);

            for (Map<String, Object> stakingPlan : stakingPlans) {
                String title = (String) stakingPlan.get("TITLE");
                int days = (int) stakingPlan.get("DAYS");
                double percent = (double) stakingPlan.get("PERCENT");

                AdminStakingPlan adminStakingPlan = new AdminStakingPlan();
                adminStakingPlan.setTitle(title);
                adminStakingPlan.setDays(days);
                adminStakingPlan.setPercent(percent);

                adminStakingPlanRepository.save(adminStakingPlan);
            }
        }
    }

    private void createSupportPresets() {
        if (adminSupportPresetRepository.count() == 0) {
            String supportPresetsJson = IOUtil.readResource("/data_preload/support_presets.json");

            Map<String, String> supportPresets = JsonUtil.readJson(supportPresetsJson, Map.class);

            for (Map.Entry<String, String> entry : supportPresets.entrySet()) {
                AdminSupportPreset adminSupportPreset = new AdminSupportPreset();
                adminSupportPreset.setTitle(entry.getKey());
                adminSupportPreset.setMessage(entry.getValue());

                adminSupportPresetRepository.save(adminSupportPreset);
            }
        }
    }

    private void createEmailSettings() {
        if (adminEmailSettingsRepository.count() == 0) {
            String registrationMessage = IOUtil.readResource("/data_preload/email_registration.html");
            String passwordRecoveryMessage = IOUtil.readResource("/data_preload/email_password_recovery.html");

            AdminEmailSettings adminEmailSettings = new AdminEmailSettings();
            adminEmailSettings.setEnabled(false);
            adminEmailSettings.setRequiredEnabled(false);
            adminEmailSettings.setDefaultServer("mail.privateemail.com");
            adminEmailSettings.setDefaultPort(465);
            adminEmailSettings.setRegistrationMessage(registrationMessage);
            adminEmailSettings.setPasswordRecoveryMessage(passwordRecoveryMessage);
            adminEmailSettings.setRegistrationTitle("{domain_exchange_name} - Confirmation of registration");
            adminEmailSettings.setPasswordRecoveryTitle("{domain_exchange_name} - Password recovery");

            adminEmailSettingsRepository.save(adminEmailSettings);
        }
    }

    private void createFeatures() {
        if (adminFeatureRepository.count() == 0) {
            for (AdminFeature.FeatureType type : AdminFeature.FeatureType.values()) {
                AdminFeature feature = new AdminFeature();
                feature.setType(type);
                feature.setEnabled(feature.getType().isDefaultValue());

                adminFeatureRepository.save(feature);
            }
        }
    }

    private void createErrorMessages() {
        if (adminErrorMessageRepository.count() == 0) {
            String errorMessagesJson = IOUtil.readResource("/data_preload/error_messages.json");

            Map<String, String> errorMessages = JsonUtil.readJson(errorMessagesJson, Map.class);

            for (Map.Entry<String, String> entry : errorMessages.entrySet()) {
                AdminErrorMessage errorMessage = new AdminErrorMessage();
                errorMessage.setType(ErrorMessage.ErrorMessageType.valueOf(entry.getKey()));
                errorMessage.setMessage(entry.getValue());

                adminErrorMessageRepository.save(errorMessage);
            }
        }
    }

    private void createTransakSettings() {
        if (paymentSettingsRepository.count() == 0) {
            PaymentSettings paymentSettings = new PaymentSettings();
            paymentSettings.setTransakEnabled(true);

            paymentSettingsRepository.save(paymentSettings);
        }
    }

    private void createTelegramSettings() {
        //update 03.10.2023
        String messagesJson = IOUtil.readResource("/data_preload/telegram_channel_messages.json");

        Map<String, String> messages = JsonUtil.readJson(messagesJson, Map.class);

        String depositMessage = messages.get("DEPOSIT");

        if (adminTelegramSettingsRepository.count() == 0) {
            AdminTelegramSettings telegramSettings = new AdminTelegramSettings();
            telegramSettings.setBotUsername(null);
            telegramSettings.setBotToken(null);
            telegramSettings.setChannelNotification(false);
            telegramSettings.setChannelMessage(depositMessage);

            adminTelegramSettingsRepository.save(telegramSettings);
        } else {
            //update 03.10.2023
            AdminTelegramSettings adminTelegramSettings = adminTelegramSettingsRepository.findFirst();

            if (StringUtils.isBlank(adminTelegramSettings.getChannelMessage())) {
                adminTelegramSettings.setChannelNotification(false);
                adminTelegramSettings.setChannelMessage(depositMessage);

                adminTelegramSettingsRepository.save(adminTelegramSettings);
            }
        }
    }

    private void createTelegramNotifications() {
        if (adminTelegramNotificationRepository.count() == 0) {
            for (AdminTelegramNotification.Type type : AdminTelegramNotification.Type.values()) {
                AdminTelegramNotification telegramNotification = new AdminTelegramNotification();
                telegramNotification.setType(type);
                telegramNotification.setEnabled(true);

                adminTelegramNotificationRepository.save(telegramNotification);
            }
        }
    }

    private void createTelegramMessages() {
        if (telegramMessageRepository.count() == 0) {
            String messagesJson = IOUtil.readResource("/data_preload/telegram_messages.json");

            Map<String, String> messages = JsonUtil.readJson(messagesJson, Map.class);

            for (Map.Entry<String, String> entry : messages.entrySet()) {
                TelegramMessage telegramMessage = new TelegramMessage();
                telegramMessage.setType(TelegramMessage.MessageType.valueOf(entry.getKey()));
                telegramMessage.setMessage(entry.getValue());

                telegramMessageRepository.save(telegramMessage);
            }
        }
    }

    private void createP2PFakes() {
        if (p2PFakeRepository.count() == 0) {
            String fakesJson = IOUtil.readResource("/data_preload/p2p_fakes.json");

            List<Map<String, String>> fakes = JsonUtil.readJson(fakesJson, List.class);

            for (Map<String, String> fake : fakes) {
                P2PFake p2PFake = new P2PFake();
                p2PFake.setUsername(fake.get("USERNAME"));
                p2PFake.setOrders(fake.get("ORDERS"));
                p2PFake.setLimits(fake.get("LIMITS"));
                p2PFake.setPaymentMethod(fake.get("PAYMENT_METHOD"));
                p2PFake.setAvatar(fake.get("AVATAR"));

                p2PFakeRepository.save(p2PFake);
            }
        }
    }
}
