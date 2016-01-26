package sslengine;

import org.apache.log4j.Logger;
import sslengine.client.ClientConnection;
import sslengine.client.ConnectionFactory;

import java.io.IOException;

public class SSLClientWrapper {

    protected final Logger LOG = Logger.getLogger(getClass());

    private ConnectionFactory connectionFactory;

    public static SSLClientWrapper wrap(ConnectionFactory connectionFactory) throws Exception {
        return new SSLClientWrapper(connectionFactory);
    }

    protected SSLClientWrapper(ConnectionFactory connectionFactory) throws Exception {
        this.connectionFactory = connectionFactory;
    }

    public byte[] call(byte[] request) throws Exception {
        ClientConnection client = null;
        try {
            client = connectionFactory.getConnection();
            client.connect();

            LOG.debug("calling to server");
            client.write(request);
            LOG.debug("reading from server");
            byte[] read = client.read();

            return read;
        } finally {
            client.shutdown();
        }
    }
}
