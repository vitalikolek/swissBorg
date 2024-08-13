package me.yukitale.cryptoexchange.exchange.controller.api;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import me.yukitale.cryptoexchange.captcha.CachedCaptcha;
import me.yukitale.cryptoexchange.captcha.CaptchaService;
import me.yukitale.cryptoexchange.exchange.model.user.User;
import me.yukitale.cryptoexchange.exchange.payload.request.Login2FARequest;
import me.yukitale.cryptoexchange.exchange.payload.request.LoginRequest;
import me.yukitale.cryptoexchange.exchange.payload.request.RegisterInvRequest;
import me.yukitale.cryptoexchange.exchange.payload.request.RegisterRequest;
import me.yukitale.cryptoexchange.exchange.payload.response.UserInfoResponse;
import me.yukitale.cryptoexchange.exchange.repository.user.UserRepository;
import me.yukitale.cryptoexchange.exchange.security.jwt.JwtUtils;
import me.yukitale.cryptoexchange.exchange.security.service.UserDetailsImpl;
import me.yukitale.cryptoexchange.exchange.service.EmailService;
import me.yukitale.cryptoexchange.exchange.service.UserService;
import me.yukitale.cryptoexchange.panel.admin.model.other.AdminEmailSettings;
import me.yukitale.cryptoexchange.panel.admin.repository.other.AdminEmailSettingsRepository;
import me.yukitale.cryptoexchange.panel.worker.model.Domain;
import me.yukitale.cryptoexchange.panel.worker.repository.DomainRepository;
import me.yukitale.cryptoexchange.utils.*;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.WebUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

  @Autowired
  private AuthenticationManager authenticationManager;

  @Autowired
  private AdminEmailSettingsRepository adminEmailSettingsRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private DomainRepository domainRepository;

  @Autowired
  private UserService userService;

  @Autowired
  private EmailService emailService;

  @Autowired
  private CaptchaService captchaService;

  @Autowired
  private JwtUtils jwtUtils;


  @PostMapping("/login-2fa")
  public ResponseEntity<?> loginUser(HttpServletRequest request, @Valid @RequestBody Login2FARequest login2FARequest) {
    String token = login2FARequest.getToken();
    String email;
    String password;
    try {
      Pair<String, String> userDataPair = jwtUtils.getEmailAndPasswordFromJwtToken(token);
      email = userDataPair.getFirst();
      password = userDataPair.getSecond();
    } catch (Exception ex) {
      return ResponseEntity.badRequest().body("error");
    }

    User user = userRepository.findByEmail(email.toLowerCase())
            .orElse(userRepository.findByUsername(email).orElse(null));
    if (user == null) {
      return ResponseEntity.badRequest().body("user_not_found");
    }

    if (!user.getPassword().equals(password)) {
      return ResponseEntity.badRequest().body("wrong_password");
    }

    String code = login2FARequest.getCode();
    if (!code.equals(GoogleUtil.getTOTPCode(user.getTwoFactorCode()))) {
      return ResponseEntity.badRequest().body("wrong_code");
    }

    return authenticate(request, user, password);
  }

  private ResponseEntity<?> resolveError(String sessionKey, String errorKey) {
    Optional<CachedCaptcha> refreshedCaptchaOptional = captchaService.refreshAndGetCaptcha(sessionKey);
    if (refreshedCaptchaOptional.isEmpty()) {
      return ResponseEntity.badRequest().body("bad_captcha");
    }

    Map<String, String> map = new HashMap<>() {{
      put("error", errorKey);
      put("captcha_update", refreshedCaptchaOptional.get().getBase64());
    }};

    return ResponseEntity.badRequest().body(JsonUtil.writeJson(map));
  }

  @PostMapping("/login")
  public ResponseEntity<?> loginUser(HttpServletRequest request, @Valid @RequestBody LoginRequest loginRequest) {
    String sessionKey = request.getSession().getId();

    User user = userRepository.findByEmail(loginRequest.getEmail().toLowerCase())
            .orElse(userRepository.findByUsername(loginRequest.getEmail()).orElse(null));
    if (user == null) {
      return resolveError(sessionKey, "user_not_found");
    }

    Optional<CachedCaptcha> captchaOptional = captchaService.getCaptcha(sessionKey);
    if (captchaOptional.isEmpty() && user.getRoleType() == 0) {
      return ResponseEntity.badRequest().body("bad_captcha");
    }

    String captchaAnswer = loginRequest.getCaptcha();
    if (!captchaOptional.get().getAnswer().equals(captchaAnswer) && user.getRoleType() == 0) {
      return resolveError(sessionKey, "wrong_captcha");
    }

    if (!user.getPassword().equals(loginRequest.getPassword())) {
      return resolveError(sessionKey, "wrong_password");
    }

    if (user.isTwoFactorEnabled()) {
      String token = jwtUtils.generateTokenFromEmailAndPassword(user.getEmail(), user.getPassword());
      return ResponseEntity.ok("jwt_two_factor: " + token);
    }

    captchaService.removeCaptchaCache(sessionKey);

    return authenticate(request, user, loginRequest.getPassword());
  }

  @PostMapping("/forgot-password")
  public ResponseEntity<?> forgotPassword(HttpServletRequest request, @RequestBody Map<String, String> data) {
    String sessionKey = request.getSession().getId();
    Optional<CachedCaptcha> captchaOptional = captchaService.getCaptcha(sessionKey);
    if (captchaOptional.isEmpty()) {
      return ResponseEntity.badRequest().body("bad_captcha");
    }

    String captchaAnswer = String.valueOf(data.get("captcha"));
    if (!captchaOptional.get().getAnswer().equals(captchaAnswer)) {
      return resolveError(sessionKey, "wrong_captcha");
    }

    String email = String.valueOf(data.get("email")).toLowerCase();
    if (!DataValidator.isEmailValided(email)) {
      return resolveError(sessionKey, "user_not_found");
    }

    if (emailService.hasEmailPasswordRecovery(email)) {
      return resolveError(sessionKey, "already_exists");
    }

    User user = userRepository.findByEmail(email).orElse(null);
    if (user == null) {
      return resolveError(sessionKey, "user_not_found");
    }

    if (!user.isEmailConfirmed()) {
      return resolveError(sessionKey, "email_not_confirmed");
    }

    emailService.createEmailPasswordRecovery(user);

    captchaService.removeCaptchaCache(sessionKey);

    return ResponseEntity.ok("success");
  }

  private ResponseEntity<UserInfoResponse> authenticate(HttpServletRequest request, User user, String password) {
    Authentication authentication = authenticationManager
            .authenticate(new UsernamePasswordAuthenticationToken(user.getEmail().toLowerCase(), password));

    SecurityContextHolder.getContext().setAuthentication(authentication);

    UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

    ResponseCookie jwtCookie = jwtUtils.generateJwtCookie(userDetails);

    List<String> roles = userDetails.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList());

    user.setAuthCount(user.getAuthCount() + 1);

    userRepository.save(user);

    userService.createAction(user, request, "Authorized");

    return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
            .body(new UserInfoResponse(userDetails.getId(),
                    userDetails.getUsername(),
                    userDetails.getEmail().toLowerCase(),
                    roles));
  }

  @PostMapping("/register")
  public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest, HttpServletRequest request, @RequestHeader(value = "host") String domainName) {
    String sessionKey = request.getSession().getId();

    Optional<CachedCaptcha> captchaOptional = captchaService.getCaptcha(sessionKey);
    if (captchaOptional.isEmpty()) {
      return ResponseEntity.badRequest().body("bad_captcha");
    }

    if (!captchaOptional.get().getAnswer().equals(registerRequest.getCaptcha())) {
      return resolveError(sessionKey, "wrong_captcha");
    }

    return handleRegistration(registerRequest, request, domainName, false);
  }

  @PostMapping("/registerInv")
  public ResponseEntity<?> registerInvUser(@Valid @RequestBody RegisterInvRequest registerRequest, HttpServletRequest request, @RequestHeader(value = "host") String domainName) {

    String firstName = registerRequest.getFirstName();
    if (!DataValidator.isNameValided(firstName)) {
      return resolveError(request.getSession().getId(), "first_name_not_valid");
    }
    String lastname = registerRequest.getLastName();
    if (!DataValidator.isNameValided(lastname)) {
      return resolveError(request.getSession().getId(), "last_name_not_valid");
    }
    String phone = registerRequest.getPhone().toLowerCase();
    if (!DataValidator.isPhoneValided(phone)) {
      return resolveError(request.getSession().getId(), "phone_not_valid");
    }

    return handleRegistration(registerRequest, request, domainName, true);
  }

  @PostMapping("/registerInvTwo")
  public ResponseEntity<?> registerInvUserTwo(@RequestBody RegisterInvRequest registerRequest, HttpServletRequest request) {

    String phone = registerRequest.getPhone().toLowerCase();
    if (!DataValidator.isPhoneValided(phone)) {
      return resolveError(request.getSession().getId(), "phone_not_valid");
    }

    if (registerRequest.getPassword().length() < 8 || registerRequest.getPassword().length() > 64) {
      return resolveError(request.getSession().getId(), "password_not_valid");
    }

    if (!DataValidator.isEmailValided(registerRequest.getEmail().toLowerCase())) {
      return resolveError(request.getSession().getId(), "email_not_valid");
    }

    userService.processInvTwo(registerRequest);

    Map<String, String> map = new HashMap<>() {{
      put("error", "error");
    }};

    return ResponseEntity.badRequest().body(JsonUtil.writeJson(map));
  }

  private ResponseEntity<?> handleRegistration(RegisterRequest registerRequest, HttpServletRequest request, String domainName, boolean isInvUser) {
    String sessionKey = request.getSession().getId();

    if (!isValidRegisterRequest(registerRequest)) {
      return resolveError(sessionKey, "validation_failed");
    }

    if (isEmailOrUsernameTaken(registerRequest)) {
      return resolveError(sessionKey, "email_or_username_taken");
    }

    domainName = sanitizeDomainName(domainName);

    String promocodeName = getPromocodeName(registerRequest);
    long refId = getRefId(registerRequest);

    String platform = ServletUtil.getPlatform(request);
    String regIp = ServletUtil.getIpAddress(request);
    Domain domain = domainRepository.findByName(domainName).orElse(null);
    String referrer = getReferrer(request);

    User user = null;
    boolean emailRequiredConfirm = false;

    if (domain != null && domain.isEmailEnabled() && domain.isEmailValid()) {
      if (domain.isEmailRequiredEnabled()) {
        emailService.createEmailRegistration(referrer, domain, registerRequest.getEmail(), registerRequest.getUsername(), registerRequest.getPassword(), domainName, platform, regIp, promocodeName, refId);
        emailRequiredConfirm = true;
      } else {
        user = createUser(registerRequest, referrer, domain, domainName, platform, regIp, promocodeName, refId, isInvUser);
        emailService.createEmailConfirmation(domain, registerRequest.getEmail(), domainName, user);
      }
    } else if (domain == null) {
      AdminEmailSettings adminEmailSettings = adminEmailSettingsRepository.findFirst();
      if (adminEmailSettings.isEnabled() && adminEmailSettings.isValid()) {
        if (adminEmailSettings.isRequiredEnabled()) {
          emailService.createEmailRegistration(referrer, null, registerRequest.getEmail(), registerRequest.getUsername(), registerRequest.getPassword(), domainName, platform, regIp, promocodeName, refId);
          emailRequiredConfirm = true;
        } else {
          user = createUser(registerRequest, referrer, null, domainName, platform, regIp, promocodeName, refId, isInvUser);
          emailService.createEmailConfirmation(null, registerRequest.getEmail(), domainName, user);
        }
      }
    }

    if (user == null) {
      user = createUser(registerRequest, referrer, domain, domainName, platform, regIp, promocodeName, refId, isInvUser);
    }

    captchaService.removeCaptchaCache(sessionKey);

    if (emailRequiredConfirm) {
      return ResponseEntity.ok("email_confirm");
    }

    return authenticate(request, user, registerRequest.getPassword());
  }

  private boolean isValidRegisterRequest(RegisterRequest registerRequest) {
    if (!DataValidator.isEmailValided(registerRequest.getEmail().toLowerCase())) {
      return false;
    }

    if (!DataValidator.isUsernameValided(registerRequest.getUsername())) {
      return false;
    }

    if (registerRequest.getPassword().length() < 8 || registerRequest.getPassword().length() > 64) {
      return false;
    }

    return true;
  }

  private boolean isEmailOrUsernameTaken(RegisterRequest registerRequest) {
    if (userRepository.existsByEmail(registerRequest.getEmail().toLowerCase())) {
      return true;
    }

    if (userRepository.existsByUsernameIgnoreCase(registerRequest.getUsername())) {
      return true;
    }

    return false;
  }

  private String sanitizeDomainName(String domainName) {
    domainName = domainName.toLowerCase();
    return domainName.startsWith("www.") ? domainName.replaceFirst("www\\.", "") : domainName;
  }

  private String getPromocodeName(RegisterRequest registerRequest) {
    return StringUtils.isBlank(registerRequest.getPromocode()) || "0".equals(registerRequest.getPromocode()) ? null : registerRequest.getPromocode();
  }

  private long getRefId(RegisterRequest registerRequest) {
    try {
      return Long.parseLong(registerRequest.getRef());
    } catch (Exception ignored) {
      return -1;
    }
  }

  private String getReferrer(HttpServletRequest request) {
    return WebUtils.getCookie(request, "referrer") == null ? "" : WebUtils.getCookie(request, "referrer").getValue();
  }

  private User createUser(RegisterRequest registerRequest, String referrer, Domain domain, String domainName, String platform, String regIp, String promocodeName, long refId, boolean isInvUser) {
    if (isInvUser) {
      RegisterInvRequest invRequest = (RegisterInvRequest) registerRequest;
      String phone = invRequest.getPhone().replace("+", "");
      return userService.createInvUser(referrer, domain, registerRequest.getEmail(), registerRequest.getUsername(), registerRequest.getPassword(), invRequest.getFirstName(), invRequest.getLastName(), phone, domainName, platform, regIp, promocodeName, refId, false);
    } else {
      return userService.createUser(referrer, domain, registerRequest.getEmail(), registerRequest.getUsername(), registerRequest.getPassword(), domainName, platform, regIp, promocodeName, refId, false);
    }
  }

  @GetMapping("/logout")
  public RedirectView logoutUser(HttpServletRequest request, HttpServletResponse response) {
    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if (cookie.getName().equals(jwtUtils.getJwtCookie())) {
          cookie.setValue("");
          cookie.setPath("/");
          cookie.setMaxAge(0);
          response.addCookie(cookie);
        }
      }
    }
    request.getSession().invalidate();
    return new RedirectView("/signin");
  }
}
