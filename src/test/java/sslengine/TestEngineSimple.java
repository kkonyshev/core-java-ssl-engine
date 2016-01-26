package sslengine;

import org.apache.log4j.Logger;
import org.junit.BeforeClass;

import javax.net.ssl.SSLContext;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class TestEngineSimple {

    protected final Logger LOG = Logger.getLogger(getClass());

    private static SSLContext clientContext;
    private static SSLContext serverContext;

    private NioSslServerThreaded server;

    @BeforeClass
    public static void initContext() throws Exception {
        clientContext = SSLContext.getInstance("TLSv1.2");
        clientContext.init(
                SSLUtils.createKeyManagers("src/test/resources/client.private", "clientpw", "clientpw"),
                SSLUtils.createTrustManagers("src/test/resources/server.public", "public"),
                new SecureRandom()
        );

        serverContext = SSLContext.getInstance("TLSv1.2");
        serverContext.init(
                SSLUtils.createKeyManagers("src/test/resources/server.private", "serverpw", "serverpw"),
                SSLUtils.createTrustManagers("src/test/resources/client.public", "public"),
                new SecureRandom()
        );
    }

    @org.junit.Test
    public void testMultiThread() throws Exception {
        SSLServerProcess server = SSLServerProcess.createInstance(new NioSslServerThreaded("localhost", 9222, serverContext));

        Executor e = Executors.newFixedThreadPool(3);
        for (int i=0; i<5; i++) {
            e.execute(new Runnable() {
                @Override
                public void run() {
                    SSLClientWrapper client = null;
                    try {
                        client = SSLClientWrapper.wrap(new NioSslClientThreadLocal("localhost", 9222, clientContext));
                        for (int i=0; i<4; i++) {
                            String req1 = UUID.randomUUID().toString();
                            String res1 = new String(client.call(req1.getBytes()));
                            LOG.debug("RES: " + res1);
                            assert req1.equals(res1);
                        }
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    } finally {
                        client.finalize();
                    }
                }
            });
        }

        Thread.sleep(3000);

        server.stop();
    }
}
