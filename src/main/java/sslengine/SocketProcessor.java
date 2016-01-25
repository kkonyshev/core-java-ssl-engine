package sslengine;

import org.apache.log4j.Logger;

import javax.net.ssl.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;


public class SocketProcessor extends NioSslPeer implements Runnable {

    protected final Logger log = Logger.getLogger(getClass());

    private SocketChannel socketChannel;
    private SSLEngine sslEngine;
    private EventHandler handler;

    private BufferedOutputStream outStream;

    private byte[] clientData;

    public SocketProcessor(SocketChannel socketChannel, SSLEngine sslEngine, EventHandler handler) throws FileNotFoundException {
        this.socketChannel = socketChannel;
        this.sslEngine = sslEngine;
        this.handler = handler;

        SSLSession dummySession = sslEngine.getSession();
        myAppData = ByteBuffer.allocate(dummySession.getApplicationBufferSize());
        myNetData = ByteBuffer.allocate(dummySession.getPacketBufferSize());
        peerAppData = ByteBuffer.allocate(dummySession.getApplicationBufferSize());
        peerNetData = ByteBuffer.allocate(dummySession.getPacketBufferSize());
        dummySession.invalidate();

        outStream = new BufferedOutputStream(new FileOutputStream("src/main/resources/huge1.out"));
    }

    @Override
    public void run() {
        try {
            clientData = read(socketChannel, sslEngine);
            log.debug("writing to buffer data size: " + clientData.length);
            outStream.write(clientData);
            onSuccessHandle();
        } catch (Exception e) {
            handler.onErrorHandler(e);
        }
    }

    protected void onSuccessHandle() throws IOException {
        //echo
        write(socketChannel, sslEngine, String.valueOf(clientData.length).getBytes());
        handler.onSuccessHandler();
    }

