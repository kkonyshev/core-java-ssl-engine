package sslengine.server;

import org.apache.log4j.Logger;

import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


abstract public class ServerConnectionAcceptor {

    protected final Logger LOG = Logger.getLogger(getClass());

    protected Selector selector;
    protected boolean active;

    protected ConcurrentHashMap<SelectionKey, Object> sessionKeys = new ConcurrentHashMap<>();
    protected ExecutorService acceptorService = Executors.newCachedThreadPool();

    protected SocketProcessorFactory socketProcessorFactory;

    public ServerConnectionAcceptor(String hostAddress, int port, SocketProcessorFactory socketProcessorFactory) throws Exception {

        selector = SelectorProvider.provider().openSelector();
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.socket().bind(new InetSocketAddress(hostAddress, port));
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        this.socketProcessorFactory = socketProcessorFactory;

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
                    acceptorService.submit(socketProcessorFactory.create(key, sessionKeys));
                }
            }
        }
        LOG.info("open connection: " + selector.selectedKeys().size());
        LOG.debug("Goodbye!");
    }


    public void stop() {
    	LOG.info("Will now close server...");
    	active = false;
        acceptorService.shutdown();
    	selector.wakeup();
        LOG.info("open connection: " + selector.selectedKeys().size());
    }

    abstract protected void accept(SelectionKey key) throws Exception;


    private boolean isActive() {
        return active;
    }
}
