package sslengine;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;

/**
 * An SSL/TLS client that connects to a server using its IP address and port.
 * <p/>
 * After initialization of a {@link NioSslClientThreadLocal} object, {@link NioSslClientThreadLocal#connect()} should be called,
 * in order to establish connection with the server.
 * <p/>
 * When the connection between the client and the object is established, {@link NioSslClientThreadLocal} provides
 * a public write and read method, in order to communicate with its peer. 
 *
 * @author <a href="mailto:travelling.with.code@gmail.com">Alex</a>
 */
public class NioSslClientThreadLocal extends NioSslPeer {

    /**
     * The remote address of the server this client is configured to connect to.
     */
	private String remoteAddress;

	/**
	 * The port of the server this client is configured to connect to.
	 */
	private int port;

	/**
	 * The engine that will be used to encrypt/decrypt data between this client and the server.
	 */
    private SSLEngine engine;

    /**
     * The socket channel that will be used as the transport link between this client and the server.
     */
    private SocketChannel socketChannel;


    /**
     * Initiates the engine to run as a client using peer information, and allocates space for the
     * buffers that will be used by the engine.
     *
     * @param context The SSL/TLS context to be used.
     * @param remoteAddress The IP address of the peer.
     * @param port The peer's port that will be used.
     * @throws Exception
     */
    public NioSslClientThreadLocal(String remoteAddress, int port, SSLContext context) throws Exception  {
    	this.remoteAddress = remoteAddress;
    	this.port = port;

        engine = context.createSSLEngine(remoteAddress, port);
        engine.setUseClientMode(true);

        SSLSession session = engine.getSession();
        myAppData = ByteBuffer.allocate(1024);
        myNetData = ByteBuffer.allocate(session.getPacketBufferSize());
        peerAppData = ByteBuffer.allocate(1024);
        peerNetData = ByteBuffer.allocate(session.getPacketBufferSize());
    }

    /**
     * Opens a socket channel to communicate with the configured server and tries to complete the handshake protocol.
     *
     * @return True if client established a connection with the server, false otherwise.
     * @throws Exception
     */
    public boolean connect() throws Exception {
    	socketChannel = SocketChannel.open();
    	socketChannel.configureBlocking(false);
    	socketChannel.connect(new InetSocketAddress(remoteAddress, port));
    	while (!socketChannel.finishConnect()) {
    		// can do something here...
    	}

    	engine.beginHandshake();
    	return doHandshake(socketChannel, engine);
    }

    /**
     * Public method to send a message to the server.
     *
     * @param data - data to be sent to the server.
     * @throws IOException if an I/O error occurs to the socket channel.
     */
    public void write(byte[] data) throws IOException {
        write(socketChannel, engine, data);
    }

    /**
     * Implements the write method that sends a message to the server the client is connected to,
     * but should not be called by the user, since socket channel and engine are inner class' variables.
     * {@link NioSslClientThreadLocal#write(byte[])} should be called instead.
     *
     * @param data - data to be sent to the server.
     * @param engine - the engine used for encryption/decryption of the data exchanged between the two peers.
     * @throws IOException if an I/O error occurs to the socket channel.
     */
    @Override
    protected void write(SocketChannel socketChannel, SSLEngine engine, byte[] data) throws IOException {

        log.debug("About to write to the server...");

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
                log.debug("Message sent to the server: " + new String(data));
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
     * Public method to try to read from the server.
     *
     * @throws Exception
     */
    public byte[] read() throws Exception {
        return read(socketChannel, engine);
    }

    /**
     * Will wait for response from the remote peer, until it actually gets something.
     * Uses {@link SocketChannel#read(ByteBuffer)}, which is non-blocking, and if
     * it gets nothing from the peer, waits for {@code waitToReadMillis} and tries again.
     * <p/>
     * Just like {@link NioSslClientThreadLocal#read(SocketChannel, SSLEngine)} it uses inner class' socket channel
     * and engine and should not be used by the client. {@link NioSslClientThreadLocal#read()} should be called instead.
     * 
     * @param engine - the engine used for encryption/decryption of the data exchanged between the two peers.
     * @throws Exception
     */
    @Override
    protected byte[] read(SocketChannel socketChannel, SSLEngine engine) throws Exception  {

        log.debug("About to read from the server...");

        byte[] data = new byte[0];

        peerNetData.clear();
        int waitToReadMillis = 50;
        boolean exitReadLoop = false;
        while (!exitReadLoop) {
            int bytesRead = socketChannel.read(peerNetData);
            if (bytesRead > 0) {
                peerNetData.flip();
                while (peerNetData.hasRemaining()) {
                    peerAppData.clear();
                    SSLEngineResult result = engine.unwrap(peerNetData, peerAppData);
                    switch (result.getStatus()) {
                    case OK:
                        peerAppData.flip();
                        //log.debug("Server response: " + new String(peerAppData.array(), peerAppData.position(), peerAppData.limit()));
                        byte[] array = peerAppData.array();
                        int arrayOffset = peerAppData.arrayOffset();
                        data = Arrays.copyOfRange(array, arrayOffset + peerAppData.position(), arrayOffset + peerAppData.limit());
                        exitReadLoop = true;
                        log.debug("Server response: " + new String(data));
                        break;
                    case BUFFER_OVERFLOW:
                        peerAppData = enlargeApplicationBuffer(engine, peerAppData);
                        break;
                    case BUFFER_UNDERFLOW:
                        peerNetData = handleBufferUnderflow(engine, peerNetData);
                        break;
                    case CLOSED:
                        closeConnection(socketChannel, engine);
                        return data;
                    default:
                        shutdown();
                        throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
                    }
                }
            } else if (bytesRead < 0) {
                handleEndOfStream(socketChannel, engine);
                return data;
            }
            Thread.sleep(waitToReadMillis);
        }

        return data;
    }

    /**
     * Should be called when the client wants to explicitly close the connection to the server.
     *
     * @throws IOException if an I/O error occurs to the socket channel.
     */
    public void shutdown() throws IOException {
        log.debug("About to close connection with the server...");
        closeConnection(socketChannel, engine);
        executor.shutdown();
        log.debug("Goodbye!");
    }

}
