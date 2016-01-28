package sslengine.client;

import javax.net.ssl.SSLContext;

public class SSLClientConnectionFactory implements ClientConnectionFactory {

    private final String remoteAddress;
    private final int port;
    private final SSLContext context;

    protected SSLClientConnectionFactory(String remoteAddress, int port, SSLContext context) {
        this.remoteAddress = remoteAddress;
        this.port = port;
        this.context = context;
    }

    public static ClientConnectionFactory buildFactory(String remoteAddress, int port, SSLContext context) {
        return new SSLClientConnectionFactory(remoteAddress, port, context);
    }

    @Override
    public ClientConnection getConnection() {
        return new SSLClientConnection(this.remoteAddress, this.port, this.context);
    }
}
