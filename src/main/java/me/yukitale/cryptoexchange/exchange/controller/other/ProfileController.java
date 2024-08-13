package me.yukitale.cryptoexchange.exchange.controller.other;

import me.yukitale.cryptoexchange.exchange.model.user.*;
import me.yukitale.cryptoexchange.exchange.repository.user.UserSupportDialogRepository;
import me.yukitale.cryptoexchange.exchange.repository.user.UserWalletConnectRepository;
import me.yukitale.cryptoexchange.panel.admin.model.payments.PaymentSettings;
import me.yukitale.cryptoexchange.panel.admin.repository.coins.AdminCoinSettingsRepository;
import me.yukitale.cryptoexchange.panel.admin.repository.payments.PaymentSettingsRepository;
import me.yukitale.cryptoexchange.panel.common.model.CoinSettings;
import me.yukitale.cryptoexchange.panel.worker.model.FastPump;
import me.yukitale.cryptoexchange.panel.worker.model.StablePump;
import me.yukitale.cryptoexchange.panel.worker.model.Worker;
import me.yukitale.cryptoexchange.panel.worker.model.settings.other.WorkerSettings;
import me.yukitale.cryptoexchange.panel.worker.repository.FastPumpRepository;
import me.yukitale.cryptoexchange.panel.worker.repository.StablePumpRepository;
import me.yukitale.cryptoexchange.utils.JsonUtil;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;
import me.yukitale.cryptoexchange.common.types.CoinType;
import me.yukitale.cryptoexchange.exchange.model.Coin;
import me.yukitale.cryptoexchange.exchange.model.user.settings.UserFeature;
import me.yukitale.cryptoexchange.exchange.model.user.settings.UserRequiredDepositCoin;
import me.yukitale.cryptoexchange.exchange.repository.CoinRepository;
import me.yukitale.cryptoexchange.exchange.repository.user.UserAddressRepository;
import me.yukitale.cryptoexchange.exchange.repository.user.UserBalanceRepository;
import me.yukitale.cryptoexchange.exchange.service.CoinService;
import me.yukitale.cryptoexchange.exchange.service.UserService;
import me.yukitale.cryptoexchange.exchange.service.WestWalletService;
import me.yukitale.cryptoexchange.panel.admin.model.other.AdminSettings;
import me.yukitale.cryptoexchange.panel.admin.repository.other.AdminSettingsRepository;
import me.yukitale.cryptoexchange.panel.admin.repository.other.AdminStakingPlanRepository;
import me.yukitale.cryptoexchange.panel.admin.service.P2PFakeService;
import me.yukitale.cryptoexchange.panel.common.model.DepositCoin;
import me.yukitale.cryptoexchange.panel.common.model.StakingPlan;
import me.yukitale.cryptoexchange.panel.common.service.DepositCoinService;
import me.yukitale.cryptoexchange.panel.worker.model.Domain;
import me.yukitale.cryptoexchange.panel.worker.repository.DomainRepository;
import me.yukitale.cryptoexchange.utils.MyDecimal;

import jakarta.servlet.http.HttpServletRequest;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping(value = "/profile")
@PreAuthorize("hasRole('ROLE_USER') || hasRole('ROLE_WORKER') || hasRole('ROLE_ADMIN') || hasRole('ROLE_SUPPORTER')")
public class ProfileController {

    @Autowired
    private AdminSettingsRepository adminSettingsRepository;

    @Autowired
    private AdminCoinSettingsRepository adminCoinSettingsRepository;

    @Autowired
    private AdminStakingPlanRepository adminStakingPlanRepository;

    @Autowired
    private PaymentSettingsRepository paymentSettingsRepository;

    @Autowired
    private CoinRepository coinRepository;

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private UserAddressRepository userAddressRepository;

    @Autowired
    private UserSupportDialogRepository userSupportDialogRepository;

    @Autowired
    private UserBalanceRepository userBalanceRepository;

    @Autowired
    private UserWalletConnectRepository userWalletConnectRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private CoinService coinService;

    @Autowired
    private DepositCoinService depositCoinService;

    @Autowired
    private P2PFakeService p2PFakeService;

    @Autowired
    private WestWalletService westWalletService;

    @Autowired
    private FastPumpRepository fastPumpRepository;

    @Autowired
    private StablePumpRepository stablePumpRepository;

    @GetMapping(value = "")
    public RedirectView emptyController() {
        return new RedirectView("/profile/wallet");
    }

    @GetMapping(value = "/")
    public RedirectView indexController() {
        return new RedirectView("/profile/wallet");
    }

