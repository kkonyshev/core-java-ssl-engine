package sslengine.server.impl;

import org.apache.log4j.Logger;
import sslengine.common.HandshakeHandler;

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


public class ServerConnectionAcceptor {

    private final Logger LOG = Logger.getLogger(getClass());

    private SSLContext context;
    private Selector selector;
    private boolean active;

    private ConcurrentHashMap<SelectionKey, Object> sessionKeys = new ConcurrentHashMap<>();
    private ExecutorService acceptorService = Executors.newCachedThreadPool();

    public ServerConnectionAcceptor(String hostAddress, int port, SSLContext context) throws Exception {
        this.context = context;

        selector = SelectorProvider.provider().openSelector();
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.socket().bind(new InetSocketAddress(hostAddress, port));
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        active = true;
    }

    public void start() throws Exception {
    	LOG.debug("Initialized and waiting for new connections...");
        while (isActive()) {
            selector.select();
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            LOG.trace("keys: " + selectionKeys.size());
            Iterator<SelectionKey> selectedKeys = selectionKeys.iterator();
            while (selectedKeys.hasNext()) {
                SelectionKey key = selectedKeys.next();
                LOG.trace("removing channel: " + key.channel());
                selectedKeys.remove();
                if (sessionKeys.get(key)!=null) {
                    LOG.trace("already in progress channel: " + key.channel());
                    continue;
                }
                if (!key.isValid()) {
                    LOG.trace("key channel invalid: " + key.channel());
                    continue;
                }
                if (key.isAcceptable()) {
                    LOG.trace("accepting channel: " + key.channel());
                    accept(key);
                } else if (key.isReadable()) {
                    LOG.trace("processing new socket channel: " + key.channel());
                    sessionKeys.put(key, new Object());
                    acceptorService.submit(ServerSocketProcessorFactory.create(key, sessionKeys));
                }
            }
        }
        LOG.debug("open connection: " + selector.selectedKeys().size());
        LOG.debug("Goodbye!");
    }


    public void stop() {
    	LOG.info("Will now close server...");
    	active = false;
        acceptorService.shutdown();
    	selector.wakeup();
    }

    private void accept(SelectionKey key) throws Exception {
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



    private boolean isActive() {
        return active;
    }
}
