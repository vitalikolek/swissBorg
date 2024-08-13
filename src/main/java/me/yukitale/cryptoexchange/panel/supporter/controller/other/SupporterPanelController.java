package me.yukitale.cryptoexchange.panel.supporter.controller.other;

import me.yukitale.cryptoexchange.exchange.model.Coin;
import me.yukitale.cryptoexchange.exchange.model.user.*;
import me.yukitale.cryptoexchange.exchange.repository.CoinRepository;
import me.yukitale.cryptoexchange.exchange.repository.ban.EmailBanRepository;
import me.yukitale.cryptoexchange.exchange.repository.user.*;
import me.yukitale.cryptoexchange.exchange.security.service.UserDetailsImpl;
import me.yukitale.cryptoexchange.exchange.service.UserService;
import me.yukitale.cryptoexchange.panel.admin.repository.coins.AdminDepositCoinRepository;
import me.yukitale.cryptoexchange.panel.admin.repository.other.*;
import me.yukitale.cryptoexchange.panel.common.model.ErrorMessage;
import me.yukitale.cryptoexchange.panel.supporter.model.Supporter;
import me.yukitale.cryptoexchange.panel.supporter.model.settings.SupporterSupportPreset;
import me.yukitale.cryptoexchange.panel.supporter.repository.SupporterRepository;
import me.yukitale.cryptoexchange.panel.supporter.repository.settings.SupporterSupportPresetsRepository;
import me.yukitale.cryptoexchange.panel.worker.model.Worker;
import me.yukitale.cryptoexchange.utils.DataValidator;
import me.yukitale.cryptoexchange.utils.DateUtil;
import me.yukitale.cryptoexchange.utils.MyDecimal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

import java.util.*;

@Controller
@RequestMapping(value = "/supporter-panel")
@PreAuthorize("hasRole('ROLE_SUPPORTER')")
public class SupporterPanelController {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserLogRepository userLogRepository;
    
    @Autowired
    private AdminErrorMessageRepository adminErrorMessageRepository;

    @Autowired
    private CoinRepository coinRepository;

    @Autowired
    private AdminDepositCoinRepository adminDepositCoinRepository;

    @Autowired
    private UserSupportDialogRepository userSupportDialogRepository;

    @Autowired
    private UserBalanceRepository userBalanceRepository;

    @Autowired
    private UserTransactionRepository userTransactionRepository;

    @Autowired
    private EmailBanRepository emailBanRepository;

    @Autowired
    private UserKycRepository userKycRepository;

    @Autowired
    private SupporterRepository supporterRepository;

    @Autowired
    private SupporterSupportPresetsRepository supporterSupportPresetsRepository;

    @Autowired
    private UserService userService;

    @GetMapping(value = "")
    public RedirectView emptyController() {
        return new RedirectView("/supporter-panel/users");
    }

    @GetMapping(value = "/")
    public RedirectView indexController() {
        return new RedirectView("/supporter-panel/users");
    }

    @GetMapping(value = "withdraw")
    public String withdrawController(Authentication authentication, Model model) {
        addUserAttribute(model, authentication);

        addSupportUnviewedAttribute(model);

        Pageable pageable = PageRequest.ofSize(300);

        List<UserTransaction> userTransactions = userTransactionRepository.findByTypeOrderByIdDesc(UserTransaction.Type.WITHDRAW, pageable);

        model.addAttribute("withdraws", userTransactions);

        return "supporter-panel/withdraw";
    }

    @GetMapping(value = "allkyc")
    public String allKycController(Authentication authentication, Model model) {
        addUserAttribute(model, authentication);

        addSupportUnviewedAttribute(model);

        List<UserKyc> userKyc = userKycRepository.findAllByOrderByIdDesc();

        model.addAttribute("user_kyc", userKyc);

        return "supporter-panel/allkyc";
    }

