package me.yukitale.cryptoexchange.captcha;

import com.pig4cloud.captcha.SpecCaptcha;
import com.pig4cloud.captcha.base.Captcha;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class CaptchaTest {

    public static void main(String[] args) throws IOException, FontFormatException {
        SpecCaptcha specCaptcha = new SpecCaptcha(130, 48, 5);
        specCaptcha.setCharType(Captcha.TYPE_ONLY_NUMBER);
        specCaptcha.setFont(Captcha.FONT_1);
        System.out.println(specCaptcha.toBase64());
        File file = new File("C:/Users/cosmo/Desktop/captcha.jpg");
        FileOutputStream fos = new FileOutputStream(file);
        specCaptcha.out(fos);

        file = new File("C:/Users/cosmo/Desktop/captcha2.jpg");
        fos = new FileOutputStream(file);
        specCaptcha.out(fos);
    }
}
