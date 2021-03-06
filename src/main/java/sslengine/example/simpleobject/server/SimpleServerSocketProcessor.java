package sslengine.example.simpleobject.server;

import sslengine.server.SocketProcessor;
import sslengine.example.simpleobject.dto.SimpleRequestDto;
import sslengine.example.simpleobject.dto.SimpleResponseDto;
import sslengine.server.EventHandler;

import javax.net.ssl.SSLEngine;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.SocketChannel;

public class SimpleServerSocketProcessor extends SocketProcessor {

    private SimpleServerHandler simpleServerHandler;
    private SimpleServerRequestProcessor simpleServerRequestProcessor;

    public SimpleServerSocketProcessor(SocketChannel socketChannel, SSLEngine sslEngine, EventHandler handler) throws FileNotFoundException {
        super(socketChannel, sslEngine, handler);
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
}
