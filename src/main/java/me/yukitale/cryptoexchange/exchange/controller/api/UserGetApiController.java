package me.yukitale.cryptoexchange.exchange.controller.api;


import jakarta.servlet.http.HttpServletResponse;
import me.yukitale.cryptoexchange.exchange.model.user.UserSupportDialog;
import me.yukitale.cryptoexchange.exchange.model.user.UserSupportMessage;
import me.yukitale.cryptoexchange.exchange.repository.user.UserSupportDialogRepository;
import me.yukitale.cryptoexchange.exchange.repository.user.UserSupportMessageRepository;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import me.yukitale.cryptoexchange.exchange.model.user.User;
import me.yukitale.cryptoexchange.exchange.model.user.UserTradeOrder;
import me.yukitale.cryptoexchange.exchange.repository.user.UserTradeOrderRepository;
import me.yukitale.cryptoexchange.exchange.service.UserService;

import java.util.List;

@Controller
@RequestMapping(value = "/api/user")
@PreAuthorize("hasRole('ROLE_USER') || hasRole('ROLE_WORKER') || hasRole('ROLE_ADMIN') || hasRole('ROLE_SUPPORTER')")
public class UserGetApiController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserSupportMessageRepository userSupportMessageRepository;

    @Autowired
    private UserSupportDialogRepository userSupportDialogRepository;

    @Autowired
    private UserTradeOrderRepository userTradeOrderRepository;

    //start support
    @GetMapping(value = "support/get")
    public String supportController(Authentication authentication, Model model) {
        User user = userService.getUser(authentication);

        List<UserSupportMessage> supportMessages = userSupportMessageRepository.findByUserIdOrderByIdDesc(user.getId());

        model.addAttribute("user", user);

        model.addAttribute("support_messages", supportMessages);

        UserSupportDialog userSupportDialog = userSupportDialogRepository.findByUserId(user.getId()).orElse(null);

        if (userSupportDialog != null && userSupportDialog.getUserUnviewedMessages() > 0) {
            userSupportDialog.setUserUnviewedMessages(0);
            userSupportDialogRepository.save(userSupportDialog);
        }

        userSupportMessageRepository.markUserViewedToTrueByUserId(user.getId());

        return "profile/get_user_support";
    }
    //end support

    //start trading
    @GetMapping(value = "trading")
    public String tradingController(Authentication authentication, Model model, @RequestParam(name = "action", defaultValue = "GET_OPEN_ORDERS") String action) {
        switch (action) {
            case "GET_OPEN_ORDERS": {
                return getOpenOrders(authentication, model);
            }
            case "GET_HISTORY_ORDERS": {
                return getHistoryOrders(authentication, model);
            }
            default: {
                return "500";
            }
        }
    }

    public String getOpenOrders(Authentication authentication, Model model) {
        User user = userService.getUser(authentication);

        List<UserTradeOrder> tradeOrders = userTradeOrderRepository.findByUserIdAndClosedOrderByCreatedDesc(user.getId(), false);
        for (UserTradeOrder order : tradeOrders) {
            if (order.getCoinSymbol().equals("GRT")) order.setCoinSymbol("BORG");
        }

        model.addAttribute("trade_orders", tradeOrders);

        return "profile/get_open_orders";
    }

    public String getHistoryOrders(Authentication authentication, Model model) {
        User user = userService.getUser(authentication);

        List<UserTradeOrder> tradeOrders = userTradeOrderRepository.findByUserIdAndClosedOrderByCreatedDesc(user.getId(), true);
        for (UserTradeOrder order : tradeOrders) {
            if (order.getCoinSymbol().equals("GRT")) order.setCoinSymbol("BORG");
        }

        model.addAttribute("trade_orders", tradeOrders);

        return "profile/get_history_orders";
    }
    //end trading
}
