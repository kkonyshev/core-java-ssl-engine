package sslengine.simpleobject.server;

import sslengine.simpleobject.dto.SimpleRequestDto;
import sslengine.simpleobject.dto.SimpleResponseDto;
import sslengine.server.ServerRequestProcessor;

import java.util.Date;

public class SimpleServerRequestProcessor implements ServerRequestProcessor<SimpleRequestDto, SimpleResponseDto> {

    @Override
    public SimpleResponseDto process(SimpleRequestDto simpleRequestDto) {
        return new SimpleResponseDto(simpleRequestDto, new Date());
    }
}