    /**
     * Implements the handshake protocol between two peers, required for the establishment of the SSL/TLS connection.
     * During the handshake, encryption configuration information - such as the list of available cipher suites - will be exchanged
     * and if the handshake is successful will lead to an established SSL/TLS session.
     *
     * <p/>
     * A typical handshake will usually contain the following steps:
     *
     * <ul>
     *   <li>1. wrap:     ClientHello</li>
     *   <li>2. unwrap:   ServerHello/Cert/ServerHelloDone</li>
     *   <li>3. wrap:     ClientKeyExchange</li>
     *   <li>4. wrap:     ChangeCipherSpec</li>
     *   <li>5. wrap:     Finished</li>
     *   <li>6. unwrap:   ChangeCipherSpec</li>
     *   <li>7. unwrap:   Finished</li>
     * </ul>
     * <p/>
     * Handshake is also used during the end of the session, in order to properly close the connection between the two peers.
     * A proper connection close will typically include the one peer sending a CLOSE message to another, and then wait for
     * the other's CLOSE message to close the transport link. The other peer from his perspective would read a CLOSE message
     * from his peer and then enter the handshake procedure to send his own CLOSE message as well.
     *
     * @param socketChannel - the socket channel that connects the two peers.
     * @param engine - the engine that will be used for encryption/decryption of the data exchanged with the other peer.
     * @return True if the connection handshake was successful or false if an error occurred.
     * @throws IOException - if an error occurs during read/write to the socket channel.
     */
    protected boolean doHandshake(SocketChannel socketChannel, SSLEngine engine) throws IOException {

        log.debug("About to do handshake...");

        SSLEngineResult result;
        SSLEngineResult.HandshakeStatus handshakeStatus;

        // NioSslPeer's fields myAppData and peerAppData are supposed to be large enough to hold all message data the peer
        // will send and expects to receive from the other peer respectively. Since the messages to be exchanged will usually be less
        // than 16KB long the capacity of these fields should also be smaller. Here we initialize these two local buffers
        // to be used for the handshake, while keeping client's buffers at the same size.
        int appBufferSize = engine.getSession().getApplicationBufferSize();
        ByteBuffer myAppData = ByteBuffer.allocate(appBufferSize);
        ByteBuffer peerAppData = ByteBuffer.allocate(appBufferSize);
        myNetData.clear();
        peerNetData.clear();

        handshakeStatus = engine.getHandshakeStatus();
        while (handshakeStatus != SSLEngineResult.HandshakeStatus.FINISHED && handshakeStatus != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
            switch (handshakeStatus) {
                case NEED_UNWRAP:
                    if (socketChannel.read(peerNetData) < 0) {
                        if (engine.isInboundDone() && engine.isOutboundDone()) {
                            return false;
                        }
                        try {
                            engine.closeInbound();
                        } catch (SSLException e) {
                            log.error("This engine was forced to close inbound, without having received the proper SSL/TLS close notification message from the peer, due to end of stream.");
                        }
                        engine.closeOutbound();
                        // After closeOutbound the engine will be set to WRAP state, in order to try to send a close message to the client.
                        handshakeStatus = engine.getHandshakeStatus();
                        break;
                    }
                    peerNetData.flip();
                    try {
                        result = engine.unwrap(peerNetData, peerAppData);
                        peerNetData.compact();
                        handshakeStatus = result.getHandshakeStatus();
                    } catch (SSLException sslException) {
                        log.error("A problem was encountered while processing the data that caused the SSLEngine to abort. Will try to properly close connection...");
                        engine.closeOutbound();
                        handshakeStatus = engine.getHandshakeStatus();
                        break;
                    }
                    switch (result.getStatus()) {
                        case OK:
                            break;
                        case BUFFER_OVERFLOW:
                            // Will occur when peerAppData's capacity is smaller than the data derived from peerNetData's unwrap.
                            peerAppData = enlargeApplicationBuffer(engine, peerAppData);
                            break;
                        case BUFFER_UNDERFLOW:
                            // Will occur either when no data was read from the peer or when the peerNetData buffer was too small to hold all peer's data.
                            peerNetData = handleBufferUnderflow(engine, peerNetData);
                            break;
                        case CLOSED:
                            if (engine.isOutboundDone()) {
                                return false;
                            } else {
                                engine.closeOutbound();
                                handshakeStatus = engine.getHandshakeStatus();
                                break;
                            }
                        default:
                            throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
                    }
                    break;
                case NEED_WRAP:
                    myNetData.clear();
                    try {
                        result = engine.wrap(myAppData, myNetData);
                        handshakeStatus = result.getHandshakeStatus();
                    } catch (SSLException sslException) {
                        log.error("A problem was encountered while processing the data that caused the SSLEngine to abort. Will try to properly close connection...");
                        engine.closeOutbound();
                        handshakeStatus = engine.getHandshakeStatus();
                        break;
                    }
                    switch (result.getStatus()) {
                        case OK :
                            myNetData.flip();
                            while (myNetData.hasRemaining()) {
                                socketChannel.write(myNetData);
                            }
                            break;
                        case BUFFER_OVERFLOW:
                            // Will occur if there is not enough space in myNetData buffer to write all the data that would be generated by the method wrap.
                            // Since myNetData is set to session's packet size we should not get to this point because SSLEngine is supposed
                            // to produce messages smaller or equal to that, but a general handling would be the following:
                            myNetData = enlargePacketBuffer(engine, myNetData);
                            break;
                        case BUFFER_UNDERFLOW:
                            throw new SSLException("Buffer underflow occured after a wrap. I don't think we should ever get here.");
                        case CLOSED:
                            try {
                                myNetData.flip();
                                while (myNetData.hasRemaining()) {
                                    socketChannel.write(myNetData);
                                }
                                // At this point the handshake status will probably be NEED_UNWRAP so we make sure that peerNetData is clear to read.
                                peerNetData.clear();
                            } catch (Exception e) {
                                log.error("Failed to send server's CLOSE message due to socket channel's failure.");
                                handshakeStatus = engine.getHandshakeStatus();
                            }
                            break;
                        default:
                            throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
                    }
                    break;
                case NEED_TASK:
                    Runnable task;
                    while ((task = engine.getDelegatedTask()) != null) {
                        executor.execute(task);
                    }
                    handshakeStatus = engine.getHandshakeStatus();
                    break;
                case FINISHED:
                    break;
                case NOT_HANDSHAKING:
                    break;
                default:
                    throw new IllegalStateException("Invalid SSL status: " + handshakeStatus);
            }
        }

        return true;
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
                        log.debug("Incoming message size: " + data.length);
                        return data;
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
                        try {
                            log.debug("closing buffer");
                            outStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return data;
                    default:
                        throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
                }
            }

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
}
