package sslengine.client;

import sslengine.client.impl.ClientConnection;

public interface ClientConnectionFactory {
    ClientConnection getConnection();
}
