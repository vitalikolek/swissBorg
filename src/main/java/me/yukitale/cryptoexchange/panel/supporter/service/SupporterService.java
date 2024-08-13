package me.yukitale.cryptoexchange.panel.supporter.service;

import me.yukitale.cryptoexchange.exchange.model.user.User;
import me.yukitale.cryptoexchange.exchange.model.user.UserRole;
import me.yukitale.cryptoexchange.exchange.model.user.UserRoleType;
import me.yukitale.cryptoexchange.exchange.repository.user.RoleRepository;
import me.yukitale.cryptoexchange.exchange.repository.user.UserRepository;
import me.yukitale.cryptoexchange.exchange.security.service.UserDetailsServiceImpl;
import me.yukitale.cryptoexchange.panel.admin.model.other.*;
import me.yukitale.cryptoexchange.panel.admin.repository.other.AdminSupportPresetRepository;
import me.yukitale.cryptoexchange.panel.supporter.model.Supporter;
import me.yukitale.cryptoexchange.panel.supporter.model.settings.SupporterSupportPreset;
import me.yukitale.cryptoexchange.panel.supporter.repository.SupporterRepository;
import me.yukitale.cryptoexchange.panel.supporter.repository.settings.SupporterSupportPresetsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class SupporterService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private SupporterRepository supporterRepository;

    @Autowired
    private SupporterSupportPresetsRepository supporterSupportPresetsRepository;

    @Autowired
    private AdminSupportPresetRepository adminSupportPresetRepository;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Transactional
    public Supporter createSupporter(User user) {
        Set<UserRole> userRoles = new HashSet<>();
        userRoles.add(roleRepository.findByName(UserRoleType.ROLE_USER).orElseThrow());
        userRoles.add(roleRepository.findByName(UserRoleType.ROLE_SUPPORTER).orElseThrow());

        user.setUserRoles(userRoles);
        user.setRoleType(UserRoleType.ROLE_SUPPORTER.ordinal());

        userRepository.save(user);

        userDetailsService.removeCache(user.getEmail());

        Supporter supporter = new Supporter();
        supporter.setSupportPresetsEnabled(true);
        supporter.setUser(user);

        supporterRepository.save(supporter);

        List<AdminSupportPreset> adminSupportPresets = adminSupportPresetRepository.findAll();
        for (AdminSupportPreset adminSupportPreset : adminSupportPresets) {
            SupporterSupportPreset supporterSupportPreset = new SupporterSupportPreset();
            supporterSupportPreset.setTitle(adminSupportPreset.getTitle());
            supporterSupportPreset.setMessage(adminSupportPreset.getMessage());
            supporterSupportPreset.setSupporter(supporter);

            supporterSupportPresetsRepository.save(supporterSupportPreset);
        }

        return supporter;
    }

    @Transactional
    public void deleteSupporter(Supporter supporter) {
        User user = supporter.getUser();

        Set<UserRole> userRoles = new HashSet<>();
        userRoles.add(roleRepository.findByName(UserRoleType.ROLE_USER).orElseThrow());

        user.setUserRoles(userRoles);
        user.setRoleType(UserRoleType.ROLE_USER.ordinal());

        userRepository.save(user);

        userDetailsService.removeCache(user.getEmail());

        supporterSupportPresetsRepository.deleteAllBySupporterId(supporter.getId());

        supporterRepository.deleteById(supporter.getId());
    }
}