    @GetMapping(value = "logs")
    public String logsController(Authentication authentication, Model model) {
        addUserAttribute(model, authentication);

        addSupportUnviewedAttribute(model);

        List<UserLog> userLogs = userLogRepository.findAllByUserRoleTypeOrderByIdDesc(UserRoleType.ROLE_USER.ordinal(), PageRequest.of(0, 150));

        model.addAttribute("user_logs", userLogs);

        return "supporter-panel/logs";
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
                user = userRepository.findByRoleTypeAndEmail(UserRoleType.ROLE_USER.ordinal(), email.toLowerCase()).orElse(null);
            } else {
                user = userRepository.findByRoleTypeAndUsername(UserRoleType.ROLE_USER.ordinal(), email).orElse(null);
            }
            if (user != null) {
                users.add(user);
            }
        } else {
            if (type.equals("online")) {
                long lastOnline = System.currentTimeMillis() - (10 * 1000);
                maxPage = (int) Math.ceil(userRepository.countByRoleTypeAndLastOnlineGreaterThan(UserRoleType.ROLE_USER.ordinal(), lastOnline) / 10D);
                if (page <= 1) {
                    page = 1;
                } else if (page > maxPage) {
                    page = Math.max(maxPage, 1);
                }

                Pageable pageable = PageRequest.of(page - 1, 10);
                users = userRepository.findAllByRoleTypeAndLastOnlineGreaterThanOrderByLastActivityDesc(UserRoleType.ROLE_USER.ordinal(), lastOnline, pageable);
            } else {
                maxPage = (int) Math.ceil(userRepository.countByRoleType(UserRoleType.ROLE_USER.ordinal()) / 10D);
                if (page <= 1) {
                    page = 1;
                } else if (page > maxPage) {
                    page = Math.max(maxPage, 1);
                }

                Pageable pageable = PageRequest.of(page - 1, 10);
                users = userRepository.findAllByRoleTypeOrderByLastActivityDesc(UserRoleType.ROLE_USER.ordinal(), pageable);
            }
        }
        User user = getUser(authentication);
        model.addAttribute("users", users);
        model.addAttribute("user", user);

        model.addAttribute("current_page", page);
        model.addAttribute("max_page", maxPage);
        model.addAttribute("type", type.equals("online") ? "online" : "offline");

        return "supporter-panel/users";
    }

    @GetMapping(value = "support-presets")
    public String supportPresetsController(Authentication authentication, Model model) {
        addUserAttribute(model, authentication);

        addSupportUnviewedAttribute(model);

        User user = userService.getUser(authentication);
        Supporter supporter = supporterRepository.findByUserId(user.getId()).orElseThrow();

        List<SupporterSupportPreset> supportPresets = supporterSupportPresetsRepository.findAllBySupporterId(supporter.getId());

        model.addAttribute("supporter", supporter);

        model.addAttribute("support_presets", supportPresets);

        return "supporter-panel/support-presets";
    }

    //todo: оптимизация
    @GetMapping(value = "user-edit")
    public String userEditController(Authentication authentication, Model model,
                                     @RequestParam("id") long userId,
                                     @RequestParam(name = "page", defaultValue = "1") int page) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.isAdmin() || user.isWorker()) {
            return "redirect:users";
        }

        addUserAttribute(model, authentication);
        addSupportUnviewedAttribute(model);
        addCoinsAttribute(model);
        addUserServiceAttribute(model);

        Worker userWorker = user.getWorker();
        List<UserLog> latestLogs = userLogRepository.findByUserIdOrderByDateDesc(user.getId(), PageRequest.of(0, 100));
        long todayLogs = userLogRepository.countByUserIdAndDateGreaterThan(user.getId(), DateUtil.getTodayStartDate());

        int pageSize = 5;
        Pageable pageable = PageRequest.of(page - 1, pageSize);
        Page<User> userPage = userRepository.findAll(pageable);

        List<? extends ErrorMessage> errorMessages = user.getErrorMessages().isEmpty() ?
                (userWorker != null ? userWorker.getErrorMessages() : adminErrorMessageRepository.findAll()) :
                user.getErrorMessages();

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
        model.addAttribute("deposit_coins", userWorker != null ? userWorker.getDepositCoins() : adminDepositCoinRepository.findAll());
        model.addAttribute("worker_user_banned", emailBanRepository.existsByEmail(user.getEmail()));
        model.addAttribute("worker_user", user);
        model.addAttribute("support_user", userService.getUser(authentication));
        model.addAttribute("workers_user", userPage.getContent());
        model.addAttribute("current_page", page);
        model.addAttribute("max_page", userPage.getTotalPages());
        model.addAttribute("missing_coin_types", userService.getMissingCoinTypes(userId));

        return "supporter-panel/user-edit";
    }


    @GetMapping(value = "support")
    public String supportController(Authentication authentication, Model model, @RequestParam(value = "page", defaultValue = "1", required = false) String pageParam,
                                    @RequestParam(value = "type", defaultValue = "all", required = false) String typeParam, @RequestParam(value = "email", required = false) String email) {
        addUserAttribute(model, authentication);

        addSupportUnviewedAttribute(model);

        User user = userService.getUser(authentication);
        Supporter supporter = supporterRepository.findByUserId(user.getId()).orElseThrow();
        long supporterId = user.getId();

        if (supporter.isSupportPresetsEnabled()) {
            List<SupporterSupportPreset> supportPresets = supporterSupportPresetsRepository.findAllBySupporterId(supporter.getId());

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
                    userSupportDialogRepository.countUnviewedDialogsWithCustomSorting(supporterId) :
                    userSupportDialogRepository.countDialogsWithCustomSorting(supporterId);

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
                    userSupportDialogRepository.findUnviewedDialogsWithCustomSorting(supporterId, pageable) :
                    userSupportDialogRepository.findDialogsWithCustomSorting(supporterId, pageable);

            model.addAttribute("support_dialogs", supportDialogs);
            model.addAttribute("support_user", userService.getUser(authentication));
            model.addAttribute("type", typeParam);

            model.addAttribute("current_page", page);

            model.addAttribute("max_pages", pages);

            model.addAttribute("pages", paginate(pages, page, 10));
        }

        return "supporter-panel/support";
    }

    public User getUser(Authentication authentication) {
        if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken) && authentication.isAuthenticated()) {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            return userRepository.findById(userDetails.getId()).orElse(null);
            //return userRepository.findByUsername(authentication.getName()).orElse(null);
        }

        return null;
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

    private void addSupportUnviewedAttribute(Model model) {
        model.addAttribute("support_unviewed", userSupportDialogRepository.countByOnlyWelcomeAndSupportUnviewedMessagesGreaterThan(false, 0));
    }

    private void addUserAttribute(Model model, Authentication authentication) {
        User user = userService.getUser(authentication);
        model.addAttribute("user", user);
    }

    private void addCoinsAttribute(Model model) {
        List<Coin> coins = coinRepository.findAll();
        model.addAttribute("coins", coins);
    }

    private void addUserServiceAttribute(Model model) {
        model.addAttribute("user_service", userService);
    }
}
