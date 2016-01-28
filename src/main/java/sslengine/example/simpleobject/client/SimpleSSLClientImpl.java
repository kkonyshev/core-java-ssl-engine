package sslengine.example.simpleobject.client;

import sslengine.client.*;
import sslengine.example.simpleobject.dto.SimpleRequestDto;
import sslengine.example.simpleobject.dto.SimpleResponseDto;

public class SimpleSSLClientImpl extends AbstractSSLClient<SimpleRequestDto, SimpleResponseDto> {

    public SimpleSSLClientImpl(ClientConnectionFactory connectionFactory, ClientHandler<SimpleRequestDto, SimpleResponseDto> clientHandler) throws Exception {
        super(connectionFactory, clientHandler);
    }

    @Override
    public SimpleResponseDto call(SimpleRequestDto requestDto) throws Exception {
        ClientConnection sslSSLClientConnection = null;
        try {
            LOG.info("calling to server");
            sslSSLClientConnection = connectionFactory.getConnection();
            sslSSLClientConnection.connect();
            byte[] bytesToSend = clientHandler.encodeReq(requestDto);
            sslSSLClientConnection.write(bytesToSend);
            LOG.debug("reading from server");
            byte[] receivedBytes = sslSSLClientConnection.read();
            return clientHandler.decodeRes(receivedBytes);
        } finally {
            if (sslSSLClientConnection !=null) {
                sslSSLClientConnection.close();
            }
        }
    }
}
