package me.yukitale.test;

import me.yukitale.cryptoexchange.utils.HttpUtil;
import me.yukitale.cryptoexchange.utils.JsonUtil;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.HmacUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Main {

    public static void main(String[] args) throws Exception {
        System.out.println(new SimpleDateFormat("dd.MM.yyyy HH:mm").parse("15.12.2023 08:00").getTime());

        /*HttpGet httpGet = HttpUtil.createGet("https://api.westwallet.io/wallet/balances");

        signRequest(httpGet, null);

        try {
            CloseableHttpResponse httpResponse = HttpUtil.sendRequest(httpGet);

            String responseJson = HttpUtil.readAndCloseResponse(httpResponse);

            System.out.println(responseJson);
        } catch (Exception ex) {
            ex.printStackTrace();
        }*/
    }

    public static Map<String, Object> getTransactions() throws RuntimeException {
        Map<String, Object> data = new HashMap<>() {{
            put("type", "receive");
            put("order", "desc");
            put("limit", 30);
        }};

        String dataJson = JsonUtil.writeJson(data);

        HttpPost httpPost = HttpUtil.createPost("https://api.westwallet.io/wallet/transactions", dataJson);

        signRequest(httpPost, dataJson);

        try {
            CloseableHttpResponse httpResponse = HttpUtil.sendRequest(httpPost);

            String responseJson = HttpUtil.readAndCloseResponse(httpResponse);

            System.out.println(responseJson);

            return JsonUtil.readJson(responseJson, Map.class);
        } catch (Exception ex) {
            throw new RuntimeException("Ошибка получения последних транзакций WestWallet.io");
        }
    }

    private static void signRequest(HttpRequest httpRequest, String data) throws RuntimeException {
        long timestamp = Instant.now().getEpochSecond();

        String sign = Hex.encodeHexString(HmacUtils.hmacSha256("REudJzc4917seNmucidH9s4rmY1DADBuyBdYUzgVV2Wz0XEQ5XFbRQ".getBytes(),
                (timestamp + (data == null || data.isEmpty() ? "" : data)).getBytes()));

        httpRequest.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        httpRequest.addHeader("X-API-KEY", "qOqu7h_Q7EbwTa9wCF7MQc4nuoG3DRRY5Upftl5L");
        httpRequest.addHeader("X-ACCESS-SIGN", sign);
        httpRequest.addHeader("X-ACCESS-TIMESTAMP", String.valueOf(timestamp));
    }
}
