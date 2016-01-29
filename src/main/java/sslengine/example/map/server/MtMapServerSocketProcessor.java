package sslengine.example.map.server;

import sslengine.AbstractSocketLayer;
import sslengine.example.map.dto.MtTransferReq;
import sslengine.example.map.dto.MtTransferRes;
import sslengine.server.EventHandler;
import sslengine.server.ServerSocketProcessor;

import java.io.IOException;

public class MtMapServerSocketProcessor<SocketData>  extends ServerSocketProcessor {

    private static ServerMapRequestProcessor mapRequestProcessor = new ServerMapRequestProcessor();

    private MtMapServerHandler mtMapServerHandler;

    public MtMapServerSocketProcessor(AbstractSocketLayer<SocketData> socketLayer, EventHandler handler) {
        super(socketLayer, handler);
        this.mtMapServerHandler = new MtMapServerHandler();
    }

    @Override
    public byte[] processRequest(byte[] clientData) throws IOException {
        //echo
        //write(socketChannel, sslEngine, clientData);
        byte[] localClientData = clientData;
        if (clientData !=null && clientData.length>0) {
            MtTransferReq clientDto = mtMapServerHandler.decodeReq(clientData);
            LOG.debug("clientDto: " + clientDto);
            MtTransferRes serverResult = mapRequestProcessor.process(clientDto);
            LOG.debug("serverResult: " + serverResult);
            localClientData = mtMapServerHandler.encodeRes(serverResult);
        }

        return localClientData;
    }
}
