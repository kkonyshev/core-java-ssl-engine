package sslengine;

import org.apache.log4j.NDC;

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
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * An SSL/TLS server, that will listen to a specific address and port and serve SSL/TLS connections
 * compatible with the protocol it applies.
 * <p/>
 * After initialization {@link NioSslServerThreaded#start()} should be called so the server starts to listen to
 * new connection requests. At this point, start is blocking, so, in order to be able to gracefully stop
 * the server, a {@link Runnable} containing a server object should be created. This runnable should 
 * start the server in its run method and also provide a stop method, which will call {@link NioSslServerThreaded#stop()}.
 * </p>
 * NioSslServer makes use of Java NIO, and specifically listens to new connection requests with a {@link ServerSocketChannel}, which will
 * create new {@link SocketChannel}s and a {@link Selector} which serves all the connections in one thread.
 *
 */
public class NioSslServerThreaded extends NioSslPeer {

	/**
	 * Declares if the server is active to serve and create new connections.
	 */
	private boolean active;

    /**
     * The context will be initialized with a specific SSL/TLS protocol and will then be used
     * to create {@link SSLEngine} classes for each new connection that arrives to the server.
     */
    private SSLContext context;

    /**
     * A part of Java NIO that will be used to serve all connections to the server in one thread.
     */
    private Selector selector;

    protected ExecutorService acceptorService = Executors.newCachedThreadPool();


    /**
     * Server is designed to apply an SSL/TLS protocol and listen to an IP address and port.
     *
     * @param context - the SSL/TLS context that this server will be configured to apply.
     * @param hostAddress - the IP address this server will listen to.
     * @param port - the port this server will listen to.
     * @throws Exception
     */
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

    private ConcurrentHashMap<SelectionKey, Object> locks = new ConcurrentHashMap<>();

    /**
     * Should be called in order the server to start listening to new connections.
     * This method will run in a loop as long as the server is active. In order to stop the server
     * you should use {@link NioSslServerThreaded#stop()} which will set it to inactive state
     * and also wake up the listener, which may be in blocking select() state.
     *
     * @throws Exception
     */
    public void start() throws Exception {
    	log.debug("Initialized and waiting for new connections...");
        while (isActive()) {
            selector.select();
            Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();
            while (selectedKeys.hasNext()) {
                SelectionKey key = selectedKeys.next();
                log.trace("removing key: " + key);
                selectedKeys.remove();

                if (locks.get(key)!=null) {
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
                    locks.put(key, new Object());
                    acceptorService.submit(
                            new SocketProcessor(
                                    (SocketChannel) key.channel(),
                                    (SSLEngine) key.attachment(),
                                    new EventHandler() {
                                        @Override
                                        void onSuccessHandler() {
                                            log.trace("removing lock from key: " + key);
                                            locks.remove(key);
                                        }

                                        @Override
                                        void onErrorHandler(Exception e) {
                                            log.error(e.getMessage(), e);
                                            locks.remove(key);
                                        }
                                    }));
                }
            }
        }
        log.debug("Goodbye!");
    }

    /**
     * Sets the server to an inactive state, in order to exit the reading loop in {@link NioSslServerThreaded#start()}
     * and also wakes up the selector, which may be in select() blocking state.
     */
    public void stop() {
    	log.debug("Will now close server...");
    	active = false;
    	executor.shutdown();
        acceptorService.shutdown();
    	selector.wakeup();
    }

    /**
     * Will be called after a new connection request arrives to the server. Creates the {@link SocketChannel} that will
     * be used as the network layer link, and the {@link SSLEngine} that will encrypt and decrypt all the data
     * that will be exchanged during the session with this specific client.
     *
     * @param key - the key dedicated to the {@link ServerSocketChannel} used by the server to listen to new connection requests.
     * @throws Exception
     */
    private void accept(SelectionKey key) throws Exception {

    	log.debug("New connection request!");

        SocketChannel socketChannel = ((ServerSocketChannel) key.channel()).accept();
        socketChannel.configureBlocking(false);

        SSLEngine engine = context.createSSLEngine();
        engine.setUseClientMode(false);
        engine.beginHandshake();

        if (doHandshake(socketChannel, engine)) {
            socketChannel.register(selector, SelectionKey.OP_READ, engine);
        } else {
            socketChannel.close();
            locks.remove(key);
            log.debug("Connection closed due to handshake failure.");
        }
    }

    /**
     * Will be called by the selector when the specific socket channel has data to be read.
     * As soon as the server reads these data, it will call {@link NioSslServerThreaded#write(SocketChannel, SSLEngine, byte[])}
     * to send back a trivial response.
     *
     * @param socketChannel - the transport link used between the two peers.
     * @param engine - the engine used for encryption/decryption of the data exchanged between the two peers.
     * @throws IOException if an I/O error occurs to the socket channel.
     */
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
                    peerAppData = enlargeApplicationBuffer(engine, peerAppData);
                    break;
                case BUFFER_UNDERFLOW:
                    peerNetData = handleBufferUnderflow(engine, peerNetData);
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

    /**
     * Will send a message back to a client.
     *
     * @param data - the message to be sent.
     * @throws IOException if an I/O error occurs to the socket channel.
     */
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
                myNetData = enlargePacketBuffer(engine, myNetData);
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

    /**
     * Determines if the the server is active or not.
     *
     * @return if the server is active or not.
     */
    private boolean isActive() {
        return active;
    }
}
