package reproducer.netty;


import javax.net.ssl.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;

public class SslContextBuilder {

    public static String PASSWORD = "password";

    public static SSLContext createJdkSSLContext(File keyStore) throws Exception {
        KeyManagerFactory kmf = kmf(keyStore);
        TrustManagerFactory tmf = tmf(keyStore);
        SSLContext sslc = SSLContext.getInstance("TLSv1.2");
        sslc.init(kmf.getKeyManagers(), tmf.getTrustManagers(), SecureRandom.getInstance("SHA1PRNG"));
        return sslc;

    }

    public static SSLEngine createSslEngine(SSLContext sslContext, String... cipherSuites) {
        SSLEngine sslEngine = sslContext.createSSLEngine();
        sslEngine.setUseClientMode(false);

        SSLParameters sslParameters = new SSLParameters();
        sslParameters.setNeedClientAuth(false);
        sslParameters.setCipherSuites(cipherSuites);
        sslParameters.setProtocols(new String[]{"TLSv1.2"});
        sslEngine.setSSLParameters(sslParameters);
        return sslEngine;
    }

    private static TrustManagerFactory tmf(File trustStore) throws Exception {
        final TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        final KeyStore keyStore = KeyStore.getInstance("JKS");
        try (final InputStream inputStream = new FileInputStream(trustStore)) {
            keyStore.load(inputStream, PASSWORD.toCharArray());
        }
        tmf.init(keyStore);
        return tmf;
    }

    private static KeyManagerFactory kmf(File file) throws Exception {
        final KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        KeyStore keyStore = KeyStore.getInstance("JKS");
        try (final InputStream inputStream = new FileInputStream(file)) {
            keyStore.load(inputStream, PASSWORD.toCharArray());
        }
        kmf.init(keyStore, PASSWORD.toCharArray());
        return kmf;
    }
}
