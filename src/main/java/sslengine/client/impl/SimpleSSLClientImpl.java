package sslengine.client.impl;

import sslengine.client.ClientConnectionFactory;
import sslengine.client.ClientHandler;
import sslengine.dto.SimpleRequestDto;
import sslengine.dto.SimpleResponseDto;

import java.util.Random;

public class SimpleSSLClientImpl extends SSLClient<SimpleRequestDto, SimpleResponseDto> {

    public SimpleSSLClientImpl(ClientConnectionFactory connectionFactory, ClientHandler<SimpleRequestDto, SimpleResponseDto> clientHandler) throws Exception {
        super(connectionFactory, clientHandler);
    }

    @Override
    public SimpleResponseDto call(SimpleRequestDto requestDto) throws Exception {
        ClientConnection clientConnection = null;
        try {
            LOG.debug("calling to server");
            clientConnection = connectionFactory.getConnection();
            clientConnection.connect();
            byte[] bytesToSend = clientHandler.encodeReq(requestDto);
            clientConnection.write(bytesToSend);
            Thread.sleep(new Random().nextInt(1000));
            LOG.debug("reading from server");
            byte[] receivedBytes = clientConnection.read();
            return clientHandler.decodeRes(receivedBytes);
        } finally {
            if (clientConnection!=null) {
                clientConnection.shutdown();
            }
        }
    }
}
