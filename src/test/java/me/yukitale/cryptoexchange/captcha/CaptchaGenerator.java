package me.yukitale.cryptoexchange.captcha;

import me.yukitale.cryptoexchange.captcha.utils.MapPalette;
import me.yukitale.cryptoexchange.captcha.painter.CaptchaPainter;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;

public class CaptchaGenerator {
    
    private static final Random RANDOM = new Random();
    private static final Font[] FONTS = new Font[] {
                    new Font( Font.SANS_SERIF, Font.PLAIN, 50 ),
                    new Font( Font.SERIF, Font.PLAIN, 50 ),
                    new Font( Font.MONOSPACED, Font.BOLD, 50 )
            };

    public static void main(String[] args) throws IOException {
        CaptchaPainter captchaPainter = new CaptchaPainter();
        BufferedImage bufferedImage = captchaPainter.draw(FONTS[0], randomNotWhiteColor(), randomAnswer());
        File file = new File("C:/Users/cosmo/Desktop/test.jpg");
        ImageIO.write(bufferedImage, "jpg", file);
    }

    private static Color randomNotWhiteColor() {
        Color color = MapPalette.colors[RANDOM.nextInt(MapPalette.colors.length)];

        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();

        if (r == 255 && g == 255 && b == 255) {
            return randomNotWhiteColor();
        }
        if (r == 220 && g == 220 && b == 220) {
            return randomNotWhiteColor();
        }
        if (r == 199 && g == 199 && b == 199) {
            return randomNotWhiteColor();
        }
        if (r == 255 && g == 252 && b == 245) {
            return randomNotWhiteColor();
        }
        if (r == 220 && g == 217 && b == 211) {
            return randomNotWhiteColor();
        }
        if (r == 247 && g == 233 && b == 163) {
            return randomNotWhiteColor();
        }
        return color;
    }

    private static String randomAnswer() {
        if (RANDOM.nextBoolean()) {
            return Integer.toString(RANDOM.nextInt((99999 - 10000) + 1) + 10000);
        } else {
            return Integer.toString(RANDOM.nextInt((9999 - 1000) + 1) + 1000);
        }
    }
}
