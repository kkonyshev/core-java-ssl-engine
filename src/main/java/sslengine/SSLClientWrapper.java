package sslengine;

import org.apache.log4j.Logger;

import java.io.IOException;

public class SSLClientWrapper {

    protected final Logger LOG = Logger.getLogger(getClass());

    private NioSslClientThreadLocal client;

    public static SSLClientWrapper wrap(NioSslClientThreadLocal client) throws Exception {
        return new SSLClientWrapper(client);
    }

    protected SSLClientWrapper(NioSslClientThreadLocal client) throws Exception {
        this.client = client;
        client.connect();
    }

    public byte[] call(byte[] request) throws Exception {
        LOG.debug("calling to server");
        client.write(request);
        LOG.debug("reading from server");
        byte[] response = client.read();
        return response;
    }

    public void finalize() {
        try {
            client.shutdown();
        } catch (IOException e) {
            LOG.warn("client shutdown interrupted!", e);
        }
        LOG.debug("client shutting shutdown complete");
    }
}
