package me.yukitale.cryptoexchange.utils;

import lombok.experimental.UtilityClass;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@UtilityClass
public class IOUtil {

    public String readResource(String resourceName) {
        return readInputStream(IOUtil.class.getResourceAsStream(resourceName));
    }

    public String readInputStream(InputStream inputStream) {
        return new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));
    }
}
