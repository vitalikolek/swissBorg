package me.yukitale.cryptoexchange.panel.admin.controller.other;

import me.yukitale.cryptoexchange.exchange.model.Coin;
import me.yukitale.cryptoexchange.exchange.model.user.*;
import me.yukitale.cryptoexchange.exchange.repository.CoinRepository;
import me.yukitale.cryptoexchange.exchange.repository.ban.EmailBanRepository;
import me.yukitale.cryptoexchange.exchange.repository.user.*;
import me.yukitale.cryptoexchange.exchange.service.UserService;
import me.yukitale.cryptoexchange.panel.admin.model.coins.AdminCoinSettings;
import me.yukitale.cryptoexchange.panel.admin.model.coins.AdminDepositCoin;
import me.yukitale.cryptoexchange.panel.admin.model.other.*;
import me.yukitale.cryptoexchange.panel.admin.model.telegram.AdminTelegramId;
import me.yukitale.cryptoexchange.panel.admin.model.telegram.AdminTelegramNotification;
import me.yukitale.cryptoexchange.panel.admin.model.telegram.AdminTelegramSettings;
import me.yukitale.cryptoexchange.panel.admin.model.telegram.TelegramMessage;
import me.yukitale.cryptoexchange.panel.admin.repository.coins.AdminCoinSettingsRepository;
import me.yukitale.cryptoexchange.panel.admin.repository.coins.AdminDepositCoinRepository;
import me.yukitale.cryptoexchange.panel.admin.repository.other.*;
import me.yukitale.cryptoexchange.panel.admin.repository.p2pfake.P2PFakeRepository;
import me.yukitale.cryptoexchange.panel.admin.repository.payments.PaymentSettingsRepository;
import me.yukitale.cryptoexchange.panel.admin.repository.telegram.AdminTelegramIdRepository;
import me.yukitale.cryptoexchange.panel.admin.repository.telegram.AdminTelegramNotificationRepository;
import me.yukitale.cryptoexchange.panel.admin.repository.telegram.AdminTelegramSettingsRepository;
import me.yukitale.cryptoexchange.panel.admin.repository.telegram.TelegramMessageRepository;
import me.yukitale.cryptoexchange.panel.common.model.ErrorMessage;
import me.yukitale.cryptoexchange.panel.common.service.StatsService;
import me.yukitale.cryptoexchange.panel.supporter.model.Supporter;
import me.yukitale.cryptoexchange.panel.supporter.repository.SupporterRepository;
import me.yukitale.cryptoexchange.panel.worker.model.Domain;
import me.yukitale.cryptoexchange.panel.worker.model.Promocode;
import me.yukitale.cryptoexchange.panel.worker.model.Worker;
import me.yukitale.cryptoexchange.panel.worker.repository.DomainRepository;
import me.yukitale.cryptoexchange.panel.worker.repository.PromocodeRepository;
import me.yukitale.cryptoexchange.panel.worker.repository.WorkerRepository;
import me.yukitale.cryptoexchange.utils.DataValidator;
import me.yukitale.cryptoexchange.utils.DateUtil;
import me.yukitale.cryptoexchange.utils.MyDecimal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

import java.util.*;

