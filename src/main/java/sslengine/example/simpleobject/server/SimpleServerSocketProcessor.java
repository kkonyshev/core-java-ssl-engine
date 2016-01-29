package sslengine.example.simpleobject.server;

import sslengine.AbstractSocketLayer;
import sslengine.example.simpleobject.dto.SimpleRequestDto;
import sslengine.example.simpleobject.dto.SimpleResponseDto;
import sslengine.server.EventHandler;
import sslengine.server.ServerSocketProcessor;

import java.io.FileNotFoundException;
import java.io.IOException;

public class SimpleServerSocketProcessor<SocketData> extends ServerSocketProcessor {

    private SimpleServerHandler simpleServerHandler;
    private SimpleServerRequestProcessor simpleServerRequestProcessor;

    public SimpleServerSocketProcessor(AbstractSocketLayer<SocketData> socketLayer, EventHandler handler) {
        super(socketLayer, handler);
        this.simpleServerHandler = new SimpleServerHandler();
        this.simpleServerRequestProcessor = new SimpleServerRequestProcessor();
    }

    @Override
    public byte[] processRequest(byte[] clientData) throws IOException {
        //echo
        //write(socketChannel, sslEngine, clientData);
        byte[] localClientData = clientData;
        if (clientData !=null && clientData.length>0) {
            SimpleRequestDto clientDto = simpleServerHandler.decodeReq(clientData);
            LOG.debug("clientDto: " + clientDto);
            SimpleResponseDto serverResult = simpleServerRequestProcessor.process(clientDto);
            LOG.debug("serverResult: " + serverResult);
            localClientData = simpleServerHandler.encodeRes(serverResult);
        }

        return localClientData;
    }

    @Override
    public void run() {

    }
}
