package sslengine.client;

import java.io.IOException;

public interface ClientConnection {
    boolean connect() throws Exception;
    byte[] read() throws Exception;
    void write(byte[] data) throws IOException;
    void close() throws IOException;
}
