package sslengine.client;

public class PlainClientConnectionFactory implements ClientConnectionFactory {

    private final String remoteAddress;
    private final int port;

    protected PlainClientConnectionFactory(String remoteAddress, int port) {
        this.remoteAddress = remoteAddress;
        this.port = port;
    }

    public static ClientConnectionFactory buildFactory(String remoteAddress, int port) {
        return new PlainClientConnectionFactory(remoteAddress, port);
    }

    @Override
    public ClientConnection getConnection() {
        return new PlainClientConnection(this.remoteAddress, this.port);
    }
}
