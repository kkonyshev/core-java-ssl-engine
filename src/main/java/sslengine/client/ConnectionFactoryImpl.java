package sslengine.client;

import sslengine.handler.HandshakeHandler;
import sslengine.utils.SSLUtils;

import javax.net.ssl.SSLContext;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class ConnectionFactoryImpl implements ConnectionFactory {

    private final String remoteAddress;
    private final int port;
    private final SSLContext clientContext;
    private final HandshakeHandler handshakeHandler;
    private SSLContext context;

    protected ConnectionFactoryImpl(String remoteAddress, int port, SSLContext clientContext) {
        this.remoteAddress = remoteAddress;
        this.port = port;
        this.clientContext = clientContext;
        this.handshakeHandler = new HandshakeHandler();
    }

    public static ConnectionFactory createFactory(String remoteAddress, int port, SSLContext clientContext) {
        return new ConnectionFactoryImpl(remoteAddress, port, clientContext);
    }

    @Override
    public ClientConnection getConnection() throws Exception {
        return new ClientConnection(remoteAddress, port, getContext(), handshakeHandler);
    }

    public SSLContext getContext() throws Exception {
        SSLContext context = SSLContext.getInstance("TLSv1.2");
        context.init(
                SSLUtils.createKeyManagers("src/test/resources/client.private", "clientpw", "clientpw"),
                SSLUtils.createTrustManagers("src/test/resources/server.public", "public"),
                new SecureRandom()
        );
        return context;
    }
}
