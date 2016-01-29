package sslengine.example.map.server;

import org.apache.log4j.Logger;
import sslengine.SSLSocketChannelData;
import sslengine.SSLSocketLayer;
import sslengine.server.EventHandler;
import sslengine.server.ServerSocketProcessor;
import sslengine.server.SocketProcessorFactory;

import javax.net.ssl.SSLEngine;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentHashMap;

public class MtMapServerSocketProcessorFactory implements SocketProcessorFactory {

    @Override
    public ServerSocketProcessor create(SelectionKey key, ConcurrentHashMap<SelectionKey, Object> sessionKeys) throws Exception {

        return new MtMapServerSocketProcessor<SSLSocketChannelData>(
                new SSLSocketLayer(new SSLSocketChannelData((SocketChannel) key.channel(), (SSLEngine) key.attachment())),
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
