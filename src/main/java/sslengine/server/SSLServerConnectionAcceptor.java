package sslengine.server;

import org.apache.log4j.Logger;
import sslengine.HandshakeHandler;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class SSLServerConnectionAcceptor extends ServerConnectionAcceptor {

    private SSLContext context;

    public SSLServerConnectionAcceptor(String hostAddress, int port, SocketProcessorFactory socketProcessorFactory, SSLContext context) throws Exception {
        super(hostAddress, port, socketProcessorFactory);
        this.context = context;
    }

    @Override
    protected void accept(SelectionKey key) throws Exception {
        SocketChannel socketChannel = ((ServerSocketChannel) key.channel()).accept();
        LOG.info("Receiving connection from: " + socketChannel.getRemoteAddress());
        socketChannel.configureBlocking(false);

        SSLEngine engine = context.createSSLEngine();
        engine.setUseClientMode(false);
        engine.beginHandshake();

        if (HandshakeHandler.doHandshake(socketChannel, engine)) {
            socketChannel.register(selector, SelectionKey.OP_READ, engine);
        } else {
            socketChannel.close();
            sessionKeys.remove(key);
            LOG.warn("Connection closed due to handshake failure.");
        }
    }
}
