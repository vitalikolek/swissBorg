package me.yukitale.cryptoexchange.exchange.controller.error;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import me.yukitale.cryptoexchange.exchange.model.user.User;
import me.yukitale.cryptoexchange.exchange.service.UserService;
import me.yukitale.cryptoexchange.panel.admin.model.other.AdminSettings;
import me.yukitale.cryptoexchange.panel.admin.repository.other.AdminSettingsRepository;
import me.yukitale.cryptoexchange.panel.worker.model.Domain;
import me.yukitale.cryptoexchange.panel.worker.repository.DomainRepository;
import me.yukitale.cryptoexchange.utils.MyDecimal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@Controller
public class CustomErrorController implements ErrorController {

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private AdminSettingsRepository adminSettingsRepository;

    @Autowired
    private UserService userService;

    @GetMapping("/error")
    public String handleError(HttpServletRequest request, Model model, @RequestHeader("host") String host) {
        addDomainInfoAttribute(model, host);

        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        if (status != null) {
            Integer statusCode = Integer.valueOf(status.toString());

            if (statusCode == HttpStatus.NOT_FOUND.value()) {
                return "error_pages/404";
            } else {
                return "error_pages/500";
            }
        }

        return "error_pages/500";
    }

    private void addDomainInfoAttribute(Model model, String host) {
        Domain domain = host == null ? null : domainRepository.findByName(host.toLowerCase()).orElse(null);

        String siteName;
        String siteTitle;
        String siteIcon;

        if (domain != null) {
            siteName = domain.getExchangeName();
            siteTitle = domain.getTitle();
            siteIcon = domain.getIcon();
        } else {
            AdminSettings adminSettings = adminSettingsRepository.findFirst();
            siteName = adminSettings.getSiteName();
            siteTitle = adminSettings.getSiteTitle();
            siteIcon = adminSettings.getSiteIcon();
        }

        model.addAttribute("site_name", siteName);
        model.addAttribute("site_title", siteTitle);
        model.addAttribute("site_icon", siteIcon);
        model.addAttribute("site_domain", host == null ? siteName : host.toUpperCase());
    }
}
