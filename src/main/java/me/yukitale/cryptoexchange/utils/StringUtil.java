package me.yukitale.cryptoexchange.utils;

import lombok.experimental.UtilityClass;

import java.text.SimpleDateFormat;
import java.util.Date;

@UtilityClass
public class StringUtil {

    private final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    private final SimpleDateFormat DATE_FORMAT_WITHOUT_SECONDS = new SimpleDateFormat("yyyy/MM/dd HH:mm");

    private final SimpleDateFormat DATE_FORMAT_WITHOUT_YEAR = new SimpleDateFormat("MM/dd HH:mm");

    public String formatDate(Date date) {
        return DATE_FORMAT.format(date);
    }

    public String formatDateWithoutYears(Date date) {
        return DATE_FORMAT_WITHOUT_YEAR.format(date);
    }

    public String formatDateWithoutSeconds(Date date) {
        return DATE_FORMAT_WITHOUT_SECONDS.format(date);
    }
}
