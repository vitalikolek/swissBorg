package me.yukitale.cryptoexchange.exchange.security;

import me.yukitale.cryptoexchange.exchange.security.jwt.AuthEntryPointJwt;
import me.yukitale.cryptoexchange.exchange.security.jwt.AuthTokenFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import me.yukitale.cryptoexchange.exchange.security.service.UserDetailsServiceImpl;
import me.yukitale.cryptoexchange.exchange.security.utils.NoOpPasswordEncoder;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableMethodSecurity()
public class WebSecurityConfig {

  @Autowired
  private UserDetailsServiceImpl userDetailsService;

  @Autowired
  private AuthEntryPointJwt unauthorizedHandler;

  @Bean
  public AuthTokenFilter authenticationJwtTokenFilter() {
    return new AuthTokenFilter();
  }

  @Bean
  public DaoAuthenticationProvider authenticationProvider() {
    DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();

    authProvider.setUserDetailsService(userDetailsService);
    authProvider.setPasswordEncoder(passwordEncoder());

    return authProvider;
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
    return authConfig.getAuthenticationManager();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new NoOpPasswordEncoder();
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.headers(headers ->
            headers.xssProtection(
                    xss -> xss.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK)
            ));

    http.cors(withDefaults())
            .csrf(AbstractHttpConfigurer::disable)
            .exceptionHandling(exception -> {
              exception.authenticationEntryPoint(unauthorizedHandler);
            }).sessionManagement(session -> {
              session.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
            }).authorizeHttpRequests(auth -> {
              auth.requestMatchers("/api/worker-panel/**").authenticated()
                      .requestMatchers("/api/admin-panel/**").authenticated()
                      .requestMatchers("/api/supporter-panel/**").authenticated()
                      .requestMatchers("/api/user/**").authenticated()
                      .requestMatchers("/trading").authenticated()
                      .requestMatchers("/trade-bot").authenticated()
                      .requestMatchers("/profile/**").authenticated()
                      .requestMatchers("/worker-panel/**").authenticated()
                      .requestMatchers("/admin-panel/**").authenticated()
                      .requestMatchers("/supporter-panel/**").authenticated()
                      .requestMatchers("/bugbounty").authenticated()
                      .requestMatchers("/cross-rates").authenticated()
                      .requestMatchers("/fees").authenticated()
                      .requestMatchers("/heat-map").authenticated()
                      .requestMatchers("/indices").authenticated()
                      .requestMatchers("/market-crypto").authenticated()
                      .requestMatchers("/market-screener").authenticated()
                      .requestMatchers("/technical-analysis").authenticated()
                      .anyRequest().permitAll();

              /*auth.requestMatchers("/").permitAll()
                      .requestMatchers("/error").permitAll()
                      .requestMatchers("/404").permitAll()
                      .requestMatchers("/banned").permitAll()
                      .requestMatchers("/api/auth/**").permitAll()
                      .requestMatchers("/api/exchange").permitAll()
                      .requestMatchers("/signin").permitAll()
                      .requestMatchers("/signup").permitAll()
                      .requestMatchers("/signin-2fa").permitAll()
                      .requestMatchers("/email").permitAll()
                      .requestMatchers("/forgot-password").permitAll()

                      .requestMatchers("/user_profiles_photo/**").permitAll()
                      .requestMatchers("/user_kyc_photo/**").permitAll()
                      .requestMatchers("/admin_icon_dir/**").permitAll()
                      .requestMatchers("/admin_coin_icons/**").permitAll()
                      .requestMatchers("/domain_icons/**").permitAll()
                      .requestMatchers("/support_images/**").permitAll()
                      .requestMatchers("/p2p_avatars/**").permitAll()

                      .requestMatchers("/assets/**").permitAll()
                      .requestMatchers("/external-embedding/**").permitAll()
                      .requestMatchers("/fonts/**").permitAll()
                      .requestMatchers("/landings/**").permitAll()
                      .requestMatchers("/npm/**").permitAll()
                      .requestMatchers("/trading_core/**").permitAll()
                      .requestMatchers("/css2").permitAll()
                      .requestMatchers("/robots.txt").permitAll()
                      .anyRequest().authenticated();*/
            });

    http .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));


            http.authenticationProvider(authenticationProvider());

    http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }
}
