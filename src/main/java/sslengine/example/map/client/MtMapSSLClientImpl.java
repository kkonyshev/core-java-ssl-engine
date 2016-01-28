package sslengine.example.map.client;

import sslengine.client.AbstractSSLClient;
import sslengine.client.ClientConnection;
import sslengine.client.ClientConnectionFactory;
import sslengine.client.ClientHandler;
import sslengine.example.map.dto.MtTransferReq;
import sslengine.example.map.dto.MtTransferRes;
import sslengine.example.map.dto.TransferEvent;

import java.io.IOException;

public class MtMapSSLClientImpl extends AbstractSSLClient<MtTransferReq, MtTransferRes> {

    private ClientConnection sslClientConnection;

    public MtMapSSLClientImpl(ClientConnectionFactory connectionFactory, ClientHandler<MtTransferReq, MtTransferRes> clientHandler) throws Exception {
        super(connectionFactory, clientHandler);
        LOG.info("calling to server");
        sslClientConnection = connectionFactory.getConnection();
        sslClientConnection.connect();
    }

    public void start(String processId) throws Exception {
        LOG.debug("init process");
        MtTransferRes result = call(new MtTransferReq(processId, TransferEvent.START, null));
        LOG.debug("Result: " + result);
    }

    @Override
    public MtTransferRes call(MtTransferReq requestDto) throws Exception {
        byte[] bytesToSend = clientHandler.encodeReq(requestDto);
        sslClientConnection.write(bytesToSend);
        LOG.debug("reading from server");
        byte[] receivedBytes = sslClientConnection.read();
        return clientHandler.decodeRes(receivedBytes);
    }

    public void stop(String processId) throws Exception {
        LOG.debug("init process");
        MtTransferRes result = call(new MtTransferReq(processId, TransferEvent.END, null));
        LOG.debug("Result: " + result);
    }

    public void shutdown() throws IOException {
        if (sslClientConnection != null) {
            sslClientConnection.shutdown();
        }
    }

}
