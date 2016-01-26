package sslengine;

import sslengine.utils.SSLUtils;

import javax.net.ssl.SSLContext;
import java.security.SecureRandom;

public class DemoServer {

    public static void main(String[] argv) {
        try {
            SSLContext context = SSLContext.getInstance("TLSv1.2");
            context.init(
                    SSLUtils.createKeyManagers("./src/main/resources/server.jks", "storepass", "keypass"),
                    SSLUtils.createTrustManagers("./src/main/resources/trustedCerts.jks", "storepass"),
                    new SecureRandom()
            );

            NioSslServerThreaded server = new NioSslServerThreaded("localhost", 9222, context);
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
