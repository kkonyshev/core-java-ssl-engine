package sslengine;

import sslengine.server.ServerConnectionAcceprot;
import sslengine.utils.SSLUtils;

import javax.net.ssl.SSLContext;
import java.security.SecureRandom;

public class DemoServer {

    public static void main(String[] argv) {
        try {
            SSLContext context = SSLContext.getInstance("TLSv1.2");
            context.init(
                    SSLUtils.createKeyManagers("src/test/resources/server.private", "serverpw", "serverpw"),
                    SSLUtils.createTrustManagers("src/test/resources/client.public", "public"),
                    new SecureRandom()
            );

            ServerConnectionAcceprot server = new ServerConnectionAcceprot("localhost", 9222, context);
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
