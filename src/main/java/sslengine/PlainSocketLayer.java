package sslengine;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;

public abstract class PlainSocketLayer extends AbstractSocketLayer<SocketChannelData> {

	protected final Logger LOG = Logger.getLogger(getClass());

    protected ByteBuffer myAppData;
    protected ByteBuffer myNetData;

    protected ByteBuffer peerAppData;
    protected ByteBuffer peerNetData;

    protected abstract byte[] read(SocketChannelData socketChannelData) throws Exception;
    protected abstract void write(SocketChannelData socketChannelData, byte[] data) throws Exception;

    protected void closeConnection(SocketChannelData socketChannelData) throws IOException  {
        LOG.info("Closing connection: " + socketChannelData.socketChannel.getRemoteAddress());
        socketChannelData.socketChannel.close();
    }

    protected void handleEndOfStream(SocketChannelData socketChannelData) throws IOException  {
        closeConnection(socketChannelData);
    }
}
