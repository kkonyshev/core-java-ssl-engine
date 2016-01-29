package sslengine;

import org.apache.log4j.Logger;

import java.io.IOException;

public abstract class AbstractSocketLayer<SocketData> {

	protected final Logger LOG = Logger.getLogger(getClass());

    protected SocketData socketChannelData;

    public abstract byte[] read(SocketData socketData) throws Exception;
    public abstract void write(SocketData socketData, byte[] data) throws Exception;

    protected abstract void closeConnection(SocketData socketData) throws IOException;
    protected abstract void handleEndOfStream(SocketData socketData) throws IOException;

    public SocketData getSocketChannelData() {
        return this.socketChannelData;
    }
}
