package sslengine.server;


import java.nio.channels.SelectionKey;
import java.util.concurrent.ConcurrentHashMap;

public interface SocketProcessorFactory {
    SocketProcessor create(SelectionKey key, ConcurrentHashMap<SelectionKey, Object> sessionKeys) throws Exception;
}
