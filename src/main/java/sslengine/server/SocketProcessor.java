package sslengine.server;

import sslengine.SSLSocketLayer;
import sslengine.utils.SSLUtils;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;


public abstract class SocketProcessor extends SSLSocketLayer implements Runnable {

    private SSLEngine sslEngine;
    private EventHandler handler;
    private SocketChannel socketChannel;

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
    }

    @Override
    public void run() {
        try {
            byte[] clientData = read(socketChannel, sslEngine);
            LOG.debug("writing to buffer data size: " + clientData.length);
            byte[] resultData = processRequest(clientData);
            write(socketChannel, sslEngine, resultData);
            handler.onSuccessHandler();
        } catch (Exception e) {
            handler.onErrorHandler(e);
        }
    }

    public abstract byte[] processRequest(byte[] clientData) throws IOException;

    /**
     * Will be called by the selector when the specific socket channel has data to be read.
     * As soon as the server reads these data, it will call {@link SocketProcessor#write(SocketChannel, SSLEngine, byte[])}
     * to send back a trivial response.
     *
     * @param socketChannel - the transport link used between the two peers.
     * @param engine - the engine used for encryption/decryption of the data exchanged between the two peers.
     * @throws IOException if an I/O error occurs to the socket channel.
     */
    @Override
    protected byte[] read(SocketChannel socketChannel, SSLEngine engine) throws IOException {
        LOG.debug("Server is reading from a client...");

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
                        //LOG.debug("Incoming message: " + new String(peerAppData.array(), peerAppData.position(), peerAppData.limit()));
                        byte[] array = peerAppData.array();
                        int arrayOffset = peerAppData.arrayOffset();
                        data = Arrays.copyOfRange(array, arrayOffset + peerAppData.position(), arrayOffset + peerAppData.limit());
                        LOG.debug("Incoming message size: " + data.length);
                        return data;
                    case BUFFER_OVERFLOW:
                        peerAppData = SSLUtils.enlargeApplicationBuffer(engine, peerAppData);
                        break;
                    case BUFFER_UNDERFLOW:
                        peerNetData = SSLUtils.handleBufferUnderflow(engine, peerNetData);
                        break;
                    case CLOSED:
                        LOG.debug("Client wants to close connection...");
                        closeConnection(socketChannel, engine);
                        LOG.debug("Goodbye client!");
                        return data;
                    default:
                        throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
                }
            }

        } else if (bytesRead < 0) {
            LOG.error("Received end of stream. Will try to close connection with client...");
            handleEndOfStream(socketChannel, engine);
            LOG.debug("Goodbye client!");
        }

        return data;
    }

    @Override
    protected void write(SocketChannel socketChannel, SSLEngine engine, byte[] data) throws IOException {
        LOG.debug("Server writing to a client...");

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
                    LOG.debug("Message size sent to the client: " + data.length);
                    break;
                case BUFFER_OVERFLOW:
                    myNetData = SSLUtils.enlargePacketBuffer(engine, myNetData);
                    break;
                case BUFFER_UNDERFLOW:
                    throw new SSLException("Buffer underflow occurred after a wrap. I don't think we should ever get here.");
                case CLOSED:
                    closeConnection(socketChannel, engine);
                    return;
                default:
                    throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
            }
        }
    }
}