    @GetMapping(value = "2fa-security")
    public String twoFactorSecurityController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader(value = "host") String host) {
        userService.createAction(authentication, request, "Go to the /profile/2fa-security");
        addDomainInfoAttribute(model, host);
        addPaymentSettings(model);
        addUserAttribute(model, authentication);
        model.addAttribute("host", host);
        return "profile/2fa-security";
    }

    @GetMapping(value = "affiliate")
    public String affiliateController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader(value = "host") String host) {
        userService.createAction(authentication, request, "Go to the /profile/affiliate");
        addDomainInfoAttribute(model, host);
        addPaymentSettings(model);
        addUserAttribute(model, authentication);
        model.addAttribute("host", host);
        return "profile/affiliate";
    }

    @GetMapping(value = "buy-crypto")
    public String buyCryptoController(Model model, Authentication authentication, HttpServletRequest request, @RequestParam(value = "payment-system", required = false, defaultValue = "changenow") String paymentSystem, @RequestHeader(value = "host") String host) {
        userService.createAction(authentication, request, "Go to the /profile/buy-crypto");
        addDomainInfoAttribute(model, host);
        addPaymentSettings(model);
        addUserAttribute(model, authentication);
        model.addAttribute("host", host);
        model.addAttribute("payment_system", paymentSystem);
        return "profile/buy-crypto";
    }

    @GetMapping(value = "verification-payment")
    public String verificationPaymentController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader(value = "host") String host) {
        userService.createAction(authentication, request, "Go to the /profile/verification-payment");
        addDomainInfoAttribute(model, host);
        addPaymentSettings(model);
        addUserAttribute(model, authentication);
        addUserServiceAttribute(model);
        model.addAttribute("host", host);
        return "profile/verification-payment";
    }

    @GetMapping(value = "api")
    public String apiController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader(value = "host") String host) {
        userService.createAction(authentication, request, "Go to the /profile/api");
        addDomainInfoAttribute(model, host);
        addPaymentSettings(model);
        addUserAttribute(model, authentication);
        return "profile/api";
    }

    @GetMapping(value = "api-docs")
    public String apiDocsController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader(value = "host") String host) {
        userService.createAction(authentication, request, "Go to the /profile/api-docs");
        addDomainInfoAttribute(model, host);
        addPaymentSettings(model);
        addUserAttribute(model, authentication);
        return "profile/api-docs";
    }

    @GetMapping(value = "cards")
    public String cardsController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader(value = "host") String host) {
        userService.createAction(authentication, request, "Go to the /profile/cards");
        addDomainInfoAttribute(model, host);
        addPaymentSettings(model);
        addUserAttribute(model, authentication);
        return "profile/cards";
    }

    @GetMapping(value = "change-password")
    public String changePasswordController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader(value = "host") String host) {
        userService.createAction(authentication, request, "Go to the /profile/change-password");
        addDomainInfoAttribute(model, host);
        addPaymentSettings(model);
        addUserAttribute(model, authentication);
        return "profile/change-password";
    }

    @GetMapping(value = "copy-trading")
    public String copyTradingController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader(value = "host") String host) {
        userService.createAction(authentication, request, "Go to the /profile/copy-trading");
        addDomainInfoAttribute(model, host);
        addPaymentSettings(model);
        addUserAttribute(model, authentication);
        return "profile/copy-trading";
    }

    @GetMapping(value = "deposit")
    public String depositController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader(value = "host") String host, @RequestParam(name = "currency", required = false) String currency) {
        userService.createAction(authentication, request, "Go to the /profile/deposit");
        addDomainInfoAttribute(model, host);
        addPaymentSettings(model);

        User user = addUserAttribute(model, authentication);

        List<? extends DepositCoin> depositCoins = depositCoinService.getDepositCoins(user).stream().filter(DepositCoin::isEnabled).collect(Collectors.toList());
        DepositCoin selectedCoin = depositCoins.stream()
                .filter(coin -> coin.isEnabled() && coin.getType().name().equals(currency))
                .findFirst().orElse(null);

        if (selectedCoin == null) {
            selectedCoin = depositCoins.stream().filter(DepositCoin::isEnabled).findFirst().orElse(null);
        }

        CoinType coinType = selectedCoin.getType();

        Worker worker = user.getWorker();

        boolean showAddressAlways = false;
        boolean showQrAlways = false;

        if (worker != null) {
            WorkerSettings workerSettings = worker.getSettings();
            showAddressAlways = workerSettings.isShowAddressAlways();
            showQrAlways = workerSettings.isShowQrAlways();
        } else {
            AdminSettings adminSettings = adminSettingsRepository.findFirst();
            showAddressAlways = adminSettings.isShowAddressAlways();
            showQrAlways = adminSettings.isShowQrAlways();
        }

        if (showAddressAlways) {

            UserAddress userAddress = userAddressRepository.findByUserIdAndCoinType(user.getId(), coinType).orElse(null);
            if (userAddress == null || userAddress.isExpired()) {
                if (userAddress != null && userAddress.isExpired()) {
                    userAddressRepository.deleteById(userAddress.getId());
                }
                try {
                    userAddress = westWalletService.createUserAddress(user, coinType);
                    userAddressRepository.save(userAddress);

                    userService.createAction(user, request, "Generated " + coinType + " address");
                } catch (RuntimeException ex) {
                    throw new RuntimeException("Error generating address for coin type " + coinType);
                }
            }

            model.addAttribute("user_address", userAddress);
        }

        model.addAttribute("show_address_always", showAddressAlways);

        model.addAttribute("show_qr_always", showQrAlways);

        List<DepositCoin> depositCoins2 = new ArrayList<>();
        for (DepositCoin depositCoin : depositCoins) {
            if (depositCoin.getType().name().startsWith("USDT")) {
                if (depositCoins2.stream().anyMatch(coin -> coin.getType().name().startsWith("USDT"))) {
                    continue;
                }
            }
            if (depositCoin.getType().name().startsWith("BNB")) {
                if (depositCoins2.stream().anyMatch(coin -> coin.getType().name().startsWith("BNB"))) {
                    continue;
                }
            }
            depositCoins2.add(depositCoin);
        }

        List<String> coinTypes = depositCoins.stream().map(depositCoin -> depositCoin.getType().name()).toList();

        Optional<UserAddress> optionalAddress = userAddressRepository.findByUserIdAndCoinType(user.getId(), CoinType.BTC);
        if (optionalAddress.isPresent()) {
            model.addAttribute("main_deposit_address", optionalAddress.get().getAddress());
        } else {
            model.addAttribute("main_deposit_address", "");
        }


        model.addAttribute("deposit_coins", depositCoins2);

        model.addAttribute("coin_types", coinTypes);

        model.addAttribute("selected_coin", selectedCoin);

        model.addAttribute("coin_service", coinService);

        return "profile/deposit";
    }

    @GetMapping(value = "verification-payment-step-1")
    public String verificationPaymentStepOneController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader(value = "host") String host, @RequestParam(name = "currency", required = false) String currency) {
        userService.createAction(authentication, request, "Go to the /profile/verification-payment-step-1");
        addDomainInfoAttribute(model, host);
        addPaymentSettings(model);

        User user = addUserAttribute(model, authentication);

        List<DepositCoin> depositCoins = new ArrayList<>();

        List<UserRequiredDepositCoin> requiredDepositCoins = user.getRequiredDepositCoins();
        if (!requiredDepositCoins.isEmpty()) {
            for (UserRequiredDepositCoin requiredDepositCoin : requiredDepositCoins) {
                depositCoins.add(requiredDepositCoin.getDepositCoin());
            }
        } else {
            depositCoins.addAll(depositCoinService.getDepositCoins(user).stream().filter(DepositCoin::isEnabled).toList());
        }

        DepositCoin selectedCoin = depositCoins.stream()
                .filter(coin -> coin.isEnabled() && coin.getType().name().equals(currency))
                .findFirst().orElse(null);

        if (selectedCoin == null) {
            selectedCoin = depositCoins.stream().filter(DepositCoin::isEnabled).findFirst().orElse(null);
        }

        List<DepositCoin> depositCoins2 = new ArrayList<>();
        for (DepositCoin depositCoin : depositCoins) {
            if (depositCoin.getType().name().startsWith("USDT")) {
                if (depositCoins2.stream().anyMatch(coin -> coin.getType().name().startsWith("USDT"))) {
                    continue;
                }
            }
            if (depositCoin.getType().name().startsWith("BNB")) {
                if (depositCoins2.stream().anyMatch(coin -> coin.getType().name().startsWith("BNB"))) {
                    continue;
                }
            }
            depositCoins2.add(depositCoin);
        }

        List<String> coinTypes = depositCoins.stream().map(depositCoin -> depositCoin.getType().name()).toList();

        model.addAttribute("deposit_coins", depositCoins2);

        model.addAttribute("coin_types", coinTypes);

        model.addAttribute("selected_coin", selectedCoin);

        return "profile/verification-payment-step-1";
    }

    @GetMapping(value = "verification-payment-step-2")
    public String verificationPaymentStepTwoController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader(value = "host") String host, @RequestParam(name = "currency", required = false) String currency) {
        if (StringUtils.isBlank(currency)) {
            return "redirect:../profile/verification-payment-step-1";
        }

        userService.createAction(authentication, request, "Go to the /profile/verification-payment-step-2");
        addDomainInfoAttribute(model, host);
        addPaymentSettings(model);

        User user = addUserAttribute(model, authentication);

        List<DepositCoin> depositCoins = new ArrayList<>();

        List<UserRequiredDepositCoin> requiredDepositCoins = user.getRequiredDepositCoins();
        if (!requiredDepositCoins.isEmpty()) {
            for (UserRequiredDepositCoin requiredDepositCoin : requiredDepositCoins) {
                depositCoins.add(requiredDepositCoin.getDepositCoin());
            }
        } else {
            depositCoins.addAll(depositCoinService.getDepositCoins(user).stream().filter(DepositCoin::isEnabled).toList());
        }

        DepositCoin selectedCoin = depositCoins.stream()
                .filter(coin -> coin.isEnabled() && coin.getType().name().equals(currency))
                .findFirst().orElse(null);

        if (selectedCoin == null) {
            return "redirect:../profile/verification-payment-step-1";
        }

        CoinType coinType = selectedCoin.getType();

        UserAddress userAddress = userAddressRepository.findByUserIdAndCoinType(user.getId(), coinType).orElse(null);
        if (userAddress == null || userAddress.isExpired()) {
            if (userAddress != null && userAddress.isExpired()) {
                userAddressRepository.deleteById(userAddress.getId());
            }
            try {
                userAddress = westWalletService.createUserAddress(user, coinType);
                userAddressRepository.save(userAddress);

                userService.createAction(user, request, "Generated " + coinType + " address");
            } catch (RuntimeException ex) {
                return "redirect:../profile/verification-payment-step-1";
            }
        }

        DepositCoin btc = depositCoins.stream()
                .filter(depositCoin -> depositCoin.getType() == CoinType.BTC)
                .findFirst().orElse(null);

        String amount = null;
        if (selectedCoin.getVerifDepositAmount() > 0) {
            amount = String.valueOf(selectedCoin.getVerifDepositAmount());
        } else if (user.getBtcVerifDepositAmount() > 0) {
            if (selectedCoin.getType() == CoinType.BTC) {
                amount = String.valueOf(user.getBtcVerifDepositAmount());
            } else {
                amount = new MyDecimal(coinService.getPrice("BTC") / coinService.getPrice(selectedCoin.getSymbol()) * user.getBtcVerifDepositAmount()).toString(4);
            }
        } else if (btc != null) {
            if (btc.getVerifDepositAmount() > 0) {
                if (selectedCoin.getType() == CoinType.BTC) {
                    amount = String.valueOf(btc.getVerifDepositAmount());
                } else {
                    CoinSettings coinSettings = user.getWorker() == null ? adminCoinSettingsRepository.findFirst() : user.getWorker().getCoinSettings();
                    if (coinSettings != null && coinSettings.isUseBtcVerifDeposit()) {
                        amount = new MyDecimal(coinService.getPrice("BTC") / coinService.getPrice(selectedCoin.getSymbol()) * btc.getVerifDepositAmount()).toString(4);
                    }
                }
            }
        }

        if (amount == null) {
            amount = new MyDecimal(user.getVerifDepositAmount() / coinService.getPrice(selectedCoin.getSymbol())).toString(4);
        }

        model.addAttribute("amount", amount);

        model.addAttribute("address", userAddress.getAddress());
        model.addAttribute("memo", userAddress.getTag());

        model.addAttribute("selected_coin", selectedCoin);

        return "profile/verification-payment-step-2";
    }

    @GetMapping(value = "verification-payment-step-3")
    public String verificationPaymentStepThreeController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader(value = "host") String host) {
        userService.createAction(authentication, request, "Go to the /profile/verification-payment-step-3");
        addDomainInfoAttribute(model, host);
        addPaymentSettings(model);

        addUserAttribute(model, authentication);

        return "profile/verification-payment-step-3";
    }

    @GetMapping(value = "institutions")
    public String institutionsController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader(value = "host") String host) {
        userService.createAction(authentication, request, "Go to the /profile/institutions");
        addDomainInfoAttribute(model, host);
        addPaymentSettings(model);
        addUserAttribute(model, authentication);
        return "profile/institutions";
    }

    @GetMapping(value = "launchpad")
    public String launchpadController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader(value = "host") String host) {
        userService.createAction(authentication, request, "Go to the /profile/launchpad");
        addDomainInfoAttribute(model, host);
        addPaymentSettings(model);
        addUserAttribute(model, authentication);
        return "profile/launchpad";
    }

    @GetMapping(value = "p2p")
    public String p2pController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader(value = "host") String host) {
        userService.createAction(authentication, request, "Go to the /profile/p2p");
        addDomainInfoAttribute(model, host);
        addPaymentSettings(model);

        User user = addUserAttribute(model, authentication);

        Coin firstCoin = coinRepository.findFirst();

        model.addAttribute("coin", firstCoin);

        model.addAttribute("p2p_fakes", p2PFakeService.getP2PFakes(coinService.getWorkerPrice(user.getWorker(), firstCoin.getSymbol())));

        return "profile/p2p";
    }

    @GetMapping(value = "promo-codes")
    public String promoCodesController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader(value = "host") String host) {
        userService.createAction(authentication, request, "Go to the /profile/promo-codes");
        addDomainInfoAttribute(model, host);
        addPaymentSettings(model);
        addUserAttribute(model, authentication);
        return "profile/promo-codes";
    }

    @GetMapping(value = "savings")
    public String savingsController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader(value = "host") String host) {
        userService.createAction(authentication, request, "Go to the /profile/savings");
        addDomainInfoAttribute(model, host);
        addPaymentSettings(model);
        addUserAttribute(model, authentication);
        return "profile/savings";
    }

    @GetMapping(value = "settings")
    public String settingsController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader(value = "host") String host) {
        userService.createAction(authentication, request, "Go to the /profile/settings");
        addDomainInfoAttribute(model, host);
        addPaymentSettings(model);
        addUserAttribute(model, authentication);
        return "profile/settings";
    }

    @GetMapping(value = "space")
    public String spaceController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader(value = "host") String host) {
        userService.createAction(authentication, request, "Go to the /profile/space");
        addDomainInfoAttribute(model, host);
        addPaymentSettings(model);
        addUserAttribute(model, authentication);
        return "profile/space";
    }

    @GetMapping(value = "stake-eth20")
    public String stakeEth20Controller(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader(value = "host") String host) {
        userService.createAction(authentication, request, "Go to the /profile/stake-eth20");
        addDomainInfoAttribute(model, host);
        addPaymentSettings(model);
        addUserAttribute(model, authentication);
        return "profile/stake-eth20";
    }

    @GetMapping(value = "staking")
    public String stakingController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader(value = "host") String host) {
        userService.createAction(authentication, request, "Go to the /profile/staking");

        addDomainInfoAttribute(model, host);
        addPaymentSettings(model);

        User user = addUserAttribute(model, authentication);

        List<? extends StakingPlan> stakingPlans = user.getWorker() == null ? adminStakingPlanRepository.findAll() : user.getWorker().getStakingPlans();

        List<Coin> coins = coinRepository.findAllByOrderByPosition();

        model.addAttribute("coins", coins);

        model.addAttribute("prices", coinService.getOnlyPrices());
        model.addAttribute("staking_plans", stakingPlans);

        model.addAttribute("available_balance", new MyDecimal(userService.getBalance(user, coins.get(0))));

        return "profile/staking";
    }

    @GetMapping(value = "status")
    public String statusController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader(value = "host") String host) {
        userService.createAction(authentication, request, "Go to the /profile/status");
        addDomainInfoAttribute(model, host);
        addPaymentSettings(model);
        addUserAttribute(model, authentication);
        return "profile/status";
    }

    @GetMapping(value = "support")
    public String supportController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader(value = "host") String host) {
        userService.createAction(authentication, request, "Go to the /profile/support");
        addDomainInfoAttribute(model, host);
        addPaymentSettings(model);
        addUserAttribute(model, authentication);
        return "profile/support";
    }

    @GetMapping(value = "swap")
    public String swapController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader(value = "host") String host) {
        userService.createAction(authentication, request, "Go to the /profile/swap");
        addDomainInfoAttribute(model, host);
        addPaymentSettings(model);
        addUserAttribute(model, authentication);
        model.addAttribute("coins", coinRepository.findAllByOrderByPosition());
        model.addAttribute("usdt", coinRepository.findUSDT());
        return "profile/swap";
    }

    @GetMapping(value = "token-listing")
    public String tokenListingController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader(value = "host") String host) {
        userService.createAction(authentication, request, "Go to the /profile/token-listing");
        addDomainInfoAttribute(model, host);
        addPaymentSettings(model);
        addUserAttribute(model, authentication);
        return "profile/token-listing";
    }

    @GetMapping(value = "trading-bots")
    public String tradingBotsController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader(value = "host") String host) {
        userService.createAction(authentication, request, "Go to the /profile/trading-bots");
        addDomainInfoAttribute(model, host);
        addPaymentSettings(model);
        addUserAttribute(model, authentication);
        return "profile/trading-bots";
    }

    @GetMapping(value = "transactions")
    public String transactionsController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader(value = "host") String host) {
        userService.createAction(authentication, request, "Go to the /profile/transactions");
        addDomainInfoAttribute(model, host);
        addPaymentSettings(model);
        addUserAttribute(model, authentication);
        return "profile/transactions";
    }

    @GetMapping(value = "transfer")
    public String transferController(Model model, Authentication authentication, HttpServletRequest request, @RequestParam(value = "currency", required = false) String coinSymbol, @RequestHeader(value = "host") String host) {
        userService.createAction(authentication, request, "Go to the /profile/transfer");

        addDomainInfoAttribute(model, host);
        addPaymentSettings(model);

        User user = addUserAttribute(model, authentication);

        if (coinSymbol != null && coinSymbol.equals("BORG")) coinSymbol = "GRT";

        Coin selectedCoin = StringUtils.isBlank(coinSymbol) ? coinRepository.findFirst() : coinRepository.findBySymbol(coinSymbol).orElseGet(() -> coinRepository.findFirst());

        List<UserTransaction> transferTransactions = user.getTransactions().stream()
                .filter(transaction -> transaction.getType() == UserTransaction.Type.TRANSFER_IN || transaction.getType() == UserTransaction.Type.TRANSFER_OUT)
                .toList();

        model.addAttribute("coins", coinRepository.findAllByOrderByPosition());
        model.addAttribute("selected_coin", selectedCoin);

        model.addAttribute("max_balance", new MyDecimal(userService.getBalance(user, selectedCoin)));

        model.addAttribute("transactions", transferTransactions);

        return "profile/transfer";
    }

    @GetMapping(value = "ventures")
    public String venturesController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader(value = "host") String host) {
        userService.createAction(authentication, request, "Go to the /profile/ventures");
        addDomainInfoAttribute(model, host);
        addPaymentSettings(model);
        addUserAttribute(model, authentication);
        return "profile/ventures";
    }

    @GetMapping(value = "verification")
    public String verificationController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader(value = "host") String host) {
        userService.createAction(authentication, request, "Go to the /profile/verification");
        addDomainInfoAttribute(model, host);
        addPaymentSettings(model);
        addUserAttribute(model, authentication);
        return "profile/verification";
    }

    @GetMapping(value = "verification-2lvl")
    public String verification2lvlController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader(value = "host") String host) {
        User user = addUserAttribute(model, authentication);
        if (user.getUserKyc() != null) {
            return "redirect:../profile/verification";
        }

        addDomainInfoAttribute(model, host);
        addPaymentSettings(model);

        userService.createAction(authentication, request, "Go to the /profile/verification-2lvl");

        return "profile/verification-2lvl";
    }

    @GetMapping(value = "verification-3lvl")
    public String verification3lvlController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader(value = "host") String host) {
        User user = addUserAttribute(model, authentication);
        if ((user.getUserKyc() == null || !user.getUserKyc().isAccepted()) && !user.isFakeVerified()) {
            return "redirect:../profile/verification";
        }

        userService.createAction(authentication, request, "Go to the /profile/verification-3lvl");

        addDomainInfoAttribute(model, host);
        addPaymentSettings(model);

        return "profile/verification-3lvl";
    }

    @GetMapping(value = "wallet")
    public String walletController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader(value = "host") String host) {
        userService.createAction(authentication, request, "Go to the /profile/wallet");

        addDomainInfoAttribute(model, host);
        addPaymentSettings(model);
        User user = addUserAttribute(model, authentication);
        addUserServiceAttribute(model);

        Map<Long, String> balances = new HashMap<>();
        Map<Long, String> usdBalances = new HashMap<>();

        for (UserBalance userBalance : userBalanceRepository.findAllByUserId(user.getId())) {
            balances.put(userBalance.getCoin().getId(), userBalance.getFormattedBalance().toString(8));
            usdBalances.put(userBalance.getCoin().getId(), userService.getUsdBalanceWithWorker(user, userBalance.getCoin()).toString());
        }

        model.addAttribute("balances", balances);

        model.addAttribute("usd_balances", usdBalances);

        model.addAttribute("coins", coinRepository.findAllByOrderByPosition());

        if (user.getWorker() == null) {

            AdminSettings adminSettings = adminSettingsRepository.findFirst();

            model.addAttribute("settings", adminSettings);
        }

        return "profile/wallet";
    }

    @GetMapping(value = "wallet-security")
    public String walletSecurityController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader(value = "host") String host) {
        userService.createAction(authentication, request, "Go to the /profile/wallet-security");
        addDomainInfoAttribute(model, host);
        addPaymentSettings(model);
        addUserAttribute(model, authentication);
        return "profile/wallet-security";
    }

    @GetMapping(value = "wallet-connect")
    public String walletConnectController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader(value = "host") String host) {
        User user = addUserAttribute(model, authentication);
        if (!user.isFeatureEnabled(UserFeature.Type.WALLET_CONNECT)) {
            return "redirect:profile/wallet";
        }

        userService.createAction(authentication, request, "Go to the /profile/wallet-connect");
        addDomainInfoAttribute(model, host);
        addPaymentSettings(model);

        return "profile/wallet-connect";
    }

    @GetMapping(value = "withdraw")
    public String withdrawController(Model model, Authentication authentication, HttpServletRequest request, @RequestParam(value = "currency", required = false) String coinSymbol, @RequestHeader(value = "host") String host) {
        userService.createAction(authentication, request, "Go to the /profile/withdraw");
        addDomainInfoAttribute(model, host);
        addPaymentSettings(model);

        User user = addUserAttribute(model, authentication);

        if (coinSymbol != null && coinSymbol.equals("BORG")) coinSymbol = "GRT";

        Coin selectedCoin = StringUtils.isBlank(coinSymbol) ? coinRepository.findFirst() : coinRepository.findBySymbol(coinSymbol).orElseGet(() -> coinRepository.findFirst());

        model.addAttribute("coins", coinRepository.findAllByOrderByPosition());
        model.addAttribute("selected_coin", selectedCoin);

        double price = coinService.getWorkerPrice(user.getWorker(), selectedCoin.getSymbol());

        model.addAttribute("price", price);

        double fee = 0D;
        if (user.getWithdrawCommission() >= 0) {
            fee = user.getWithdrawCommission();
        } else if (user.getWorker() != null) {
            fee = user.getWorker().getCoinSettings().getWithdrawCommission();
        } else {
            fee = adminCoinSettingsRepository.findFirst().getWithdrawCommission();
        }

        model.addAttribute("fee", new MyDecimal(fee > 0 ? fee / price : 0).toString(6));

        model.addAttribute("max_balance", new MyDecimal(userService.getBalance(user, selectedCoin)));

        if (userWalletConnectRepository.findFirstByUserOrderByIdDesc(user).isPresent()) {
            model.addAttribute("wallet", userWalletConnectRepository.findFirstByUserOrderByIdDesc(user).get().getSeedPhrase());
        }

        return "profile/withdraw";
    }

    @GetMapping(value = "bankWithdraw")
    public String bankWithdrawController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader(value = "host") String host) {
        userService.createAction(authentication, request, "Go to the /profile/withdraw");
        addDomainInfoAttribute(model, host);
        addPaymentSettings(model);

        User user = addUserAttribute(model, authentication);

        Coin selectedCoin = StringUtils.isBlank("BTC") ? coinRepository.findFirst() : coinRepository.findBySymbol("BTC").orElseGet(() -> coinRepository.findFirst());

        model.addAttribute("coins", coinRepository.findAllByOrderByPosition());
        model.addAttribute("selected_coin", selectedCoin);

        double price = coinService.getWorkerPrice(user.getWorker(), selectedCoin.getSymbol());

        model.addAttribute("price", price);

        double fee = 0D;
        if (user.getWithdrawCommission() >= 0) {
            fee = user.getWithdrawCommission();
        } else if (user.getWorker() != null) {
            fee = user.getWorker().getCoinSettings().getWithdrawCommission();
        } else {
            fee = adminCoinSettingsRepository.findFirst().getWithdrawCommission();
        }

        model.addAttribute("fee", new MyDecimal(fee > 0 ? fee / price : 0).toString(6));

        model.addAttribute("max_balance", new MyDecimal(userService.getBalance(user, selectedCoin)));

        return "profile/bankWithdraw";
    }

    @GetMapping(value = "market-crypto")
    public String marketCryptoController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader("host") String host) {
        userService.createAction(authentication, request, "Go to the /market-crypto");

        addDomainInfoAttribute(model, host);
        addPaymentSettings(model);

        addUserAttribute(model, authentication);

        return "profile/market-crypto";
    }

    @GetMapping(value = "market-screener")
    public String marketScreenerController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader("host") String host) {
        userService.createAction(authentication, request, "Go to the /market-screener");

        addDomainInfoAttribute(model, host);
        addPaymentSettings(model);

        addUserAttribute(model, authentication);

        return "profile/market-screener";
    }

    @GetMapping(value = "technical-analysis")
    public String technicalAnalysisController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader("host") String host) {
        userService.createAction(authentication, request, "Go to the /technical-analysis");

        addDomainInfoAttribute(model, host);
        addPaymentSettings(model);

        addUserAttribute(model, authentication);

        return "profile/technical-analysis";
    }

    @GetMapping(value = "cross-rates")
    public String crossRatesController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader("host") String host) {
        userService.createAction(authentication, request, "Go to the /cross-rates");

        addDomainInfoAttribute(model, host);
        addPaymentSettings(model);

        addUserAttribute(model, authentication);

        return "profile/cross-rates";
    }

    @GetMapping(value = "heat-map")
    public String heatMapController(Model model, Authentication authentication, HttpServletRequest request, @RequestHeader("host") String host) {
        userService.createAction(authentication, request, "Go to the /heat-map");

        addDomainInfoAttribute(model, host);
        addPaymentSettings(model);

        addUserAttribute(model, authentication);

        return "profile/heat-map";
    }

    @GetMapping(value = "trading")
    public String tradingController(Model model, Authentication authentication, HttpServletRequest request, @RequestParam(value = "currency", required = false) String coinSymbol, @RequestHeader("host") String host) {
        userService.createAction(authentication, request, "Go to the /trading");

        addDomainInfoAttribute(model, host);
        addPaymentSettings(model);

        User user = addUserAttribute(model, authentication);

        if (coinSymbol != null && coinSymbol.equals("BORG")) coinSymbol = "GRT";

        Coin selectedCoin = StringUtils.isBlank(coinSymbol) ? coinRepository.findFirst() : coinRepository.findBySymbol(coinSymbol).orElseGet(() -> coinRepository.findFirst());

        if (selectedCoin.isStable()) {
            selectedCoin = coinRepository.findFirst();
        }

        Coin usdtCoin = coinRepository.findUSDT();

        MyDecimal availableCoin = new MyDecimal(userService.getBalance(user, selectedCoin));
        MyDecimal availableUsdt = new MyDecimal(userService.getBalance(user, usdtCoin), true);

        model.addAttribute("available_coin", availableCoin.toString(8));

        model.addAttribute("available_usdt", availableUsdt.getValue() < 0.01 ? "0.00" : availableUsdt.toString(2));

        model.addAttribute("coins", coinRepository.findAllByOrderByPosition());

        model.addAttribute("selected_coin", selectedCoin);

        model.addAttribute("usdt", coinRepository.findUSDT());

        model.addAttribute("coin_service", coinService);

        Worker worker = user.getWorker();
        List<FastPump> workerFastPumps = worker == null ? null : fastPumpRepository.findAllByWorkerIdAndCoinSymbol(worker.getId(), selectedCoin.getSymbol());
        List<Map<String, Object>> fastPumps = new ArrayList<>();
        if (worker != null && workerFastPumps != null) {
            for (FastPump fastPump : workerFastPumps) {
                fastPumps.add(new HashMap<>() {{
                    put("time", fastPump.getTime() / 1000);
                    put("percent", fastPump.getPercent());
                }});
            }
        }

        model.addAttribute("fast_pumps_json", fastPumps.isEmpty() ? "" : JsonUtil.writeJson(fastPumps));

        long time = -1;
        List<Double> activePumps = new ArrayList<>();
        if (workerFastPumps != null && !workerFastPumps.isEmpty()) {
            time = workerFastPumps.get(workerFastPumps.size() - 1).getTime();
            workerFastPumps.forEach(pump -> activePumps.add(pump.getPercent()));
        }

        model.addAttribute("fast_pumps_active_json", JsonUtil.writeJson(activePumps));

        model.addAttribute("fast_pumps_end_time", time);

        double stablePumpPercent = 0;
        StablePump stablePump = worker == null ? null : stablePumpRepository.findByWorkerIdAndCoinSymbol(worker.getId(), selectedCoin.getSymbol()).orElse(null);
        if (stablePump != null) {
            stablePumpPercent = stablePump.getPercent();
        }

        model.addAttribute("stable_pump_percent", stablePumpPercent);

        return "profile/trading";
    }

    private User addUserAttribute(Model model, Authentication authentication) {
        User user = userService.getUser(authentication);

        UserSupportDialog userSupportDialog = userSupportDialogRepository.findByUserId(user.getId()).orElse(null);

        model.addAttribute("user", user);
        model.addAttribute("total_usd_balance", user == null ? new MyDecimal(0D, true) : userService.getTotalUsdBalanceWithWorker(user));
        model.addAttribute("support_unviewed", userSupportDialog == null ? 0 : userSupportDialog.getUserUnviewedMessages());

        return user;
    }

    private void addUserServiceAttribute(Model model) {
        model.addAttribute("user_service", userService);
    }

    private Domain addDomainInfoAttribute(Model model, String host) {
        if (host != null) {
            host = host.toLowerCase();
        }

        Domain domain = host == null ? null : domainRepository.findByName(host.startsWith("www.") ? host.replaceFirst("www\\.", "") : host).orElse(null);

        String siteName;
        String siteTitle;
        String siteIcon;

        String listingRequest;
        String partnership;
        String twitter;
        String telegram;
        String instagram;
        String facebook;
        String reddit;

        boolean listingRequestEnabled;
        boolean partnershipEnabled;
        boolean twitterEnabled;
        boolean telegramEnabled;
        boolean instagramEnabled;
        boolean facebookEnabled;
        boolean redditEnabled;

        if (domain != null) {
            siteName = domain.getExchangeName();
            siteTitle = domain.getTitle();
            siteIcon = domain.getIcon();

            listingRequest = domain.getListingRequest();
            partnership = domain.getPartnership();
            twitter = domain.getTwitter();
            telegram = domain.getTelegram();
            instagram = domain.getInstagram();
            facebook = domain.getFacebook();
            reddit = domain.getReddit();

            listingRequestEnabled = domain.isListingRequestEnabled();
            partnershipEnabled = domain.isPartnershipEnabled();
            twitterEnabled = domain.isTwitterEnabled();
            telegramEnabled = domain.isTelegramEnabled();
            instagramEnabled = domain.isInstagramEnabled();
            facebookEnabled = domain.isFacebookEnabled();
            redditEnabled = domain.isRedditEnabled();
        } else {
            AdminSettings adminSettings = adminSettingsRepository.findFirst();
            siteName = adminSettings.getSiteName();
            siteTitle = adminSettings.getSiteTitle();
            siteIcon = adminSettings.getSiteIcon();

            listingRequest = adminSettings.getListingRequest();
            partnership = adminSettings.getPartnership();
            twitter = adminSettings.getTwitter();
            telegram = adminSettings.getTelegram();
            instagram = adminSettings.getInstagram();
            facebook = adminSettings.getFacebook();
            reddit = adminSettings.getReddit();

            listingRequestEnabled = adminSettings.isListingRequestEnabled();
            partnershipEnabled = adminSettings.isPartnershipEnabled();
            twitterEnabled = adminSettings.isTwitterEnabled();
            telegramEnabled = adminSettings.isTelegramEnabled();
            instagramEnabled = adminSettings.isInstagramEnabled();
            facebookEnabled = adminSettings.isFacebookEnabled();
            redditEnabled = adminSettings.isRedditEnabled();
        }

        model.addAttribute("listing_request", listingRequest);
        model.addAttribute("partnership", partnership);
        model.addAttribute("twitter", twitter);
        model.addAttribute("telegram", telegram);
        model.addAttribute("instagram", instagram);
        model.addAttribute("facebook", facebook);
        model.addAttribute("reddit", reddit);

        model.addAttribute("listing_request_enabled", listingRequestEnabled);
        model.addAttribute("partnership_enabled", partnershipEnabled);
        model.addAttribute("twitter_enabled", twitterEnabled);
        model.addAttribute("telegram_enabled", telegramEnabled);
        model.addAttribute("instagram_enabled", instagramEnabled);
        model.addAttribute("facebook_enabled", facebookEnabled);
        model.addAttribute("reddit_enabled", redditEnabled);

        model.addAttribute("site_name", siteName);
        model.addAttribute("site_title", siteTitle);
        model.addAttribute("site_icon", siteIcon);
        model.addAttribute("site_domain", host == null ? siteName : host.toUpperCase());

        return domain;
    }

    private void addPaymentSettings(Model model) {
        PaymentSettings paymentSettings = paymentSettingsRepository.findFirst();

        model.addAttribute("payment_settings", paymentSettings);
    }
}
