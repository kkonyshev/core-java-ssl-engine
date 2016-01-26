package sslengine;

import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import sslengine.client.ClientConnection;
import sslengine.client.ConnectionFactory;
import sslengine.client.ConnectionFactoryImpl;
import sslengine.handler.HandshakeHandler;
import sslengine.server.ServerConnectionAcceptor;
import sslengine.utils.SSLUtils;

import javax.net.ssl.SSLContext;
import java.security.SecureRandom;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class TestEngineSimple {

    protected final Logger LOG = Logger.getLogger(getClass());

    private static SSLContext clientContext;
    private static SSLContext serverContext;

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
        SSLServerProcess server = SSLServerProcess.createInstance(new ServerConnectionAcceptor("localhost", 9222, serverContext));

        ConnectionFactory clientConnectionFactory = ConnectionFactoryImpl.createFactory("localhost", 9222, clientContext);
        Executor e = Executors.newFixedThreadPool(3);
        for (int i=0; i<1; i++) {
            e.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        SSLClientWrapper client = SSLClientWrapper.wrap(clientConnectionFactory);
                        for (int i=0; i<2; i++) {
                            String req1 = UUID.randomUUID().toString();
                            //Thread.sleep(Math.abs(new Random().nextInt(1000)));
                            String res1 = new String(client.call(req1.getBytes()));
                            LOG.debug("RES: " + res1);
                            assert req1.equals(res1);
                        }
                    } catch (Exception e1) {
                        LOG.error(e1.getMessage(), e1);
                        throw new RuntimeException();
                    }
                }
            });
        }

        Thread.sleep(10000);
        //Thread.currentThread().join();

        server.stop();
    }
}
