package sslengine.example.map.client;

import sslengine.client.*;
import sslengine.example.map.dto.MtTransferReq;
import sslengine.example.map.dto.MtTransferRes;
import sslengine.example.map.dto.TransferEvent;

import java.io.IOException;

public class MtMapClientImpl extends AbstractClient<MtTransferReq, MtTransferRes> {

    private ClientConnection clientConnection;

    public MtMapClientImpl(ClientConnectionFactory connectionFactory, ClientHandler<MtTransferReq, MtTransferRes> clientHandler) throws Exception {
        super(connectionFactory, clientHandler);
        LOG.info("calling to server");
        clientConnection = connectionFactory.getConnection();
        clientConnection.connect();
    }

    public void start(String processId) throws Exception {
        LOG.debug("init process");
        MtTransferRes result = call(new MtTransferReq(processId, TransferEvent.START, null));
        LOG.debug("Result: " + result);
    }

    @Override
    public MtTransferRes call(MtTransferReq requestDto) throws Exception {
        byte[] bytesToSend = clientHandler.encodeReq(requestDto);
        clientConnection.write(bytesToSend);
        LOG.debug("reading from server");
        byte[] receivedBytes = clientConnection.read();
        return clientHandler.decodeRes(receivedBytes);
    }

    public void stop(String processId) throws Exception {
        LOG.debug("init process");
        MtTransferRes result = call(new MtTransferReq(processId, TransferEvent.END, null));
        LOG.debug("Result: " + result);
    }

    public void shutdown() throws IOException {
        if (clientConnection != null) {
            clientConnection.close();
        }
    }

}
