package sslengine.server.impl;

import org.apache.log4j.Logger;
import sslengine.common.EventHandler;

import javax.net.ssl.SSLEngine;
import java.io.FileNotFoundException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentHashMap;

public class ServerSocketProcessorFactory {

    public static SimpleServerSocketProcessor create(SelectionKey key, ConcurrentHashMap<SelectionKey, Object> sessionKeys) throws FileNotFoundException {

        return new SimpleServerSocketProcessor(
                (SocketChannel) key.channel(),
                (SSLEngine) key.attachment(),
                new EventHandler() {
                    private final Logger LOG = Logger.getLogger(getClass());
                    @Override
                    public void onSuccessHandler() {
                        LOG.trace("removing lock from key: " + key);
                        sessionKeys.remove(key);
                    }

                    @Override
                    public void onErrorHandler(Exception e) {
                        LOG.error(e.getMessage(), e);
                        sessionKeys.remove(key);
                    }
                }
        );
    }
}
