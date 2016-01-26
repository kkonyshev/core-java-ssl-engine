package sslengine;

import org.apache.log4j.Logger;
import sslengine.client.ClientConnector;

import java.io.IOException;

public class SSLClientWrapper {

    protected final Logger LOG = Logger.getLogger(getClass());

    private ClientConnector client;

    public static SSLClientWrapper wrap(ClientConnector client) throws Exception {
        return new SSLClientWrapper(client);
    }

    protected SSLClientWrapper(ClientConnector client) throws Exception {
        this.client = client;
        client.connect();
    }

    public byte[] call(byte[] request) throws Exception {
        LOG.debug("calling to server");
        client.write(request);
        LOG.debug("reading from server");
        return client.read();
    }

    public void finalize() {
        try {
            super.finalize();
            client.shutdown();
        } catch (IOException e) {
            LOG.warn("client shutdown interrupted!", e);
        } catch (Throwable throwable) {
            LOG.error(throwable.getMessage(), throwable);
        }
        LOG.debug("client shutting shutdown complete");
    }
}
