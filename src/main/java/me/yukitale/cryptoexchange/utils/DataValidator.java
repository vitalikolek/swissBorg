package me.yukitale.cryptoexchange.utils;

import lombok.experimental.UtilityClass;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.regex.Pattern;

@UtilityClass
public class DataValidator {

    private final Pattern DOMAIN_REGEX = Pattern.compile("^(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}$", Pattern.CASE_INSENSITIVE);
    private final Pattern EMAIL_PATTERN = Pattern.compile("[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}$", Pattern.CASE_INSENSITIVE);
    private final Pattern USERNAME_PATTERN = Pattern.compile("^[a-z0-9-_.]{5,32}$", Pattern.CASE_INSENSITIVE);
    private final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z]{2,32}$", Pattern.CASE_INSENSITIVE);
    private final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9]{9,15}$", Pattern.CASE_INSENSITIVE);

    private final char[] ALLOWED_SYMBOLS = new char[] { '"', '-', '_', '.', '!', '$', '(', ')', ',', '+',
            '=', '&', '*', '@', '#', ';', ':', '%', '/', '\\' };

    private boolean isAllowedSymbol(char c) {
        for (char allowedSymbol : ALLOWED_SYMBOLS) {
            if (c == allowedSymbol) {
                return true;
            }
        }
        return false;
    }

    public boolean isTextValided(String text) {
        return text.chars().noneMatch(c -> !Character.isLetter(c) && !Character.isDigit(c) && c != ' ' && !isAllowedSymbol((char) c));
    }

    public boolean isTextValidedWithoutSymbols(String text) {
        return text.chars().noneMatch(c -> !Character.isLetter(c) && !Character.isDigit(c));
    }

    public boolean isTextValidedLowest(String text) {
        return text.chars().noneMatch(c -> !Character.isLetter(c) && !Character.isDigit(c) && c != ' ' && c != '.' && c != ',' && c != '(' && c != ')' && c != '|' && c != '[' && c != ']' && c != '&' && c != '#' && c != '!');
    }

    public boolean isDomainValided(String domain) {
        return DOMAIN_REGEX.matcher(domain).matches();
    }

    public boolean isEmailValided(String email) {
        return EMAIL_PATTERN.matcher(email.toLowerCase()).matches();
    }

    public boolean isPhoneValided(String phone) {
        return PHONE_PATTERN.matcher(phone.toLowerCase()).matches();
    }

    public boolean isUsernameValided(String username) {
        return USERNAME_PATTERN.matcher(username.toLowerCase()).matches();
    }

    public boolean isNameValided(String username) {
        return NAME_PATTERN.matcher(username.toLowerCase()).matches();
    }

    public boolean isValidImage(MultipartFile file) {
        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            return image != null;
        } catch (IOException e) {
            return false;
        }
    }
}
