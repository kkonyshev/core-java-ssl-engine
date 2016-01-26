package sslengine;

import sslengine.utils.SSLUtils;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class NioSslServerThreaded extends NioSslPeer {

    private SSLContext  context;
    private Selector    selector;
    private boolean     active;

    private ConcurrentHashMap<SelectionKey, Object> sessionKeys = new ConcurrentHashMap<>();
    private ExecutorService acceptorService = Executors.newCachedThreadPool();

    public NioSslServerThreaded(String hostAddress, int port, SSLContext context) throws Exception {
        this.context = context;
        SSLSession dummySession = context.createSSLEngine().getSession();
        myAppData = ByteBuffer.allocate(dummySession.getApplicationBufferSize());
        myNetData = ByteBuffer.allocate(dummySession.getPacketBufferSize());
        peerAppData = ByteBuffer.allocate(dummySession.getApplicationBufferSize());
        peerNetData = ByteBuffer.allocate(dummySession.getPacketBufferSize());
        dummySession.invalidate();

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
                                        void onSuccessHandler() {
                                            log.trace("removing lock from key: " + key);
                                            sessionKeys.remove(key);
                                        }

                                        @Override
                                        void onErrorHandler(Exception e) {
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


    @Override
    protected byte[] read(SocketChannel socketChannel, SSLEngine engine) throws IOException {

        log.debug("About to read from a client...");

        byte[] data = new byte[0];

        peerNetData.clear();
        int bytesRead = socketChannel.read(peerNetData);
        if (bytesRead > 0) {
            peerNetData.flip();
            while (peerNetData.hasRemaining()) {
                peerAppData.clear();
                SSLEngineResult result = engine.unwrap(peerNetData, peerAppData);
                switch (result.getStatus()) {
                case OK:
                    peerAppData.flip();
                    //log.debug("Incoming message: " + new String(peerAppData.array(), peerAppData.position(), peerAppData.limit()));
                    byte[] array = peerAppData.array();
                    int arrayOffset = peerAppData.arrayOffset();
                    data = Arrays.copyOfRange(array, arrayOffset + peerAppData.position(), arrayOffset + peerAppData.limit());
                    break;
                case BUFFER_OVERFLOW:
                    peerAppData = SSLUtils.enlargeApplicationBuffer(engine, peerAppData);
                    break;
                case BUFFER_UNDERFLOW:
                    peerNetData = SSLUtils.handleBufferUnderflow(engine, peerNetData);
                    break;
                case CLOSED:
                    log.debug("Client wants to close connection...");
                    closeConnection(socketChannel, engine);
                    log.debug("Goodbye client!");
                    return data;
                default:
                    throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
                }
            }

            write(socketChannel, engine, "Hello! I am your server!".getBytes());

        } else if (bytesRead < 0) {
            log.error("Received end of stream. Will try to close connection with client...");
            handleEndOfStream(socketChannel, engine);
            log.debug("Goodbye client!");
        }

        return data;
    }


    @Override
    protected void write(SocketChannel socketChannel, SSLEngine engine, byte[] data) throws IOException {

        log.debug("About to write to a client...");

        myAppData.clear();
        myAppData.put(data);
        myAppData.flip();
        while (myAppData.hasRemaining()) {
            // The loop has a meaning for (outgoing) messages larger than 16KB.
            // Every wrap call will remove 16KB from the original message and send it to the remote peer.
            myNetData.clear();
            SSLEngineResult result = engine.wrap(myAppData, myNetData);
            switch (result.getStatus()) {
            case OK:
                myNetData.flip();
                while (myNetData.hasRemaining()) {
                    socketChannel.write(myNetData);
                }
                log.debug("Message sent to the client: " + new String(data));
                closeConnection(socketChannel, engine);
                break;
            case BUFFER_OVERFLOW:
                myNetData = SSLUtils.enlargePacketBuffer(engine, myNetData);
                break;
            case BUFFER_UNDERFLOW:
                throw new SSLException("Buffer underflow occured after a wrap. I don't think we should ever get here.");
            case CLOSED:
                closeConnection(socketChannel, engine);
                return;
            default:
                throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
            }
        }
    }

    private boolean isActive() {
        return active;
    }
}
