package sslengine;

import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import sslengine.client.AbstractClient;
import sslengine.client.ClientConnectionFactory;
import sslengine.client.SSLClientConnectionFactory;
import sslengine.example.simpleobject.client.SimpleClientHandler;
import sslengine.example.simpleobject.client.SimpleClientImpl;
import sslengine.example.simpleobject.dto.SimpleRequestDto;
import sslengine.example.simpleobject.dto.SimpleResponseDto;
import sslengine.example.simpleobject.server.SimpleSocketProcessorFactory;
import sslengine.server.SSLServerConnectionAcceptor;
import sslengine.utils.SSLUtils;

import javax.net.ssl.SSLContext;
import java.security.SecureRandom;
import java.util.Date;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class TestEngineSimple {

    protected final Logger LOG = Logger.getLogger(getClass());

    private static SSLContext clientContext;
    private static SSLContext serverContext;

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

    @org.junit.Test
    public void testMultiThread() throws Exception {
        ServerProcess server = ServerProcess.createInstance(new SSLServerConnectionAcceptor("localhost", 9222, new SimpleSocketProcessorFactory(), serverContext));

        ClientConnectionFactory clientConnectionFactory = SSLClientConnectionFactory.buildFactory("localhost", 9222, clientContext);
        Executor e = Executors.newFixedThreadPool(3);
        for (int i=0; i<4; i++) {
            e.execute(() -> {
                    try {
                        AbstractClient<SimpleRequestDto, SimpleResponseDto> client = new SimpleClientImpl(clientConnectionFactory, new SimpleClientHandler());
                        for (int j=0; j<3; j++) {
                            SimpleRequestDto requestDto = new SimpleRequestDto(new Date());
                            LOG.debug("REQ: " + requestDto);
                            SimpleResponseDto responseDto = client.call(requestDto);
                            LOG.debug("RES: " + responseDto);
                            assert requestDto.requestDate==responseDto.requestDto.requestDate;
                        }
                    } catch (Exception e1) {
                        LOG.error(e1.getMessage(), e1);
                    }
                }
            );
        }

        Thread.sleep(3000);

        server.stop();
    }
}
