package sslengine;

import sslengine.server.ServerConnectionAcceptor;
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

            ServerConnectionAcceptor server = new ServerConnectionAcceptor("localhost", 9222, context);
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
