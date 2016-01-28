package sslengine;

import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import sslengine.utils.SSLUtils;

import javax.net.ssl.SSLContext;
import java.security.SecureRandom;

abstract public class BaseSSLTest {

    protected final Logger LOG = Logger.getLogger(getClass());

    protected static SSLContext clientContext;
    protected static SSLContext serverContext;

    @BeforeClass
    public static void initContext() throws Exception {
        //System.setProperty("javax.net.debug","ssl");

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
}
