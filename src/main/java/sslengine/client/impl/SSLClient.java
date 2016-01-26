package sslengine.client.impl;

import org.apache.log4j.Logger;
import sslengine.client.ClientConnectionFactory;
import sslengine.client.ClientHandler;

public abstract class SSLClient<RequestDto, ResponseDto> {

    protected final Logger LOG = Logger.getLogger(getClass());

    protected ClientConnectionFactory connectionFactory;
    protected ClientHandler<RequestDto, ResponseDto> clientHandler;

    protected SSLClient(ClientConnectionFactory connectionFactory, ClientHandler<RequestDto, ResponseDto> clientHandler) throws Exception {
        this.connectionFactory = connectionFactory;
        this.clientHandler = clientHandler;
    }

    abstract public ResponseDto call(RequestDto requestDto) throws Exception;
}
