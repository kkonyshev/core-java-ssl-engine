package sslengine.example.map.server;

import sslengine.example.map.dto.MtTransferReq;
import sslengine.example.map.dto.MtTransferRes;
import sslengine.example.simpleobject.dto.SimpleRequestDto;
import sslengine.example.simpleobject.dto.SimpleResponseDto;
import sslengine.example.simpleobject.server.SimpleServerHandler;
import sslengine.example.simpleobject.server.SimpleServerRequestProcessor;
import sslengine.server.EventHandler;
import sslengine.server.SocketProcessor;

import javax.net.ssl.SSLEngine;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.SocketChannel;

public class MtMapServerSocketProcessor extends SocketProcessor {

    private static ServerMapRequestProcessor mapRequestProcessor = new ServerMapRequestProcessor();

    private MtMapServerHandler mtMapServerHandler;

    public MtMapServerSocketProcessor(SocketChannel socketChannel, SSLEngine sslEngine, EventHandler handler) throws FileNotFoundException {
        super(socketChannel, sslEngine, handler);
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
