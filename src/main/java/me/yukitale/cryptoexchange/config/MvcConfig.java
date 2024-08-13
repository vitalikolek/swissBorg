package me.yukitale.cryptoexchange.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Locale;

@Configuration
@EnableWebMvc
public class MvcConfig implements WebMvcConfigurer {

    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setFallbackToSystemLocale(false);
        return messageSource;
    }

    @Bean
    public SessionLocaleResolver localeResolver() {
        SessionLocaleResolver localeResolver = new SessionLocaleResolver();
        localeResolver.setDefaultLocale(Locale.ENGLISH);
        return localeResolver;
    }

    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor localeChangeInterceptor = new LocaleChangeInterceptor();
        localeChangeInterceptor.setParamName("lang");
        return localeChangeInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }

    private void exposeDirectory(String dirName, ResourceHandlerRegistry registry) {
        Path uploadDir = Paths.get(dirName);
        String uploadPath = uploadDir.toFile().getAbsolutePath();

        if (dirName.startsWith("../")) {
            dirName = dirName.replace("../", "");
        }

        registry.addResourceHandler("/" + dirName + "/**").addResourceLocations("file:" + uploadPath + "/").setCacheControl(cacheControl());
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        exposeDirectory(Resources.USER_PROFILES_PHOTO_DIR, registry);
        exposeDirectory(Resources.USER_KYC_PHOTO_DIR, registry);
        exposeDirectory(Resources.ADMIN_ICON_DIR, registry);
        exposeDirectory(Resources.ADMIN_COIN_ICONS_DIR, registry);
        exposeDirectory(Resources.DOMAIN_ICONS_DIR, registry);
        exposeDirectory(Resources.SUPPORT_IMAGES, registry);
        exposeDirectory(Resources.P2P_AVATARS, registry);

        registry.addResourceHandler("/**").addResourceLocations("classpath:/static/").setCacheControl(cacheControl());
        registry.addResourceHandler("/assets/**").addResourceLocations("classpath:/static/assets/").setCacheControl(cacheControl());
        registry.addResourceHandler("/fonts/**").addResourceLocations("classpath:/static/fonts/").setCacheControl(cacheControl());
        registry.addResourceHandler("/external-embedding/**").addResourceLocations("classpath:/static/external-embedding/").setCacheControl(cacheControl());
        registry.addResourceHandler("/landings/**").addResourceLocations("classpath:/static/landings/").setCacheControl(cacheControl());
        registry.addResourceHandler("/npm/**").addResourceLocations("classpath:/static/npm/").setCacheControl(cacheControl());
        registry.addResourceHandler("/trading_core/**").addResourceLocations("classpath:/static/trading_core/").setCacheControl(cacheControl());
    }

    private CacheControl cacheControl() {
        return CacheControl.maxAge(Duration.ofSeconds(3600));
    }
}
