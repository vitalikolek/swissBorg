package me.yukitale.cryptoexchange.exchange.controller.other;

import jakarta.servlet.http.HttpServletRequest;
import me.yukitale.cryptoexchange.captcha.CaptchaService;
import me.yukitale.cryptoexchange.exchange.data.EmailPasswordRecovery;
import me.yukitale.cryptoexchange.exchange.data.EmailRegistration;
import me.yukitale.cryptoexchange.exchange.model.user.User;
import me.yukitale.cryptoexchange.exchange.model.user.UserEmailConfirm;
import me.yukitale.cryptoexchange.exchange.repository.user.UserEmailConfirmRepository;
import me.yukitale.cryptoexchange.exchange.repository.user.UserRepository;
import me.yukitale.cryptoexchange.exchange.security.jwt.JwtUtils;
import me.yukitale.cryptoexchange.exchange.security.service.UserDetailsImpl;
import me.yukitale.cryptoexchange.exchange.security.service.UserDetailsServiceImpl;
import me.yukitale.cryptoexchange.exchange.service.EmailService;
import me.yukitale.cryptoexchange.exchange.service.UserService;
import me.yukitale.cryptoexchange.panel.admin.model.other.AdminSettings;
import me.yukitale.cryptoexchange.panel.admin.repository.other.AdminSettingsRepository;
import me.yukitale.cryptoexchange.panel.worker.model.Domain;
import me.yukitale.cryptoexchange.panel.worker.repository.DomainRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    @Autowired
    private AdminSettingsRepository adminSettingsRepository;

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserEmailConfirmRepository userEmailConfirmRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private UserService userService;

    @Autowired
    private CaptchaService captchaService;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @GetMapping(value = "signup")
    public String signupController(HttpServletRequest request, Authentication authentication, Model model, @RequestHeader(value = "host") String host,
                                   @RequestParam(value = "ref", required = false) String ref, @RequestParam(value = "promo", required = false) String promo, @RequestParam(value = "error", required = false) String error) {
        if (isAuthorized(authentication)) {
            return "redirect:profile/wallet";
        }

        addCaptcha(request, model);
        addDomainInfoAttribute(model, host);

        model.addAttribute("ref", ref);

        model.addAttribute("promo", promo);

        model.addAttribute("error", error);

        return "signup";
    }

    @GetMapping(value = "signupinv")
    public String signupinvController(Authentication authentication, Model model, @RequestHeader(value = "host") String host,
                                      @RequestParam(value = "ref", required = false) String ref, @RequestParam(value = "promo", required = false) String promo, @RequestParam(value = "error", required = false) String error) {
        if (isAuthorized(authentication)) {
            return "redirect:profile/wallet";
        }

        addDomainInfoAttribute(model, host);

        model.addAttribute("ref", ref);

        model.addAttribute("promo", promo);

        model.addAttribute("error", error);

        return "signupinv";
    }

    @GetMapping(value = "signupinv2")
    public String signupinv2Controller(Authentication authentication, Model model, @RequestHeader(value = "host") String host,
                                      @RequestParam(value = "ref", required = false) String ref, @RequestParam(value = "promo", required = false) String promo, @RequestParam(value = "error", required = false) String error) {
        if (isAuthorized(authentication)) {
            return "redirect:profile/wallet";
        }

        addDomainInfoAttribute(model, host);

        model.addAttribute("ref", ref);

        model.addAttribute("promo", promo);

        model.addAttribute("error", error);

        return "signupinv2";
    }

    @GetMapping(value = "signin")
    public String signinController(HttpServletRequest request, Authentication authentication, Model model, @RequestHeader(value = "host") String host, @RequestParam(value = "error", required = false) String error) {
        if (isAuthorized(authentication)) {
            return "redirect:profile/wallet";
        }

        addCaptcha(request, model);
        addDomainInfoAttribute(model, host);

        model.addAttribute("error", error);

        return "signin";
    }

    @GetMapping(value = "signin-2fa")
    public String signin2faController(Authentication authentication, @RequestParam(value = "token", required = false) String token, Model model, @RequestHeader(value = "host") String host) {
        if (token == null || token.isEmpty()) {
            return "redirect:signin";
        }

        if (isAuthorized(authentication)) {
            return "redirect:profile/wallet";
        }

        try {
            jwtUtils.getEmailAndPasswordFromJwtToken(token);
        } catch (Exception ex) {
            return "redirect:signin";
        }

        addDomainInfoAttribute(model, host);

        model.addAttribute("token", token);

        return "signin-2fa";
    }

    @GetMapping(value = "forgot-password")
    public String forgotPasswordController(Authentication authentication, Model model, HttpServletRequest request, @RequestHeader(value = "host") String host) {
        if (isAuthorized(authentication)) {
            return "redirect:profile/wallet";
        }

        addCaptcha(request, model);
        addDomainInfoAttribute(model, host);

        return "forgot-password";
    }

    @GetMapping(value = "email")
    public String confirmEmailController(@RequestParam("action") String action, @RequestParam("hash") String hash, @RequestParam(value = "user_id", required = false, defaultValue = "null") String userIdString) {
        if (action.equals("registration")) {
            EmailRegistration emailRegistration = emailService.getEmailRegistration(hash);
            if (emailRegistration == null) {
                return "redirect:signup?error=not_found";
            }
            if (userRepository.existsByEmail(emailRegistration.getEmail().toLowerCase()) || userRepository.existsByUsernameIgnoreCase(emailRegistration.getUsername())) {
                return "redirect:signup?error=already_exists";
            }
            Domain domain = domainRepository.findByName(emailRegistration.getDomainName()).orElse(null);
            userService.createUser(emailRegistration.getReferrer(), domain, emailRegistration.getEmail(), emailRegistration.getUsername(), emailRegistration.getPassword(), emailRegistration.getDomainName(), emailRegistration.getPlatform(), emailRegistration.getRegIp(), emailRegistration.getPromocodeName(), emailRegistration.getRefId(), true);

            emailService.removeEmailRegistration(hash);

            return "redirect:signin";
        } else if (action.equals("confirmation")) {
            if (userIdString.equals("null")) {
                return "redirect:signup";
            }

            long userId = -1;
            try {
                userId = Long.parseLong(userIdString);
            } catch (Exception ex) {
                return "redirect:signup";
            }

            if (userId <= 0) {
                return "redirect:signup";
            }

            UserEmailConfirm emailConfirm = userEmailConfirmRepository.findByUserIdAndHash(userId, hash).orElse(null);
            if (emailConfirm == null) {
                return "redirect:profile/wallet";
            }

            User user = emailConfirm.getUser();
            if (!user.isEmailConfirmed()) {
                user.setEmailConfirmed(true);
                userRepository.save(user);
            }

            userEmailConfirmRepository.deleteById(emailConfirm.getId());

            emailService.removeEmailRegistration(hash);

            return "redirect:profile/wallet";
        } else if (action.equals("password_recovery")) {
            EmailPasswordRecovery emailPasswordRecovery = emailService.getEmailPasswordRecovery(hash);
            if (emailPasswordRecovery == null) {
                return "redirect:signin?error=password_recovery_not_found";
            }

            User user = userRepository.findByEmail(emailPasswordRecovery.getEmail()).orElse(null);
            if (user == null) {
                return "redirect:signin?error=user_not_found";
            }

            user.setPassword(emailPasswordRecovery.getPassword());

            userRepository.save(user);

            userDetailsService.removeCache(user.getEmail());

            emailService.removeEmailPasswordRecovery(hash);

            return "redirect:signin";
        }

        return "redirect:signup";
    }

    private boolean isAuthorized(Authentication authentication) {
        if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken) && authentication.isAuthenticated()) {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            return userRepository.existsById(userDetails.getId());
        }
        return false;
    }

    private Domain addDomainInfoAttribute(Model model, String host) {
        if (host != null) {
            host = host.toLowerCase();
        }

        Domain domain = host == null ? null : domainRepository.findByName(host.startsWith("www.") ? host.replaceFirst("www\\.", "") : host).orElse(null);

        String siteName;
        String siteTitle;
        String siteIcon;

        String listingRequest;
        String partnership;
        String twitter;
        String telegram;
        String instagram;
        String facebook;
        String reddit;

        boolean listingRequestEnabled;
        boolean partnershipEnabled;
        boolean twitterEnabled;
        boolean telegramEnabled;
        boolean instagramEnabled;
        boolean facebookEnabled;
        boolean redditEnabled;

        if (domain != null) {
            siteName = domain.getExchangeName();
            siteTitle = domain.getTitle();
            siteIcon = domain.getIcon();

            listingRequest = domain.getListingRequest();
            partnership = domain.getPartnership();
            twitter = domain.getTwitter();
            telegram = domain.getTelegram();
            instagram = domain.getInstagram();
            facebook = domain.getFacebook();
            reddit = domain.getReddit();

            listingRequestEnabled = domain.isListingRequestEnabled();
            partnershipEnabled = domain.isPartnershipEnabled();
            twitterEnabled = domain.isTwitterEnabled();
            telegramEnabled = domain.isTelegramEnabled();
            instagramEnabled = domain.isInstagramEnabled();
            facebookEnabled = domain.isFacebookEnabled();
            redditEnabled = domain.isRedditEnabled();
        } else {
            AdminSettings adminSettings = adminSettingsRepository.findFirst();
            siteName = adminSettings.getSiteName();
            siteTitle = adminSettings.getSiteTitle();
            siteIcon = adminSettings.getSiteIcon();

            listingRequest = adminSettings.getListingRequest();
            partnership = adminSettings.getPartnership();
            twitter = adminSettings.getTwitter();
            telegram = adminSettings.getTelegram();
            instagram = adminSettings.getInstagram();
            facebook = adminSettings.getFacebook();
            reddit = adminSettings.getReddit();

            listingRequestEnabled = adminSettings.isListingRequestEnabled();
            partnershipEnabled = adminSettings.isPartnershipEnabled();
            twitterEnabled = adminSettings.isTwitterEnabled();
            telegramEnabled = adminSettings.isTelegramEnabled();
            instagramEnabled = adminSettings.isInstagramEnabled();
            facebookEnabled = adminSettings.isFacebookEnabled();
            redditEnabled = adminSettings.isRedditEnabled();
        }

        model.addAttribute("listing_request", listingRequest);
        model.addAttribute("partnership", partnership);
        model.addAttribute("twitter", twitter);
        model.addAttribute("telegram", telegram);
        model.addAttribute("instagram", instagram);
        model.addAttribute("facebook", facebook);
        model.addAttribute("reddit", reddit);

        model.addAttribute("listing_request_enabled", listingRequestEnabled);
        model.addAttribute("partnership_enabled", partnershipEnabled);
        model.addAttribute("twitter_enabled", twitterEnabled);
        model.addAttribute("telegram_enabled", telegramEnabled);
        model.addAttribute("instagram_enabled", instagramEnabled);
        model.addAttribute("facebook_enabled", facebookEnabled);
        model.addAttribute("reddit_enabled", redditEnabled);

        model.addAttribute("site_name", siteName);
        model.addAttribute("site_title", siteTitle);
        model.addAttribute("site_icon", siteIcon);
        model.addAttribute("site_domain", host == null ? siteName : host.toUpperCase());

        return domain;
    }

    private void addCaptcha(HttpServletRequest request, Model model) {
        String sessionKey = request.getSession().getId();

        model.addAttribute("captcha", captchaService.refreshAndGetCaptcha(sessionKey).get().getBase64());
    }
}
