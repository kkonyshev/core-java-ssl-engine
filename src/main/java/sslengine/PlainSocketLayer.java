package sslengine;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;

public abstract class PlainSocketLayer extends AbstractSocketLayer<PlainSocketChannelData> {

	protected final Logger LOG = Logger.getLogger(getClass());

    protected ByteBuffer myAppData;
    protected ByteBuffer myNetData;

    protected ByteBuffer peerAppData;
    protected ByteBuffer peerNetData;

    protected abstract byte[] read(PlainSocketChannelData plainSocketChannelData) throws Exception;
    protected abstract void write(PlainSocketChannelData plainSocketChannelData, byte[] data) throws Exception;

    protected void closeConnection(PlainSocketChannelData plainSocketChannelData) throws IOException  {
        LOG.info("Closing connection: " + plainSocketChannelData.socketChannel.getRemoteAddress());
        plainSocketChannelData.socketChannel.close();
    }

    protected void handleEndOfStream(PlainSocketChannelData plainSocketChannelData) throws IOException  {
        closeConnection(plainSocketChannelData);
    }
}
