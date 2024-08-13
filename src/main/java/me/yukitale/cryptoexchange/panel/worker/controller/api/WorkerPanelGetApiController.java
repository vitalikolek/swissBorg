package me.yukitale.cryptoexchange.panel.worker.controller.api;

import me.yukitale.cryptoexchange.common.types.StatsType;
import me.yukitale.cryptoexchange.exchange.model.user.UserSupportDialog;
import me.yukitale.cryptoexchange.exchange.model.user.UserSupportMessage;
import me.yukitale.cryptoexchange.exchange.repository.user.UserRepository;
import me.yukitale.cryptoexchange.exchange.repository.user.UserSupportDialogRepository;
import me.yukitale.cryptoexchange.exchange.repository.user.UserSupportMessageRepository;
import me.yukitale.cryptoexchange.panel.common.data.WorkerTopStats;
import me.yukitale.cryptoexchange.panel.common.service.StatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import me.yukitale.cryptoexchange.exchange.model.user.User;
import me.yukitale.cryptoexchange.panel.worker.model.Worker;
import me.yukitale.cryptoexchange.panel.worker.service.WorkerService;

import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping(value = "/api/worker-panel")
@PreAuthorize("hasRole('ROLE_WORKER')")
public class WorkerPanelGetApiController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSupportMessageRepository userSupportMessageRepository;

    @Autowired
    private UserSupportDialogRepository userSupportDialogRepository;

    @Autowired
    private WorkerService workerService;

    @Autowired
    private StatsService statsService;

    //start support
    @GetMapping(value = "support/get")
    public String supportGetController(Authentication authentication, Model model, @RequestParam(name = "user_id") long userId) {
        Worker worker = workerService.getWorker(authentication);
        User user = userRepository.findByIdAndWorkerId(userId, worker.getId()).orElse(null);
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

        return "worker-panel/get_worker_support";
    }
    //end support

    //start stats
    @GetMapping(value = "stats")
    public String statsController(Model model, @RequestParam(name = "type", required = false, defaultValue = "TODAY") String typeName) {
        WorkerTopStats.Type type = Arrays.stream(WorkerTopStats.Type.values())
                .filter(type1 -> type1.name().equalsIgnoreCase(typeName))
                .findFirst()
                .orElse(WorkerTopStats.Type.TODAY);

        model.addAttribute("all_stats", statsService.getAllWorkerStats(type));

        return "worker-panel/get_statistic";
    }

    @GetMapping(value = "detailed-stats")
    public String detailedStatsController(Authentication authentication, Model model, @RequestParam(name = "type", required = false, defaultValue = "ALL") String typeName, @RequestParam(name = "stats_type", required = false, defaultValue = "userCountries") String statsTypeName) {
        Worker worker = workerService.getWorker(authentication);
        if (worker == null) {
            return "500";
        }

        StatsType type = Arrays.stream(StatsType.values())
                .filter(type1 -> type1.name().equalsIgnoreCase(typeName))
                .findFirst()
                .orElse(StatsType.ALL);

        String statsType = statsTypeName == null || (!statsTypeName.equals("depositCountries") && !statsTypeName.equals("depositCoins") && !statsTypeName.equals("userRefers")) ? "userCountries" : statsTypeName;

        model.addAttribute("detailed_stats", statsService.getWorkerDetailedStats(worker, type));

        model.addAttribute("stats_type", statsType);

        return "worker-panel/get_detailed_statistic";
    }
    //end stats
}
