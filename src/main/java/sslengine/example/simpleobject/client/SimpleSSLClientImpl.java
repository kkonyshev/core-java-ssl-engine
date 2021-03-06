package sslengine.example.simpleobject.client;

import sslengine.client.ClientConnectionFactory;
import sslengine.client.ClientHandler;
import sslengine.client.AbstractSSLClient;
import sslengine.client.ClientConnection;
import sslengine.example.simpleobject.dto.SimpleRequestDto;
import sslengine.example.simpleobject.dto.SimpleResponseDto;

public class SimpleSSLClientImpl extends AbstractSSLClient<SimpleRequestDto, SimpleResponseDto> {

    public SimpleSSLClientImpl(ClientConnectionFactory connectionFactory, ClientHandler<SimpleRequestDto, SimpleResponseDto> clientHandler) throws Exception {
        super(connectionFactory, clientHandler);
    }

    @Override
    public SimpleResponseDto call(SimpleRequestDto requestDto) throws Exception {
        ClientConnection SSLClientConnection = null;
        try {
            LOG.info("calling to server");
            SSLClientConnection = connectionFactory.getConnection();
            SSLClientConnection.connect();
            byte[] bytesToSend = clientHandler.encodeReq(requestDto);
            SSLClientConnection.write(bytesToSend);
            LOG.debug("reading from server");
            byte[] receivedBytes = SSLClientConnection.read();
            return clientHandler.decodeRes(receivedBytes);
        } finally {
            if (SSLClientConnection !=null) {
                SSLClientConnection.shutdown();
            }
        }
    }
}
