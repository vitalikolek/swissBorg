package me.yukitale.cryptoexchange.panel.supporter.controller.api;

import me.yukitale.cryptoexchange.exchange.model.user.User;
import me.yukitale.cryptoexchange.exchange.model.user.UserSupportDialog;
import me.yukitale.cryptoexchange.exchange.model.user.UserSupportMessage;
import me.yukitale.cryptoexchange.exchange.repository.user.UserRepository;
import me.yukitale.cryptoexchange.exchange.repository.user.UserSupportDialogRepository;
import me.yukitale.cryptoexchange.exchange.repository.user.UserSupportMessageRepository;
import me.yukitale.cryptoexchange.panel.common.data.WorkerTopStats;
import me.yukitale.cryptoexchange.panel.common.service.StatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping(value = "/api/supporter-panel")
@PreAuthorize("hasRole('ROLE_SUPPORTER')")
public class SupporterPanelGetApiController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSupportMessageRepository userSupportMessageRepository;

    @Autowired
    private UserSupportDialogRepository userSupportDialogRepository;

    //start support
    @GetMapping(value = "support/get")
    public String supportGetController(Model model, @RequestParam(name = "user_id") long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return "500";
        }

        List<UserSupportMessage> supportMessages = userSupportMessageRepository.findByUserIdOrderByIdDesc(user.getId());

        UserSupportDialog userSupportDialog = userSupportDialogRepository.findByUserId(userId).orElse(null);

        model.addAttribute("user", user);

        model.addAttribute("support_messages", supportMessages);

        if (userSupportDialog != null && userSupportDialog.getSupportUnviewedMessages() > 0) {
            userSupportDialog.setSupportUnviewedMessages(0);
            userSupportDialogRepository.save(userSupportDialog);
        }

        userSupportMessageRepository.markSupportViewedToTrueByUserId(user.getId());

        return "admin-panel/get_admin_support";
    }
    //end support
}
