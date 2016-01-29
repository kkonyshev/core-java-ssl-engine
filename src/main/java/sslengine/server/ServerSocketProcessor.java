package sslengine.server;


import org.apache.log4j.Logger;
import sslengine.AbstractSocketLayer;

import java.io.IOException;

abstract public class ServerSocketProcessor<SocketDate> implements Runnable {

    protected final Logger LOG = Logger.getLogger(getClass());

    protected AbstractSocketLayer<SocketDate> processorSocketLayer;
    protected EventHandler handler;

    public ServerSocketProcessor(AbstractSocketLayer<SocketDate> processorSocketLayer, EventHandler handler) {
        this.processorSocketLayer = processorSocketLayer;
        this.handler = handler;
    }

    @Override
    public void run() {
        try {
            byte[] clientData = processorSocketLayer.read(processorSocketLayer.getSocketChannelData());
            LOG.debug("writing to buffer data size: " + clientData.length);
            byte[] resultData = processRequest(clientData);
            processorSocketLayer.write(processorSocketLayer.getSocketChannelData(), resultData);
            handler.onSuccessHandler();
        } catch (Exception e) {
            handler.onErrorHandler(e);
        }
    }

    public abstract byte[] processRequest(byte[] clientData) throws IOException;
}
