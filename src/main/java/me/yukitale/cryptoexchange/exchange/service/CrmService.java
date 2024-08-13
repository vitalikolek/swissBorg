package me.yukitale.cryptoexchange.exchange.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.net.ssl.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.sql.Timestamp;

@Service
public class CrmService {

    private String hostname = "dusk.name";
    private int port = 443;

    @PostConstruct
    public void setupTrustStore() {
        try {
            X509Certificate certificate = downloadCertificate(hostname, port);

            String certFilePath = System.getProperty("java.io.tmpdir") + hostname + ".crt";
            saveCertificate(certificate, certFilePath);

            importCertificate(certFilePath, hostname);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private X509Certificate downloadCertificate(String hostname, int port) throws Exception {
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, new TrustManager[]{new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }
            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        }}, new java.security.SecureRandom());

        SSLSocketFactory factory = context.getSocketFactory();
        SSLSocket socket = (SSLSocket) factory.createSocket(hostname, port);
        socket.startHandshake();

        Certificate[] certs = socket.getSession().getPeerCertificates();
        if (certs.length > 0 && certs[0] instanceof X509Certificate) {
            return (X509Certificate) certs[0];
        } else {
            throw new SSLPeerUnverifiedException("No server certificates found");
        }
    }

    private void saveCertificate(X509Certificate certificate, String certFilePath) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(certFilePath)) {
            fos.write("-----BEGIN CERTIFICATE-----\n".getBytes());
            fos.write(java.util.Base64.getMimeEncoder(64, "\n".getBytes()).encode(certificate.getEncoded()));
            fos.write("\n-----END CERTIFICATE-----\n".getBytes());
        }
    }

    private void importCertificate(String certFilePath, String alias) throws Exception {
        String javaHome = System.getProperty("java.home");
        String keytoolPath = javaHome + "/bin/keytool";
        String cacertsPath = javaHome + "/lib/security/cacerts";
        String storepass = "changeit"; // Default password for the Java TrustStore

        ProcessBuilder processBuilder = new ProcessBuilder(
                keytoolPath,
                "-importcert",
                "-noprompt",
                "-trustcacerts",
                "-alias", alias,
                "-file", certFilePath,
                "-keystore", cacertsPath,
                "-storepass", storepass
        );

        Process process = processBuilder.start();
        process.waitFor();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }

        reader.close();
    }

    public void sendDataToCrm(String phone, String firstName, String lastName, String email) {
        String time = new Timestamp(System.currentTimeMillis()).toString().replace(" ", "---");

        String requestUrl = "https://dusk.name/api/importLead?api_key=4e6d83f795e1353ffe29afd1d679f6f6&phone=" +
                phone + "&namelastname=" + firstName + "%20" + lastName + "&email=" + email + "&otherinfo=" + time;
        try {
            URL url = new URL(requestUrl);

            // Open connection
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            System.out.println("Response Code: " + responseCode);

            if (responseCode != HttpURLConnection.HTTP_OK) {
                System.out.println("Error: Unable to send data to CRM. Response Code: " + responseCode);
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                System.out.println("Response: " + response.toString());
            } else {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                System.out.println("Response: " + response.toString());
            }

        } catch (java.net.UnknownHostException e) {
            System.out.println("Unknown Host: " + e.getMessage());
            System.out.println("Please check if the hostname is correct and the server is accessible.");
        } catch (javax.net.ssl.SSLHandshakeException e) {
            System.out.println("SSL Handshake failed: " + e.getMessage());
            System.out.println("This may be caused by an untrusted SSL certificate. Consider importing the certificate into the Java KeyStore.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
