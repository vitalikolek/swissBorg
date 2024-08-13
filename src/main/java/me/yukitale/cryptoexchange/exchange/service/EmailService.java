package me.yukitale.cryptoexchange.exchange.service;

import jakarta.annotation.PostConstruct;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpServletRequest;
import me.yukitale.cryptoexchange.exchange.data.EmailPasswordRecovery;
import me.yukitale.cryptoexchange.exchange.data.EmailRegistration;
import me.yukitale.cryptoexchange.exchange.model.user.User;
import me.yukitale.cryptoexchange.exchange.model.user.UserEmailConfirm;
import me.yukitale.cryptoexchange.exchange.model.user.UserSupportDialog;
import me.yukitale.cryptoexchange.exchange.model.user.UserSupportMessage;
import me.yukitale.cryptoexchange.exchange.model.user.settings.UserFeature;
import me.yukitale.cryptoexchange.exchange.repository.user.UserEmailConfirmRepository;
import me.yukitale.cryptoexchange.exchange.repository.user.UserRepository;
import me.yukitale.cryptoexchange.exchange.repository.user.UserSupportDialogRepository;
import me.yukitale.cryptoexchange.exchange.repository.user.UserSupportMessageRepository;
import me.yukitale.cryptoexchange.exchange.security.xss.XSSUtils;
import me.yukitale.cryptoexchange.panel.admin.model.other.AdminEmailSettings;
import me.yukitale.cryptoexchange.panel.admin.model.other.AdminSettings;
import me.yukitale.cryptoexchange.panel.admin.repository.other.AdminEmailSettingsRepository;
import me.yukitale.cryptoexchange.panel.admin.repository.other.AdminSettingsRepository;
import me.yukitale.cryptoexchange.panel.worker.model.Domain;
import me.yukitale.cryptoexchange.panel.worker.repository.DomainRepository;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class EmailService {

    private final ExecutorService executor = Executors.newFixedThreadPool(3);
    private final Map<String, Pair<EmailRegistration, Long>> emailRegistrations = new ConcurrentHashMap<>();
    private final Map<String, Pair<EmailPasswordRecovery, Long>> emailPasswordRecoveries = new ConcurrentHashMap<>();

    @Autowired
    private AdminSettingsRepository adminSettingsRepository;

    @Autowired
    private AdminEmailSettingsRepository adminEmailSettingsRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserEmailConfirmRepository userEmailConfirmRepository;

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private CooldownService cooldownService;

    @Autowired
    private UserSupportDialogRepository userSupportDialogRepository;

    @Autowired
    private UserSupportMessageRepository userSupportMessageRepository;

    @PostConstruct
    public void init() {
        startClearTask();
    }

    private void startClearTask() {
        executor.execute(() -> {
            while (true) {
                long currentTime = System.currentTimeMillis();
                List<String> keysToRemove = new ArrayList<>();
                List<String> keysToRemove2 = new ArrayList<>();
                for (Map.Entry<String, Pair<EmailRegistration, Long>> entry : this.emailRegistrations.entrySet()) {
                    long time = entry.getValue().getSecond();
                    if (time < currentTime) {
                        keysToRemove.add(entry.getKey());
                    }
                }

                for (Map.Entry<String, Pair<EmailPasswordRecovery, Long>> entry : this.emailPasswordRecoveries.entrySet()) {
                    long time = entry.getValue().getSecond();
                    if (time < currentTime) {
                        keysToRemove2.add(entry.getKey());
                    }
                }

                keysToRemove.forEach(this.emailRegistrations::remove);
                keysToRemove2.forEach(this.emailPasswordRecoveries::remove);

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public EmailPasswordRecovery getEmailPasswordRecovery(String hash) {
        Pair<EmailPasswordRecovery, Long> pair = this.emailPasswordRecoveries.get(hash);
        if (pair == null) {
            return null;
        }

        return pair.getFirst();
    }

    public EmailRegistration getEmailRegistration(String hash) {
        Pair<EmailRegistration, Long> pair = this.emailRegistrations.get(hash);
        if (pair == null) {
            return null;
        }

        return pair.getFirst();
    }

    public void removeEmailPasswordRecovery(String hash) {
        this.emailPasswordRecoveries.remove(hash);
    }

    public void removeEmailRegistration(String hash) {
        this.emailRegistrations.remove(hash);
    }

    public boolean hasEmailPasswordRecovery(String email) {
        return this.emailPasswordRecoveries.values().stream().anyMatch(pair -> pair.getFirst().getEmail().equals(email));
    }

    public void createEmailPasswordRecovery(User user) {
        String hash = RandomStringUtils.random(32, true, true);

        String password = RandomStringUtils.random(16, true, true);
        EmailPasswordRecovery emailPasswordRecovery = new EmailPasswordRecovery(user.getEmail(), password);

        user.setPassword(password);

        userRepository.save(user);

        this.emailPasswordRecoveries.put(hash, Pair.of(emailPasswordRecovery, System.currentTimeMillis() + (60 * 60 * 1000)));

        sendEmailPasswordRecoveryAsync(user, password, hash);
    }

    private void sendEmailPasswordRecoveryAsync(User user, String password, String hash) {
        executor.execute(() -> {
            Domain domain = domainRepository.findByName(user.getDomain()).orElse(null);
            String domainName = user.getDomain();

            AdminEmailSettings adminEmailSettings = adminEmailSettingsRepository.findFirst();
            String title = adminEmailSettings.getPasswordRecoveryTitle();
            String html = adminEmailSettings.getPasswordRecoveryMessage();

            html = html.replace("{domain_url}", "https://" + domainName).replace("{confirm_url}", "https://" + domainName + "/email?action=password_recovery&hash=" + hash).replace("{password}", password);

            try {
                if (domain != null) {
                    title = title.replace("{domain_exchange_name}", domain.getExchangeName());
                    html = html.replace("{domain_exchange_name}", domain.getExchangeName());
                    sendEmail(domain, user.getEmail(), title, html);
                } else {
                    AdminSettings adminSettings = adminSettingsRepository.findFirst();
                    title = title.replace("{domain_exchange_name}", adminSettings.getSiteName());
                    html = html.replace("{domain_exchange_name}", adminSettings.getSiteName());
                    sendEmail(adminEmailSettings.getServer(), adminEmailSettings.getPort(), adminEmailSettings.getEmail(), adminEmailSettings.getPassword(), user.getEmail(), title, html);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                this.emailRegistrations.remove(hash);
            }
        });
    }

    public void createEmailConfirmation(Domain domain, String email, String domainName, User user) {
        String hash = RandomStringUtils.random(32, true, true);

        UserEmailConfirm userEmailConfirm = new UserEmailConfirm();
        userEmailConfirm.setHash(hash);
        userEmailConfirm.setUser(user);

        userEmailConfirmRepository.save(userEmailConfirm);

        sendEmailConfirmationAsync(domain, email, domainName, "confirmation&user_id=" + user.getId(), hash);
    }

    public void createEmailRegistration(String referrer, Domain domain, String email, String username, String password, String domainName, String platform, String regIp, String promocodeName, long refId) {
        String hash = RandomStringUtils.random(32, true, true);

        EmailRegistration emailRegistration = new EmailRegistration(referrer, email, username, password, domainName, platform, regIp, promocodeName, refId);
        this.emailRegistrations.put(hash, Pair.of(emailRegistration, System.currentTimeMillis() + (60 * 60 * 1000)));

        sendEmailConfirmationAsync(domain, email, domainName, "registration", hash);
    }

    private void sendEmailConfirmationAsync(Domain domain, String email, String domainName, String action, String hash) {
        executor.execute(() -> {
            AdminEmailSettings adminEmailSettings = adminEmailSettingsRepository.findFirst();
            String title = adminEmailSettings.getRegistrationTitle();
            String html = adminEmailSettings.getRegistrationMessage();

            html = html.replace("{domain_url}", "https://" + domainName).replace("{confirm_url}", "https://" + domainName + "/email?action=" + action + "&hash=" + hash);

            try {
                if (domain != null) {
                    title = title.replace("{domain_exchange_name}", domain.getExchangeName());
                    html = html.replace("{domain_exchange_name}", domain.getExchangeName());
                    sendEmail(domain, email, title, html);
                } else {
                    AdminSettings adminSettings = adminSettingsRepository.findFirst();
                    title = title.replace("{domain_exchange_name}", adminSettings.getSiteName());
                    html = html.replace("{domain_exchange_name}", adminSettings.getSiteName());
                    sendEmail(adminEmailSettings.getServer(), adminEmailSettings.getPort(), adminEmailSettings.getEmail(), adminEmailSettings.getPassword(), email, title, html);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                this.emailRegistrations.remove(hash);
            }
        });
    }

    public boolean validateEmail(String server, int port, String email, String password) {
        try {
            Properties properties = new Properties();
            properties.put("mail.smtp.host", server);
            properties.put("mail.smtp.port", String.valueOf(port));
            properties.put("mail.smtp.auth", "true");
            properties.put("mail.smtp.starttls.enable", "true");
            properties.put("mail.smtp.connectiontimeout", "3000");
            properties.put("mail.smtp.writetimeout", "1500");
            properties.put("mail.smtp.timeout", "1500");

            Session session = Session.getInstance(properties, new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(email, password);
                }
            });

            Transport transport = session.getTransport("smtp");
            transport.connect();
            transport.close();
            return true;
        } catch (MessagingException e) {
            System.out.println(e);
            e.printStackTrace();
            return false;
        }
    }

    public void sendEmail(Domain domainEmail, String toEmail, String subject, String htmlContent) throws RuntimeException {
        sendEmail(domainEmail.getServer(), domainEmail.getPort(), domainEmail.getEmail(), domainEmail.getPassword(), toEmail, subject, htmlContent);
    }

    private void sendEmail(String server, int port, String email, String password, String toEmail, String subject, String htmlContent) throws RuntimeException {
        try {
            Properties properties = new Properties();
            properties.put("mail.smtp.auth", "true");
            properties.put("mail.smtp.starttls.enable", "true");
            properties.put("mail.smtp.host", server);
            properties.put("mail.smtp.port", String.valueOf(port));
            //properties.put("mail.smtp.connectiontimeout", "1500");
            //properties.put("mail.smtp.writetimeout", "1500");
            //properties.put("mail.smtp.timeout", "1500");

            Session session = Session.getInstance(properties, new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(email, password);
                }
            });

            Message message = new MimeMessage(session);

            message.setFrom(new InternetAddress(email));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);
            message.setContent(htmlContent, "text/html; charset=utf-8");

            Transport.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public ResponseEntity<String> makePasswordRecoveryRequest(User user, HttpServletRequest request) {
        String message = "Send password recovery data to email";

        if (cooldownService.isCooldown(user.getId() + "-support")) {
            return ResponseEntity.badRequest().body("cooldown");
        }

        if (!user.isFeatureEnabled(UserFeature.Type.SUPPORT)) {
            return ResponseEntity.badRequest().body("support_ban");
        }

        if (StringUtils.isBlank(message)) {
            return ResponseEntity.badRequest().body("message_is_empty");
        }
        if (message.length() > 2000) {
            return ResponseEntity.badRequest().body("message_limit");
        }

        message = XSSUtils.stripXSS(message);

        UserSupportMessage supportMessage = new UserSupportMessage(UserSupportMessage.Target.TO_SUPPORT, UserSupportMessage.Type.TEXT, message, true, false, user);

        createOrUpdateSupportDialog(supportMessage, user);

        userSupportMessageRepository.save(supportMessage);

        userService.createAction(user, request, "Sended support message");

        cooldownService.addCooldown(user.getId() + "-support", Duration.ofMillis(500));

        return ResponseEntity.ok("success");
    }

    private void createOrUpdateSupportDialog(UserSupportMessage supportMessage, User user) {
        UserSupportDialog userSupportDialog = userSupportDialogRepository.findByUserId(user.getId()).orElse(null);
        if (userSupportDialog == null) {
            userSupportDialog = new UserSupportDialog();
        }

        userSupportDialog.setOnlyWelcome(false);
        userSupportDialog.setSupportUnviewedMessages(userSupportDialog.getSupportUnviewedMessages() + 1);
        userSupportDialog.setTotalMessages(userSupportDialog.getTotalMessages() + 1);
        userSupportDialog.setLastMessageDate(supportMessage.getCreated());
        userSupportDialog.setUser(user);

        userSupportDialogRepository.save(userSupportDialog);
    }
}
