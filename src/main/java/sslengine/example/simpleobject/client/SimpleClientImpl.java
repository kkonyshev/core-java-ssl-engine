package sslengine.example.simpleobject.client;

import sslengine.client.*;
import sslengine.example.simpleobject.dto.SimpleRequestDto;
import sslengine.example.simpleobject.dto.SimpleResponseDto;

public class SimpleClientImpl extends AbstractClient<SimpleRequestDto, SimpleResponseDto> {

    public SimpleClientImpl(ClientConnectionFactory connectionFactory, ClientHandler<SimpleRequestDto, SimpleResponseDto> clientHandler) throws Exception {
        super(connectionFactory, clientHandler);
    }

    @Override
    public SimpleResponseDto call(SimpleRequestDto requestDto) throws Exception {
        ClientConnection clientConnection = null;
        try {
            LOG.info("calling to server");
            clientConnection = connectionFactory.getConnection();
            clientConnection.connect();
            byte[] bytesToSend = clientHandler.encodeReq(requestDto);
            clientConnection.write(bytesToSend);
            LOG.debug("reading from server");
            byte[] receivedBytes = clientConnection.read();
            return clientHandler.decodeRes(receivedBytes);
        } finally {
            if (clientConnection !=null) {
                clientConnection.close();
            }
        }
    }
}
