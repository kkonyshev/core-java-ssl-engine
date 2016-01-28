package sslengine.client;

import org.apache.log4j.Logger;
import sslengine.client.ClientConnectionFactory;
import sslengine.client.ClientHandler;

public abstract class AbstractClient<RequestDto, ResponseDto> {

    protected final Logger LOG = Logger.getLogger(getClass());

    protected ClientConnectionFactory connectionFactory;
    protected ClientHandler<RequestDto, ResponseDto> clientHandler;

    protected AbstractClient(ClientConnectionFactory connectionFactory, ClientHandler<RequestDto, ResponseDto> clientHandler) throws Exception {
        this.connectionFactory = connectionFactory;
        this.clientHandler = clientHandler;
    }

    abstract public ResponseDto call(RequestDto requestDto) throws Exception;
}
