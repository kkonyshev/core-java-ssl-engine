package sslengine;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;

public abstract class SSLSocketLayer extends AbstractSocketLayer<SSLSocketChannelData> {

	protected final Logger LOG = Logger.getLogger(getClass());

    protected ByteBuffer myAppData;
    protected ByteBuffer myNetData;

    protected ByteBuffer peerAppData;
    protected ByteBuffer peerNetData;

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
