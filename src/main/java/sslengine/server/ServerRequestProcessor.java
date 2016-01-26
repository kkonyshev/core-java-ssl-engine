package sslengine.server;

public interface ServerRequestProcessor<RequestDto, ResponseDto> {
    ResponseDto process(RequestDto requestDto);
}