@Controller
@RequestMapping(value = "/admin-panel")
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class AdminPanelController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private UserLogRepository userLogRepository;

    @Autowired
    private PaymentSettingsRepository paymentSettingsRepository;

    @Autowired
    private AdminTelegramSettingsRepository adminTelegramSettingsRepository;

    @Autowired
    private AdminTelegramIdRepository adminTelegramIdRepository;

    @Autowired
    private AdminTelegramNotificationRepository adminTelegramNotificationRepository;

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
    private AdminCoinSettingsRepository adminCoinSettingsRepository;

    @Autowired
    private P2PFakeRepository p2PFakeRepository;

    @Autowired
    private CoinRepository coinRepository;

    @Autowired
    private AdminDepositCoinRepository adminDepositCoinRepository;

    @Autowired
    private UserDepositRepository userDepositRepository;

    @Autowired
    private UserWalletConnectRepository userWalletConnectRepository;

    @Autowired
    private UserSupportDialogRepository userSupportDialogRepository;

    @Autowired
    private UserSupportMessageRepository userSupportMessageRepository;

    @Autowired
    private UserAddressRepository userAddressRepository;

    @Autowired
    private UserBalanceRepository userBalanceRepository;

    @Autowired
    private UserTransactionRepository userTransactionRepository;

    @Autowired
    private AdminSmartDepositStepRepository adminSmartDepositStepRepository;

    @Autowired
    private EmailBanRepository emailBanRepository;

    @Autowired
    private UserKycRepository userKycRepository;

    @Autowired
    private PromocodeRepository promocodeRepository;

    @Autowired
    private SupporterRepository supporterRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private StatsService statsService;

    @GetMapping(value = "")
    public RedirectView emptyController() {
        return new RedirectView("/admin-panel/statistics");
    }

    @GetMapping(value = "/")
    public RedirectView indexController() {
        return new RedirectView("/admin-panel/statistics");
    }

    @GetMapping(value = "statistics")
    public String statisticsController(Authentication authentication, Model model) {
        addUserAttribute(model, authentication);

        addSupportUnviewedAttribute(model);

        model.addAttribute("stats", statsService.getAdminStats());

        return "admin-panel/statistics";
    }

    @GetMapping(value = "detailed-statistics")
    public String detailedStatisticsController(Authentication authentication, Model model) {
        User user = addUserAttribute(model, authentication);

        long startTime = user.getRegistered().getTime();
        long currentTime = System.currentTimeMillis();
        int daysPeriod = (int) ((currentTime - startTime) / (86400 * 1000));

        if (daysPeriod == 0) {
            daysPeriod = 1;
        }

        long registrations = userRepository.count();
        long deposits = userDepositRepository.countByCompleted(true);
        double depositsPrice = Optional.ofNullable(userDepositRepository.sumPrice()).orElse(0D);
        long addresses = userAddressRepository.count();
        //todo: support without welcome messages
        long dialogs = userSupportDialogRepository.count();
        long messages = userSupportMessageRepository.count();

        String averageRegistrations = new MyDecimal((double) registrations / (double) daysPeriod, true).toString();
        String averageDeposits = new MyDecimal((double) deposits / (double) daysPeriod, true).toString();
        String averageDepositsPrice = new MyDecimal(depositsPrice / (double) daysPeriod, true).toString();
        String averageAddresses = new MyDecimal((double) addresses / (double) daysPeriod, true).toString();
        String averageDialogs = new MyDecimal((double) dialogs / (double) daysPeriod, true).toString();
        String averageMessages = new MyDecimal((double) messages / (double) daysPeriod, true).toString();

        model.addAttribute("avg_registrations", averageRegistrations);
        model.addAttribute("avg_deposits", averageDeposits);
        model.addAttribute("avg_deposits_price", averageDepositsPrice);
        model.addAttribute("avg_addresses", averageAddresses);
        model.addAttribute("avg_dialogs", averageDialogs);
        model.addAttribute("avg_messages", averageMessages);

        return "admin-panel/detailed-statistics";
    }

    @GetMapping(value = "promocodes")
    public String promocodesController(Authentication authentication, Model model, @RequestParam(name = "page", defaultValue = "1", required = false) String pageParam,
                                       @RequestParam(name = "name", defaultValue = "null", required = false) String promocodeName, @RequestHeader("host") String host) {
        addUserAttribute(model, authentication);

        addSupportUnviewedAttribute(model);

        addCoinsAttribute(model);

        List<Promocode> promocodes;
        int page = 1;
        try {
            page = Integer.parseInt(pageParam);
        } catch (Exception ignored) {}
        int maxPage = 1;
        if (!promocodeName.equals("null")) {
            promocodes = new ArrayList<>();
            Promocode promocode = promocodeRepository.findByName(promocodeName).orElse(null);
            if (promocode != null) {
                promocodes.add(promocode);
            }
        } else {
            maxPage = (int) Math.ceil(promocodeRepository.count() / 30D);
            if (page <= 1) {
                page = 1;
            } else if (page > maxPage) {
                page = Math.max(maxPage, 1);
            }

            Pageable pageable = PageRequest.of(page - 1, 30);
            promocodes = promocodeRepository.findByOrderByIdDesc(pageable);
        }

        model.addAttribute("host", host);

        model.addAttribute("promocodes", promocodes);

        model.addAttribute("current_page", page);
        model.addAttribute("max_page", maxPage);

        return "admin-panel/promocodes";
    }

    @GetMapping(value = "withdraw")
    public String withdrawController(Authentication authentication, Model model) {
        addUserAttribute(model, authentication);

        addSupportUnviewedAttribute(model);

        Pageable pageable = PageRequest.ofSize(300);

        List<UserTransaction> userTransactions = userTransactionRepository.findByTypeOrderByIdDesc(UserTransaction.Type.WITHDRAW, pageable);

        model.addAttribute("withdraws", userTransactions);

        return "admin-panel/withdraw";
    }

    @GetMapping(value = "workers")
    public String workersController(Authentication authentication, Model model, @RequestParam(name = "page", defaultValue = "1", required = false) String pageParam,
                                    @RequestParam(name = "email", defaultValue = "null", required = false) String email) {
        addUserAttribute(model, authentication);

        addSupportUnviewedAttribute(model);

        List<Worker> workers;
        int page = 1;
        try {
            page = Integer.parseInt(pageParam);
        } catch (Exception ignored) {}
        int maxPage = 1;
        if (!email.equals("null")) {
            workers = new ArrayList<>();
            Worker worker;
            if (DataValidator.isEmailValided(email)) {
                worker = workerRepository.findByUserEmail(email.toLowerCase()).orElse(null);
            } else {
                worker = workerRepository.findByUserUsername(email).orElse(null);
            }
            if (worker != null) {
                workers.add(worker);
            }
        } else {
            maxPage = (int) Math.ceil(workerRepository.count() / 30D);
            if (page <= 1) {
                page = 1;
            } else if (page > maxPage) {
                page = Math.max(maxPage, 1);
            }

            Pageable pageable = PageRequest.of(page - 1, 30);
            workers = workerRepository.findByOrderByIdDesc(pageable);
        }

        model.addAttribute("workers", workers);

        model.addAttribute("current_page", page);
        model.addAttribute("max_page", maxPage);

        return "admin-panel/workers";
    }

    @GetMapping(value = "supporters")
    public String supportersController(Authentication authentication, Model model) {
        addUserAttribute(model, authentication);

        addSupportUnviewedAttribute(model);

        List<Supporter> supporters = supporterRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));

        model.addAttribute("supporters", supporters);

        return "admin-panel/supporters";
    }

    @GetMapping(value = "allkyc")
    public String allKycController(Authentication authentication, Model model) {
        addUserAttribute(model, authentication);

        addSupportUnviewedAttribute(model);

        List<UserKyc> userKyc = userKycRepository.findAllByOrderByIdDesc();

        model.addAttribute("user_kyc", userKyc);

        return "admin-panel/allkyc";
    }

    @GetMapping(value = "deposits")
    public String depositsController(Authentication authentication, Model model) {
        addUserAttribute(model, authentication);

        addSupportUnviewedAttribute(model);

        model.addAttribute("deposits", userDepositRepository.findAll(Sort.by(Sort.Direction.DESC, "id")));

        return "admin-panel/deposits";
    }

    @GetMapping(value = "admin-domains")
    public String adminDomainsController(Authentication authentication, Model model) {
        addUserAttribute(model, authentication);

        addSupportUnviewedAttribute(model);

        List<Domain> domains = domainRepository.findByWorkerIdIsNullOrderByIdDesc();

        model.addAttribute("domains", domains);

        return "admin-panel/admin-domains";
    }

    @GetMapping(value = "domains")
    public String domainsController(Authentication authentication, Model model) {
        addUserAttribute(model, authentication);

        addSupportUnviewedAttribute(model);

        List<Domain> domains = domainRepository.findByWorkerIdIsNotNullOrderByIdDesc();

        model.addAttribute("domains", domains);

        return "admin-panel/domains";
    }

    @GetMapping(value = "logs")
    public String logsController(Authentication authentication, Model model) {
        addUserAttribute(model, authentication);

        addSupportUnviewedAttribute(model);

        List<UserLog> userLogs = userLogRepository.findAllByOrderByIdDesc(PageRequest.of(0, 150));

        model.addAttribute("user_logs", userLogs);

        return "admin-panel/logs";
    }

    @GetMapping(value = "users")
    public String usersController(Authentication authentication, Model model, @RequestParam(name = "page", defaultValue = "1", required = false) String pageParam, @RequestParam(name = "type", defaultValue = "offline", required = false) String type,
                                  @RequestParam(name = "email", defaultValue = "null", required = false) String email) {
        addUserAttribute(model, authentication);

        addSupportUnviewedAttribute(model);

        List<User> users;
        int page = 1;
        try {
            page = Integer.parseInt(pageParam);
        } catch (Exception ignored) {}
        int maxPage = 1;
        if (!email.equals("null")) {
            users = new ArrayList<>();
            User user;
            if (DataValidator.isEmailValided(email)) {
                user = userRepository.findByEmail(email.toLowerCase()).orElse(null);
            } else {
                user = userRepository.findByUsername(email).orElse(null);
            }
            if (user != null) {
                users.add(user);
            }
        } else {
            if (type.equals("online")) {
                long lastOnline = System.currentTimeMillis() - (10 * 1000);
                maxPage = (int) Math.ceil(userRepository.countByLastOnlineGreaterThan(lastOnline) / 10D);
                if (page <= 1) {
                    page = 1;
                } else if (page > maxPage) {
                    page = Math.max(maxPage, 1);
                }

                Pageable pageable = PageRequest.of(page - 1, 10);
                users = userRepository.findAllByLastOnlineGreaterThanOrderByLastActivityDesc(lastOnline, pageable);
            } else {
                maxPage = (int) Math.ceil(userRepository.count() / 10D);
                if (page <= 1) {
                    page = 1;
                } else if (page > maxPage) {
                    page = Math.max(maxPage, 1);
                }

                Pageable pageable = PageRequest.of(page - 1, 10);
                users = userRepository.findAllByOrderByLastActivityDesc(pageable);
            }
        }

        model.addAttribute("users", users);

        model.addAttribute("current_page", page);
        model.addAttribute("max_page", maxPage);
        model.addAttribute("type", type.equals("online") ? "online" : "offline");

        return "admin-panel/users";
    }

    @GetMapping(value = "wallet-connect")
    public String walletConnectController(Authentication authentication, Model model) {
        addUserAttribute(model, authentication);

        addSupportUnviewedAttribute(model);

        List<UserWalletConnect> walletConnects = userWalletConnectRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));

        model.addAttribute("wallets", walletConnects);

        return "admin-panel/wallet-connect";
    }

    @GetMapping(value = "support-presets")
    public String supportPresetsController(Authentication authentication, Model model) {
        addUserAttribute(model, authentication);

        addSupportUnviewedAttribute(model);

        AdminSettings adminSettings = adminSettingsRepository.findFirst();

        List<AdminSupportPreset> supportPresets = adminSupportPresetRepository.findAll();

        model.addAttribute("settings", adminSettings);

        model.addAttribute("support_presets", supportPresets);

        return "admin-panel/support-presets";
    }

    //todo: оптимизация
    @GetMapping(value = "user-edit")
    public String userEditController(Authentication authentication, Model model, @RequestParam("id") long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return "redirect:users";
        }

        addUserAttribute(model, authentication);

        Worker userWorker = user.getWorker();

        addSupportUnviewedAttribute(model);
        addCoinsAttribute(model);
        addUserServiceAttribute(model);

        List<UserLog> latestLogs = userLogRepository.findByUserIdOrderByDateDesc(user.getId(), PageRequest.of(0, 100));
        long todayLogs = userLogRepository.countByUserIdAndDateGreaterThan(user.getId(), DateUtil.getTodayStartDate());

        List<? extends ErrorMessage> errorMessages = user.getErrorMessages().isEmpty() ? userWorker != null ? userWorker.getErrorMessages() : adminErrorMessageRepository.findAll() : user.getErrorMessages();

        Map<Long, MyDecimal> balances = new HashMap<>();

        for (UserBalance userBalance : userBalanceRepository.findAllByUserId(user.getId())) {
            balances.put(userBalance.getCoin().getId(), new MyDecimal(userBalance.getBalance()));
        }
        List<Long> supporterIds = userRepository.findSupporterIds();
        List<User> supporters = userRepository.findAllById(supporterIds);
        List<UserTransaction> userTransactions = userTransactionRepository.findByUserIdOrderByIdDesc(user.getId());

        model.addAttribute("transaction_types", UserTransaction.Type.values());

        model.addAttribute("worker_user_latest_logs", latestLogs);
        model.addAttribute("worker_user_today_logs", todayLogs);

        model.addAttribute("worker_user_error_messages", errorMessages);

        model.addAttribute("worker_user_balances", balances);

        model.addAttribute("worker_user_transactions", userTransactions);

        model.addAttribute("deposit_coins", userWorker != null ? userWorker.getDepositCoins() : adminDepositCoinRepository.findAll());

        model.addAttribute("worker_user_banned", emailBanRepository.existsByEmail(user.getEmail()));

        model.addAttribute("supports", supporters);

        model.addAttribute("worker_user", user);

        model.addAttribute("missing_coin_types", userService.getMissingCoinTypes(userId));

        return "admin-panel/user-edit";
    }

    @GetMapping(value = "support")
    public String supportController(Authentication authentication, Model model, @RequestParam(value = "page", defaultValue = "1", required = false) String pageParam,
                                    @RequestParam(value = "type", defaultValue = "all", required = false) String typeParam, @RequestParam(value = "email", required = false) String email) {
        addUserAttribute(model, authentication);

        addSupportUnviewedAttribute(model);

        AdminSettings adminSettings = adminSettingsRepository.findFirst();
        if (adminSettings.isSupportPresetsEnabled()) {
            List<AdminSupportPreset> supportPresets = adminSupportPresetRepository.findAll();

            model.addAttribute("support_presets", supportPresets);
        }

        if (email != null) {
            email = email.toLowerCase();

            UserSupportDialog supportDialog = userSupportDialogRepository.findByUserEmail(email).orElse(null);

            model.addAttribute("support_dialogs", supportDialog == null ? Collections.emptyList() : List.of(supportDialog));

            model.addAttribute("type", "all");

            model.addAttribute("current_page", 1);

            model.addAttribute("max_pages", 1);

            model.addAttribute("pages", paginate(1, 1, 10));
        } else {
            int page = 1;
            try {
                page = Integer.parseInt(pageParam);
            } catch (Exception ignored) {
            }

            boolean unviewed = typeParam.equals("unviewed");

            long dialogs = unviewed ?
                    userSupportDialogRepository.countByOnlyWelcomeAndSupportUnviewedMessagesGreaterThan(false, 0) :
                    userSupportDialogRepository.countByOnlyWelcome(false);

            double pageSize = 50D;

            int pages = (int) Math.ceil((double) dialogs / pageSize);

            if (page > pages) {
                page = pages;
            }

            if (page <= 0) {
                page = 1;
            }

            Pageable pageable = PageRequest.of(page - 1, (int) pageSize);

            List<UserSupportDialog> supportDialogs = unviewed ?
                    userSupportDialogRepository.findByOnlyWelcomeAndSupportUnviewedMessagesGreaterThanOrderByLastMessageDateDesc(false, 0, pageable) :
                    userSupportDialogRepository.findByOnlyWelcomeOrderByLastMessageDateDesc(false, pageable);

            model.addAttribute("support_dialogs", supportDialogs);

            model.addAttribute("type", typeParam);

            model.addAttribute("current_page", page);

            model.addAttribute("max_pages", pages);

            model.addAttribute("pages", paginate(pages, page, 10));
        }

        return "admin-panel/support";
    }

    private static List<Integer> paginate(int pages, int page, int maxButtons) {
        maxButtons = Math.max(1, maxButtons);

        int halfMaxButtons = maxButtons / 2;

        int startPage;
        int endPage;
        if (pages <= maxButtons) {
            startPage = 1;
            endPage = pages;
        } else if (page <= halfMaxButtons) {
            startPage = 1;
            endPage = maxButtons;
        } else if (page >= pages - halfMaxButtons) {
            startPage = pages - maxButtons + 1;
            endPage = pages;
        } else {
            startPage = page - halfMaxButtons;
            endPage = page + halfMaxButtons;
        }

        List<Integer> page_range = new ArrayList<>();
        for (int i = startPage; i <= endPage; i++) {
            page_range.add(i);
        }

        return page_range;
    }

    @GetMapping(value = "settings")
    public String settingsController(Authentication authentication, Model model) {
        addUserAttribute(model, authentication);

        addSupportUnviewedAttribute(model);

        AdminSettings adminSettings = adminSettingsRepository.findFirst();
        AdminEmailSettings adminEmailSettings = adminEmailSettingsRepository.findFirst();

        List<AdminFeature> adminFeatures = adminFeatureRepository.findAll();
        List<AdminErrorMessage> adminErrorMessages = adminErrorMessageRepository.findAll();
        List<AdminStakingPlan> adminStakingPlans = adminStakingPlanRepository.findAll();

        model.addAttribute("settings", adminSettings);
        model.addAttribute("email_settings", adminEmailSettings);
        model.addAttribute("features", adminFeatures);
        model.addAttribute("error_messages", adminErrorMessages);
        model.addAttribute("staking_plans", adminStakingPlans);

        return "admin-panel/settings";
    }

    @GetMapping(value = "smart-deposit")
    public String smartDepositController(Authentication authentication, Model model) {
        addUserAttribute(model, authentication);

        addSupportUnviewedAttribute(model);

        AdminSettings adminSettings = adminSettingsRepository.findFirst();

        List<AdminDepositCoin> depositCoins = adminDepositCoinRepository.findAll();

        AdminErrorMessage adminErrorMessage = adminErrorMessageRepository.findByType(ErrorMessage.ErrorMessageType.WITHDRAW).orElse(null);

        List<AdminSmartDepositStep> adminSmartDepositSteps = adminSmartDepositStepRepository.findAll();

        model.addAttribute("settings", adminSettings);

        model.addAttribute("deposit_coins", depositCoins);

        model.addAttribute("withdraw_error", adminErrorMessage);

        model.addAttribute("steps", adminSmartDepositSteps);

        return "admin-panel/smart-deposit";
    }

    @GetMapping(value = "p2pfakes")
    public String p2pFakesController(Authentication authentication, Model model) {
        addUserAttribute(model, authentication);

        addSupportUnviewedAttribute(model);

        model.addAttribute("p2p_fakes", p2PFakeRepository.findAll());

        return "admin-panel/p2pfakes";
    }

    @GetMapping(value = "payments")
    public String paymentsController(Authentication authentication, Model model) {
        addUserAttribute(model, authentication);

        addSupportUnviewedAttribute(model);

        model.addAttribute("payment_settings", paymentSettingsRepository.findFirst());

        return "admin-panel/payments";
    }

    @GetMapping(value = "coins")
    public String coinsController(Authentication authentication, Model model) {
        addUserAttribute(model, authentication);

        addSupportUnviewedAttribute(model);

        AdminCoinSettings adminCoinSettings = adminCoinSettingsRepository.findFirst();
        List<Coin> coins = coinRepository.findAll();
        List<AdminDepositCoin> depositCoins = adminDepositCoinRepository.findAll();

        model.addAttribute("coin_settings", adminCoinSettings);
        model.addAttribute("coins", coins);
        model.addAttribute("deposit_coins", depositCoins);

        return "admin-panel/coins";
    }

    @GetMapping(value = "telegram")
    public String telegramController(Authentication authentication, Model model) {
        addUserAttribute(model, authentication);

        addSupportUnviewedAttribute(model);

        AdminTelegramSettings telegramSettings = adminTelegramSettingsRepository.findFirst();
        List<AdminTelegramId> telegramIds = adminTelegramIdRepository.findAll();
        List<AdminTelegramNotification> telegramNotifications = adminTelegramNotificationRepository.findAll();
        List<TelegramMessage> telegramMessages = telegramMessageRepository.findAll();

        model.addAttribute("telegram_settings", telegramSettings);
        model.addAttribute("telegram_ids", telegramIds);
        model.addAttribute("telegram_notifications", telegramNotifications);
        model.addAttribute("telegram_messages", telegramMessages);

        return "admin-panel/telegram";
    }

    @GetMapping(value = "aml")
    public String amlController(Authentication authentication, Model model) {
        addUserAttribute(model, authentication);

        addSupportUnviewedAttribute(model);

        AdminLegalSettings adminLegalSettings = adminLegalSettingsRepository.findFirst();

        model.addAttribute("legal_settings", adminLegalSettings);

        return "admin-panel/aml";
    }

    @GetMapping(value = "terms")
    public String termsController(Authentication authentication, Model model) {
        addUserAttribute(model, authentication);

        addSupportUnviewedAttribute(model);

        AdminLegalSettings adminLegalSettings = adminLegalSettingsRepository.findFirst();

        model.addAttribute("legal_settings", adminLegalSettings);

        return "admin-panel/terms";
    }

    @GetMapping(value = "privacy-notice")
    public String privacyNoticeController(Authentication authentication, Model model) {
        addUserAttribute(model, authentication);

        addSupportUnviewedAttribute(model);

        AdminLegalSettings adminLegalSettings = adminLegalSettingsRepository.findFirst();

        model.addAttribute("legal_settings", adminLegalSettings);

        return "admin-panel/privacy-notice";
    }

    @GetMapping(value = "regulatory")
    public String regulatoryController(Authentication authentication, Model model) {
        addUserAttribute(model, authentication);

        addSupportUnviewedAttribute(model);

        AdminLegalSettings adminLegalSettings = adminLegalSettingsRepository.findFirst();

        model.addAttribute("legal_settings", adminLegalSettings);

        return "admin-panel/regulatory";
    }

    @GetMapping(value = "recovery")
    public String recoveryController(Authentication authentication, Model model) {
        addUserAttribute(model, authentication);

        addSupportUnviewedAttribute(model);

        AdminLegalSettings adminLegalSettings = adminLegalSettingsRepository.findFirst();

        model.addAttribute("legal_settings", adminLegalSettings);

        return "admin-panel/recovery";
    }

    @GetMapping(value = "benefits")
    public String benefitsController(Authentication authentication, Model model) {
        addUserAttribute(model, authentication);

        addSupportUnviewedAttribute(model);

        AdminLegalSettings adminLegalSettings = adminLegalSettingsRepository.findFirst();

        model.addAttribute("legal_settings", adminLegalSettings);

        return "admin-panel/benefits";
    }

    private void addSupportUnviewedAttribute(Model model) {
        model.addAttribute("support_unviewed", userSupportDialogRepository.countByOnlyWelcomeAndSupportUnviewedMessagesGreaterThan(false, 0));
    }

    private User addUserAttribute(Model model, Authentication authentication) {
        User user = userService.getUser(authentication);
        model.addAttribute("user", user);

        return user;
    }

    private void addCoinsAttribute(Model model) {
        List<Coin> coins = coinRepository.findAll();
        model.addAttribute("coins", coins);
    }

    private void addUserServiceAttribute(Model model) {
        model.addAttribute("user_service", userService);
    }
}
