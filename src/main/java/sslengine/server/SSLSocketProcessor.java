package sslengine.server;

import sslengine.SSLSocketChannelData;
import sslengine.SSLSocketLayer;
import sslengine.utils.SSLUtils;

import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;


public abstract class SSLSocketProcessor extends SSLSocketLayer implements Runnable {

    private EventHandler handler;
    private SSLSocketChannelData sslSocketChannelData;

    public SSLSocketProcessor(SSLSocketChannelData sslSocketChannelData, EventHandler handler) throws FileNotFoundException {
        this.sslSocketChannelData = sslSocketChannelData;
        this.handler = handler;

        SSLSession dummySession = this.sslSocketChannelData.socketSSLEngine.getSession();
        myAppData = ByteBuffer.allocate(dummySession.getApplicationBufferSize());
        myNetData = ByteBuffer.allocate(dummySession.getPacketBufferSize());
        peerAppData = ByteBuffer.allocate(dummySession.getApplicationBufferSize());
        peerNetData = ByteBuffer.allocate(dummySession.getPacketBufferSize());
        dummySession.invalidate();
    }

    @Override
    public void run() {
        try {
            byte[] clientData = read(sslSocketChannelData);
            LOG.debug("writing to buffer data size: " + clientData.length);
            byte[] resultData = processRequest(clientData);
            write(sslSocketChannelData, resultData);
            handler.onSuccessHandler();
        } catch (Exception e) {
            handler.onErrorHandler(e);
        }
    }

    public abstract byte[] processRequest(byte[] clientData) throws IOException;

    @Override
    protected byte[] read(SSLSocketChannelData sslSocketChannelData) throws IOException {
        LOG.debug("Server is reading from a client...");

        byte[] data = new byte[0];

        peerNetData.clear();
        int bytesRead = sslSocketChannelData.socketChannel.read(peerNetData);
        if (bytesRead > 0) {
            peerNetData.flip();
            while (peerNetData.hasRemaining()) {
                peerAppData.clear();
                SSLEngineResult result = sslSocketChannelData.socketSSLEngine.unwrap(peerNetData, peerAppData);
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
                        peerAppData = SSLUtils.enlargeApplicationBuffer(sslSocketChannelData.socketSSLEngine, peerAppData);
                        break;
                    case BUFFER_UNDERFLOW:
                        peerNetData = SSLUtils.handleBufferUnderflow(sslSocketChannelData.socketSSLEngine, peerNetData);
                        break;
                    case CLOSED:
                        LOG.debug("Client wants to close connection...");
                        closeConnection(sslSocketChannelData);
                        LOG.debug("Goodbye client!");
                        return data;
                    default:
                        throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
                }
            }

        } else if (bytesRead < 0) {
            LOG.error("Received end of stream. Will try to close connection with client...");
            handleEndOfStream(sslSocketChannelData);
            LOG.debug("Goodbye client!");
        }

        return data;
    }

    @Override
    protected void write(SSLSocketChannelData sslSocketChannelData, byte[] data) throws IOException {
        LOG.debug("Server writing to a client...");

        myAppData.clear();
        myAppData.put(data);
        myAppData.flip();
        while (myAppData.hasRemaining()) {
            // The loop has a meaning for (outgoing) messages larger than 16KB.
            // Every wrap call will remove 16KB from the original message and send it to the remote peer.
            myNetData.clear();
            SSLEngineResult result = sslSocketChannelData.socketSSLEngine.wrap(myAppData, myNetData);
            switch (result.getStatus()) {
                case OK:
                    myNetData.flip();
                    while (myNetData.hasRemaining()) {
                        sslSocketChannelData.socketChannel.write(myNetData);
                    }
                    LOG.debug("Message size sent to the client: " + data.length);
                    break;
                case BUFFER_OVERFLOW:
                    myNetData = SSLUtils.enlargePacketBuffer(sslSocketChannelData.socketSSLEngine, myNetData);
                    break;
                case BUFFER_UNDERFLOW:
                    throw new SSLException("Buffer underflow occurred after a wrap. I don't think we should ever get here.");
                case CLOSED:
                    closeConnection(sslSocketChannelData);
                    return;
                default:
                    throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
            }
        }
    }
}
