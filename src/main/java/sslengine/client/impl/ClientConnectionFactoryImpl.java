package sslengine.client.impl;

import sslengine.client.ClientConnectionFactory;

import javax.net.ssl.SSLContext;

public class ClientConnectionFactoryImpl implements ClientConnectionFactory {

    private final String remoteAddress;
    private final int port;
    private final SSLContext context;

    protected ClientConnectionFactoryImpl(String remoteAddress, int port, SSLContext context) {
        this.remoteAddress = remoteAddress;
        this.port = port;
        this.context = context;
    }

    public static ClientConnectionFactory buildFactory(String remoteAddress, int port, SSLContext context) {
        return new ClientConnectionFactoryImpl(remoteAddress, port, context);
    }

    @Override
    public ClientConnection getConnection() {
        return new ClientConnection(this.remoteAddress, this.port, this.context);
    }
}
