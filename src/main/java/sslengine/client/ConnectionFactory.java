package sslengine.client;

public interface ConnectionFactory {
    ClientConnection getConnection() throws Exception;
}
