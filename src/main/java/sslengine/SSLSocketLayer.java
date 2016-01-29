package sslengine;

import org.apache.log4j.Logger;
import sslengine.server.EventHandler;
import sslengine.utils.SSLUtils;

import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class SSLSocketLayer extends AbstractSocketLayer<SSLSocketChannelData> {

	protected final Logger LOG = Logger.getLogger(getClass());

    protected ByteBuffer myAppData;
    protected ByteBuffer myNetData;

    protected ByteBuffer peerAppData;
    protected ByteBuffer peerNetData;

    public SSLSocketLayer(SSLSocketChannelData socketChannelData) {
        this.socketChannelData = socketChannelData;
        SSLSession dummySession = this.socketChannelData.socketSSLEngine.getSession();

        myAppData = ByteBuffer.allocate(dummySession.getApplicationBufferSize());
        myNetData = ByteBuffer.allocate(dummySession.getPacketBufferSize());
        peerAppData = ByteBuffer.allocate(dummySession.getApplicationBufferSize());
        peerNetData = ByteBuffer.allocate(dummySession.getPacketBufferSize());
        dummySession.invalidate();
    }

    @Override
    public byte[] read(SSLSocketChannelData sslSocketChannelData) throws IOException, InterruptedException {
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
    public void write(SSLSocketChannelData sslSocketChannelData, byte[] data) throws IOException {
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

    protected void closeConnection(SSLSocketChannelData sslSocketChannelData) throws IOException  {
        sslSocketChannelData.socketSSLEngine.closeOutbound();
        HandshakeHandler.doHandshake(sslSocketChannelData.socketChannel, sslSocketChannelData.socketSSLEngine);
        LOG.info("Closing connection: " + sslSocketChannelData.socketChannel.getRemoteAddress());
        sslSocketChannelData.socketChannel.close();
    }

    protected void handleEndOfStream(SSLSocketChannelData sslSocketChannelData) throws IOException  {
        try {
            sslSocketChannelData.socketSSLEngine.closeInbound();
        } catch (Exception e) {
            LOG.error("This engine was forced to close inbound, without having received the proper SSL/TLS close notification message from the peer, due to end of stream.");
        }
        closeConnection(sslSocketChannelData);
    }
}
