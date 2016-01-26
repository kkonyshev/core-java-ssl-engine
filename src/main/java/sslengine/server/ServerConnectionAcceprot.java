package sslengine.server;

import org.apache.log4j.Logger;
import sslengine.handler.EventHandler;
import sslengine.handler.HandshakeHandler;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ServerConnectionAcceprot {

    private final Logger log = Logger.getLogger(getClass());

    private SSLContext  context;
    private Selector    selector;
    private boolean     active;

    private ConcurrentHashMap<SelectionKey, Object> sessionKeys = new ConcurrentHashMap<>();
    private ExecutorService acceptorService = Executors.newCachedThreadPool();

    public ServerConnectionAcceprot(String hostAddress, int port, SSLContext context) throws Exception {
        this.context = context;

        selector = SelectorProvider.provider().openSelector();
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.socket().bind(new InetSocketAddress(hostAddress, port));
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        active = true;
    }

    public void start() throws Exception {
    	log.debug("Initialized and waiting for new connections...");
        while (isActive()) {
            selector.select();
            Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();
            while (selectedKeys.hasNext()) {
                SelectionKey key = selectedKeys.next();
                log.trace("removing key: " + key);
                selectedKeys.remove();

                if (sessionKeys.get(key)!=null) {
                    log.trace("in process: " + key);
                    continue;
                }

                if (!key.isValid()) {
                    log.trace("key invalid");
                    continue;
                }
                if (key.isAcceptable()) {
                    log.trace("accepting");
                    accept(key);
                } else if (key.isReadable()) {
                    log.trace("processing new socket channel: " + key);
                    sessionKeys.put(key, new Object());
                    acceptorService.submit(
                            new SocketProcessor(
                                    (SocketChannel) key.channel(),
                                    (SSLEngine) key.attachment(),
                                    new EventHandler() {
                                        @Override
                                        public void onSuccessHandler() {
                                            log.trace("removing lock from key: " + key);
                                            sessionKeys.remove(key);
                                        }

                                        @Override
                                        public void onErrorHandler(Exception e) {
                                            log.error(e.getMessage(), e);
                                            sessionKeys.remove(key);
                                        }
                                    }));
                }
            }
        }
        log.debug("Goodbye!");
    }


    public void stop() {
    	log.debug("Will now close server...");
    	active = false;
        acceptorService.shutdown();
    	selector.wakeup();
    }

    private void accept(SelectionKey key) throws Exception {

    	log.debug("New connection request!");

        SocketChannel socketChannel = ((ServerSocketChannel) key.channel()).accept();
        socketChannel.configureBlocking(false);

        SSLEngine engine = context.createSSLEngine();
        engine.setUseClientMode(false);
        engine.beginHandshake();

        if (new HandshakeHandler().doHandshake(socketChannel, engine)) {
            socketChannel.register(selector, SelectionKey.OP_READ, engine);
        } else {
            socketChannel.close();
            sessionKeys.remove(key);
            log.debug("Connection closed due to handshake failure.");
        }
    }



    private boolean isActive() {
        return active;
    }
}
