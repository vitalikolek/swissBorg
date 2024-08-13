package me.yukitale.cryptoexchange.panel.worker.controller.other;

import me.yukitale.cryptoexchange.exchange.model.user.*;
import me.yukitale.cryptoexchange.exchange.repository.ban.EmailBanRepository;
import me.yukitale.cryptoexchange.exchange.repository.user.*;
import me.yukitale.cryptoexchange.exchange.service.UserService;
import me.yukitale.cryptoexchange.panel.admin.model.other.AdminLegalSettings;
import me.yukitale.cryptoexchange.panel.admin.repository.other.AdminErrorMessageRepository;
import me.yukitale.cryptoexchange.panel.admin.repository.other.AdminLegalSettingsRepository;
import me.yukitale.cryptoexchange.panel.admin.repository.other.AdminSettingsRepository;
import me.yukitale.cryptoexchange.panel.common.model.ErrorMessage;
import me.yukitale.cryptoexchange.panel.common.service.StatsService;
import me.yukitale.cryptoexchange.panel.worker.model.settings.coins.WorkerDepositCoin;
import me.yukitale.cryptoexchange.panel.worker.model.settings.other.WorkerRecordSettings;
import me.yukitale.cryptoexchange.panel.worker.model.settings.other.WorkerSettings;
import me.yukitale.cryptoexchange.panel.worker.model.settings.other.WorkerSmartDepositStep;
import me.yukitale.cryptoexchange.panel.worker.model.settings.other.WorkerSupportPreset;
import me.yukitale.cryptoexchange.panel.worker.repository.DomainRepository;
import me.yukitale.cryptoexchange.panel.worker.repository.FastPumpRepository;
import me.yukitale.cryptoexchange.panel.worker.repository.settings.other.WorkerErrorMessageRepository;
import me.yukitale.cryptoexchange.panel.worker.repository.settings.other.WorkerRecordSettingsRepository;
import me.yukitale.cryptoexchange.panel.worker.repository.settings.other.WorkerSmartDepositStepsRepository;
import me.yukitale.cryptoexchange.panel.worker.repository.settings.other.WorkerSupportPresetsRepository;
import me.yukitale.cryptoexchange.utils.DataValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;
import me.yukitale.cryptoexchange.exchange.model.Coin;
import me.yukitale.cryptoexchange.exchange.repository.CoinRepository;
import me.yukitale.cryptoexchange.exchange.service.CoinService;
import me.yukitale.cryptoexchange.panel.worker.model.Domain;
import me.yukitale.cryptoexchange.panel.worker.model.Worker;
import me.yukitale.cryptoexchange.panel.worker.service.WorkerService;
import me.yukitale.cryptoexchange.utils.DateUtil;
import me.yukitale.cryptoexchange.utils.MyDecimal;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping(value = "/worker-panel")
@PreAuthorize("hasRole('ROLE_WORKER')")
public class WorkerPanelController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private UserLogRepository userLogRepository;

    @Autowired
    private CoinRepository coinRepository;

    @Autowired
    private UserKycRepository kycRepository;

    @Autowired
    private FastPumpRepository fastPumpRepository;

    @Autowired
    private AdminSettingsRepository adminSettingsRepository;

    @Autowired
    private AdminLegalSettingsRepository adminLegalSettingsRepository;

    @Autowired
    private UserSupportDialogRepository userSupportDialogRepository;

    @Autowired
    private UserTransactionRepository userTransactionRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private UserDepositRepository userDepositRepository;

    @Autowired
    private UserWalletConnectRepository userWalletConnectRepository;

    @Autowired
    private UserBalanceRepository userBalanceRepository;

    @Autowired
    private UserAddressRepository userAddressRepository;

    @Autowired
    private UserSupportMessageRepository userSupportMessageRepository;

    @Autowired
    private EmailBanRepository emailBanRepository;

    @Autowired
    private WorkerSupportPresetsRepository workerSupportPresetsRepository;

    @Autowired
    private WorkerRecordSettingsRepository workerRecordSettingsRepository;

    @Autowired
    private WorkerSmartDepositStepsRepository workerSmartDepositStepsRepository;

    @Autowired
    private AdminErrorMessageRepository adminErrorMessageRepository;

    @Autowired
    private WorkerErrorMessageRepository workerErrorMessageRepository;

    @Autowired
    private WorkerService workerService;

    @Autowired
    private CoinService coinService;

    @Autowired
    private StatsService statsService;

    @GetMapping(value = "")
    public RedirectView emptyController() {
        return new RedirectView("/worker-panel/binding");
    }

    @GetMapping(value = "/")
    public RedirectView indexController() {
        return new RedirectView("/worker-panel/binding");
    }

    @GetMapping(value = "binding")
    public String bindingController(Authentication authentication, Model model, @RequestHeader("host") String host) {
        User user = addUserAttribute(model, authentication);
        addCoinsAttribute(model);

        Worker worker = workerService.getWorker(user);

        addWorkerSupportUnviewedAttribute(model, worker);

        List<Domain> domains = worker.getDomains();
        String domainsLine = domains.isEmpty() ? "No domains" : domains.stream().map(Domain::getName).collect(Collectors.joining(", "));

        model.addAttribute("host", host);
        model.addAttribute("worker", worker);
        model.addAttribute("domains", domainsLine);

        return "worker-panel/binding";
    }

    @GetMapping(value = "statistics")
    public String statisticsController(Authentication authentication, Model model) {
        User user = addUserAttribute(model, authentication);

        Worker worker = workerService.getWorker(user);

        addWorkerSupportUnviewedAttribute(model, worker);

        model.addAttribute("stats", statsService.getWorkerStats(worker));

        model.addAttribute("settings", adminSettingsRepository.findFirst());

        return "worker-panel/statistics";
    }

    @GetMapping(value = "detailed-statistics")
    public String detailedStatisticsController(Authentication authentication, Model model) {
        User user = addUserAttribute(model, authentication);

        Worker worker = workerService.getWorker(user);

        addWorkerSupportUnviewedAttribute(model, worker);

        long startTime = user.getRegistered().getTime();
        long currentTime = System.currentTimeMillis();
        int daysPeriod = (int) ((currentTime - startTime) / (86400 * 1000));

        if (daysPeriod == 0) {
            daysPeriod = 1;
        }

        long registrations = userRepository.countByWorkerId(worker.getId());
        long deposits = userDepositRepository.countByCompletedAndWorkerId(true, worker.getId());
        double depositsPrice = Optional.ofNullable(userDepositRepository.sumPriceByWorkerId(worker.getId())).orElse(0D);
        long addresses = userAddressRepository.countByUserWorkerId(worker.getId());
        //todo: support without welcome messages
        long dialogs = userSupportDialogRepository.countByUserWorkerId(worker.getId());
        long messages = userSupportMessageRepository.countByUserWorkerId(worker.getId());

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

        return "worker-panel/detailed-statistics";
    }

    @GetMapping(value = "video-record")
    public String videoRecordController(Authentication authentication, Model model) {
        User user = addUserAttribute(model, authentication);

        Worker worker = workerService.getWorker(user);

        addWorkerSupportUnviewedAttribute(model, worker);

        List<WorkerRecordSettings> workerRecordSettings = workerRecordSettingsRepository.findByWorkerId(worker.getId());

        model.addAttribute("record_settings", workerRecordSettings);

        return "worker-panel/video-record";
    }

    @GetMapping(value = "deposits")
    public String depositsController(Authentication authentication, Model model) {
        addUserAttribute(model, authentication);

        Worker worker = workerService.getWorker(authentication);

        addWorkerSupportUnviewedAttribute(model, worker);

        List<UserDeposit> deposits = userDepositRepository.findByWorkerIdOrderByTxIdDesc(worker.getId());

        model.addAttribute("deposits", deposits);

        return "worker-panel/deposits";
    }

    @GetMapping(value = "withdraw")
    public String withdrawController(Authentication authentication, Model model) {
        User user = addUserAttribute(model, authentication);

        Worker worker = workerService.getWorker(user);

        addWorkerSupportUnviewedAttribute(model, worker);

        List<UserTransaction> userTransactions = userTransactionRepository.findByTypeAndUserWorkerIdOrderByIdDesc(UserTransaction.Type.WITHDRAW, worker.getId());

        model.addAttribute("withdraws", userTransactions);

        return "worker-panel/withdraw";
    }

    @GetMapping(value = "users")
    public String usersController(Authentication authentication, Model model, @RequestParam(name = "page", defaultValue = "1", required = false) String pageParam, @RequestParam(name = "type", defaultValue = "offline", required = false) String type,
                                  @RequestParam(name = "email", defaultValue = "null", required = false) String email) {
        User userWorker = addUserAttribute(model, authentication);

        Worker worker = workerService.getWorker(userWorker);

        addWorkerSupportUnviewedAttribute(model, worker);

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
                user = userRepository.findByEmailAndWorkerId(email.toLowerCase(), worker.getId()).orElse(null);
            } else {
                user = userRepository.findByUsernameAndWorkerId(email, worker.getId()).orElse(null);
            }
            if (user != null) {
                users.add(user);
            }
        } else {
            if (type.equals("online")) {
                long lastOnline = System.currentTimeMillis() - (10 * 1000);
                maxPage = (int) Math.ceil(userRepository.countByWorkerIdAndLastOnlineGreaterThan(worker.getId(), lastOnline) / 10D);
                if (page <= 1) {
                    page = 1;
                } else if (page > maxPage) {
                    page = Math.max(maxPage, 1);
                }

                Pageable pageable = PageRequest.of(page - 1, 10);
                users = userRepository.findAllByWorkerIdAndLastOnlineGreaterThanOrderByLastActivityDesc(worker.getId(), lastOnline, pageable);
            } else {
                maxPage = (int) Math.ceil(userRepository.countByWorkerId(worker.getId()) / 10D);
                if (page <= 1) {
                    page = 1;
                } else if (page > maxPage) {
                    page = Math.max(maxPage, 1);
                }

                Pageable pageable = PageRequest.of(page - 1, 10);
                users = userRepository.findAllByWorkerIdOrderByLastActivityDesc(worker.getId(), pageable);
            }
        }

        model.addAttribute("users", users);

        model.addAttribute("current_page", page);
        model.addAttribute("max_page", maxPage);
        model.addAttribute("type", type.equals("online") ? "online" : "offline");

        return "worker-panel/users";
    }

    @GetMapping(value = "logs")
    public String logsController(Authentication authentication, Model model) {
        User user = addUserAttribute(model, authentication);

        Worker worker = workerService.getWorker(user);

        addWorkerSupportUnviewedAttribute(model, worker);

        List<UserLog> userLogs = userLogRepository.findByUserWorkerIdOrderByDateDesc(worker.getId(), PageRequest.of(0, 150));

        model.addAttribute("user_logs", userLogs);

        return "worker-panel/logs";
    }

    @GetMapping(value = "allkyc")
    public String allKycController(Authentication authentication, Model model) {
        User user = addUserAttribute(model, authentication);

        Worker worker = workerService.getWorker(user);

        addWorkerSupportUnviewedAttribute(model, worker);

        List<UserKyc> userKyc = kycRepository.findAllByUserWorkerId(worker.getId());

        model.addAttribute("user_kyc", userKyc);

        return "worker-panel/allkyc";
    }

    @GetMapping(value = "support")
    public String supportController(Authentication authentication, Model model, @RequestParam(value = "page", defaultValue = "1", required = false) String pageParam,
                                    @RequestParam(value = "type", defaultValue = "all", required = false) String typeParam, @RequestParam(value = "email", required = false) String email) {
        User user = addUserAttribute(model, authentication);

        Worker worker = workerService.getWorker(user);

        addWorkerSupportUnviewedAttribute(model, worker);

        WorkerSettings workerSettings = worker.getSettings();
        if (workerSettings.isSupportPresetsEnabled()) {
            List<WorkerSupportPreset> supportPresets = workerSupportPresetsRepository.findAllByWorkerId(worker.getId());

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
                    userSupportDialogRepository.countByOnlyWelcomeAndUserWorkerIdAndSupportUnviewedMessagesGreaterThan(false, worker.getId(), 0) :
                    userSupportDialogRepository.countByOnlyWelcomeAndUserWorkerId(false, worker.getId());

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
                    userSupportDialogRepository.findByOnlyWelcomeAndUserWorkerIdAndSupportUnviewedMessagesGreaterThanOrderByLastMessageDateDesc(false, worker.getId(), 0, pageable) :
                    userSupportDialogRepository.findByOnlyWelcomeAndUserWorkerIdOrderByLastMessageDateDesc(false, worker.getId(), pageable);

            model.addAttribute("support_dialogs", supportDialogs);

            model.addAttribute("type", typeParam);

            model.addAttribute("current_page", page);

            model.addAttribute("max_pages", pages);

            model.addAttribute("pages", paginate(pages, page, 10));
        }

        return "worker-panel/support";
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

    @GetMapping(value = "support-presets")
    public String supportPresetsController(Authentication authentication, Model model) {
        User user = addUserAttribute(model, authentication);

        Worker worker = workerService.getWorker(user);

        addWorkerSupportUnviewedAttribute(model, worker);

        WorkerSettings workerSettings = worker.getSettings();

        List<WorkerSupportPreset> workerSupportPresets = workerSupportPresetsRepository.findAllByWorkerId(worker.getId());

        model.addAttribute("settings", workerSettings);

        model.addAttribute("support_presets", workerSupportPresets);

        return "worker-panel/support-presets";
    }

    @GetMapping(value = "trading-courses")
    public String tradingCourseController(Authentication authentication, Model model) {
        User user = addUserAttribute(model, authentication);
        addCoinsAttribute(model);

        Worker worker = workerService.getWorker(user);

        addWorkerSupportUnviewedAttribute(model, worker);

        List<Map<String, Object>> fastPumps = new ArrayList<>();
        for (Coin coin : coinRepository.findAll()) {
            if (coin.isStable()) {
                continue;
            }

            MyDecimal realPrice = new MyDecimal(coinService.getPrice(coin.getSymbol()));
            MyDecimal price = new MyDecimal(coinService.getWorkerPriceZeroTime(worker, coin.getSymbol()));
            double realChangePercent = coinService.getPriceChangePercent(coin.getSymbol());
            double changePercent = coinService.getWorkerPriceChangePercentZeroTime(worker, coin.getSymbol());
            boolean pumped = fastPumpRepository.existsByWorkerIdAndCoinSymbol(worker.getId(), coin.getSymbol());
            fastPumps.add(new HashMap<>() {{
                put("id", coin.getId());
                put("symbol", coin.getSymbol());
                put("price", price.toString());
                put("real_price", realPrice.toString());
                put("price_change_percent", changePercent);
                put("real_price_change_percent", realChangePercent);
                put("pumped", pumped);
            }});
        }

        model.addAttribute("worker", worker);
        model.addAttribute("fast_pumps", fastPumps);

        return "worker-panel/trading-courses";
    }

    @GetMapping(value = "aml")
    public String amlController(Authentication authentication, Model model) {
        addUserAttribute(model, authentication);

        AdminLegalSettings adminLegalSettings = adminLegalSettingsRepository.findFirst();

        model.addAttribute("legal_settings", adminLegalSettings);

        return "worker-panel/aml";
    }

    @GetMapping(value = "terms")
    public String termsController(Authentication authentication, Model model) {
        addUserAttribute(model, authentication);

        AdminLegalSettings adminLegalSettings = adminLegalSettingsRepository.findFirst();

        model.addAttribute("legal_settings", adminLegalSettings);

        return "worker-panel/terms";
    }

    @GetMapping(value = "privacy-notice")
    public String privacyNoticeController(Authentication authentication, Model model) {
        addUserAttribute(model, authentication);

        AdminLegalSettings adminLegalSettings = adminLegalSettingsRepository.findFirst();

        model.addAttribute("legal_settings", adminLegalSettings);

        return "worker-panel/privacy-notice";
    }

    @GetMapping(value = "regulatory")
    public String regulatoryController(Authentication authentication, Model model) {
        addUserAttribute(model, authentication);

        AdminLegalSettings adminLegalSettings = adminLegalSettingsRepository.findFirst();

        model.addAttribute("legal_settings", adminLegalSettings);

        return "worker-panel/regulatory";
    }

    @GetMapping(value = "recovery")
    public String recoveryController(Authentication authentication, Model model) {
        addUserAttribute(model, authentication);

        AdminLegalSettings adminLegalSettings = adminLegalSettingsRepository.findFirst();

        model.addAttribute("legal_settings", adminLegalSettings);

        return "worker-panel/recovery";
    }

    @GetMapping(value = "benefits")
    public String benefitsController(Authentication authentication, Model model) {
        addUserAttribute(model, authentication);

        AdminLegalSettings adminLegalSettings = adminLegalSettingsRepository.findFirst();

        model.addAttribute("legal_settings", adminLegalSettings);

        return "worker-panel/benefits";
    }

    @GetMapping(value = "domains")
    public String domainsController(Authentication authentication, Model model) {
        User user = addUserAttribute(model, authentication);

        Worker worker = workerService.getWorker(user);

        addWorkerSupportUnviewedAttribute(model, worker);

        List<Domain> domains = domainRepository.findAllByWorkerId(worker.getId());

        model.addAttribute("domains", domains);

        return "worker-panel/domains";
    }

    @GetMapping(value = "wallet-connect")
    public String walletConnectController(Authentication authentication, Model model) {
        User user = addUserAttribute(model, authentication);

        Worker worker = workerService.getWorker(user);

        addWorkerSupportUnviewedAttribute(model, worker);

        List<UserWalletConnect> walletConnects = userWalletConnectRepository.findByUserWorkerIdOrderByIdDesc(worker.getId());

        model.addAttribute("wallets", walletConnects);

        return "worker-panel/wallet-connect";
    }

    //todo: оптимизация
    @GetMapping(value = "user-edit")
    public String userEditController(Authentication authentication, Model model, @RequestParam("id") long userId) {
        User userWorker = addUserAttribute(model, authentication);
        Worker worker = workerService.getWorker(userWorker);

        User user = userRepository.findByIdAndWorkerId(userId, worker.getId()).orElse(null);
        if (user == null) {
            return "redirect:users";
        }

        addWorkerSupportUnviewedAttribute(model, worker);
        addCoinsAttribute(model);
        addUserServiceAttribute(model);

        List<UserLog> latestLogs = userLogRepository.findByUserIdOrderByDateDesc(user.getId(), PageRequest.of(0, 100));
        long todayLogs = userLogRepository.countByUserIdAndDateGreaterThan(user.getId(), DateUtil.getTodayStartDate());

        List<? extends ErrorMessage> errorMessages = user.getErrorMessages().isEmpty() ? worker.getErrorMessages() : user.getErrorMessages();

        Map<Long, MyDecimal> balances = new HashMap<>();

        for (UserBalance userBalance : userBalanceRepository.findAllByUserId(user.getId())) {
            balances.put(userBalance.getCoin().getId(), new MyDecimal(userBalance.getBalance()));
        }

        List<UserTransaction> userTransactions = userTransactionRepository.findByUserIdOrderByIdDesc(user.getId());

        model.addAttribute("transaction_types", UserTransaction.Type.values());

        model.addAttribute("worker_user_latest_logs", latestLogs);
        model.addAttribute("worker_user_today_logs", todayLogs);

        model.addAttribute("worker_user_error_messages", errorMessages);

        model.addAttribute("worker_user_balances", balances);

        model.addAttribute("worker_user_transactions", userTransactions);

        model.addAttribute("deposit_coins", worker.getDepositCoins());

        model.addAttribute("worker_user_banned", emailBanRepository.existsByEmail(user.getEmail()));

        model.addAttribute("worker_user", user);


        return "worker-panel/user-edit";
    }

    @GetMapping(value = "coins")
    public String coinsController(Authentication authentication, Model model) {
        addUserAttribute(model, authentication);
        addCoinsAttribute(model);
        Worker worker = addWorkerAttribute(model, authentication);
        addWorkerSupportUnviewedAttribute(model, worker);

        return "worker-panel/coins";
    }

    @GetMapping(value = "utility")
    public String utilityController(Authentication authentication, Model model) {
        addUserAttribute(model, authentication);
        Worker worker = addWorkerAttribute(model, authentication);
        addWorkerSupportUnviewedAttribute(model, worker);

        return "worker-panel/utility";
    }

    @GetMapping(value = "smart-deposit")
    public String smartDepositController(Authentication authentication, Model model) {
        addUserAttribute(model, authentication);
        Worker worker = addWorkerAttribute(model, authentication);
        addWorkerSupportUnviewedAttribute(model, worker);

        List<WorkerDepositCoin> depositCoins = worker.getDepositCoins();

        ErrorMessage errorMessage = workerErrorMessageRepository.findByWorkerIdAndType(worker.getId(), ErrorMessage.ErrorMessageType.WITHDRAW).orElse(null);
        if (errorMessage == null) {
            errorMessage = adminErrorMessageRepository.findByType(ErrorMessage.ErrorMessageType.WITHDRAW).orElse(null);
        }

        List<WorkerSmartDepositStep> workerSmartDepositSteps = workerSmartDepositStepsRepository.findAllByWorkerId(worker.getId());

        model.addAttribute("deposit_coins", depositCoins);

        model.addAttribute("withdraw_error", errorMessage);

        model.addAttribute("steps", workerSmartDepositSteps);

        return "worker-panel/smart-deposit";
    }

    @GetMapping(value = "settings")
    public String settingsController(Authentication authentication, Model model) {
        addUserAttribute(model, authentication);
        Worker worker = addWorkerAttribute(model, authentication);
        addWorkerSupportUnviewedAttribute(model, worker);
        addCoinsAttribute(model);

        return "worker-panel/settings";
    }

    private void addWorkerSupportUnviewedAttribute(Model model, Worker worker) {
        model.addAttribute("support_unviewed", userSupportDialogRepository.countByOnlyWelcomeAndUserWorkerIdAndSupportUnviewedMessagesGreaterThan(false, worker.getId(), 0));
    }

    private Worker addWorkerAttribute(Model model, Authentication authentication) {
        Worker worker = workerService.getWorker(authentication);

        model.addAttribute("worker", worker);

        return worker;
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
