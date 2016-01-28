package sslengine.example.simpleobject.server;

import sslengine.SSLSocketChannelData;
import sslengine.example.simpleobject.dto.SimpleRequestDto;
import sslengine.example.simpleobject.dto.SimpleResponseDto;
import sslengine.server.EventHandler;
import sslengine.server.SSLSocketProcessor;

import java.io.FileNotFoundException;
import java.io.IOException;

public class SimpleServerSSLSocketProcessor extends SSLSocketProcessor {

    private SimpleServerHandler simpleServerHandler;
    private SimpleServerRequestProcessor simpleServerRequestProcessor;

    public SimpleServerSSLSocketProcessor(SSLSocketChannelData sslSocketChannelData, EventHandler handler) throws FileNotFoundException {
        super(sslSocketChannelData, handler);
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
