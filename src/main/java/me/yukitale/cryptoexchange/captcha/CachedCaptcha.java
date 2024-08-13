package me.yukitale.cryptoexchange.captcha;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class CachedCaptcha {

    private final String answer;

    private final String base64;
}
