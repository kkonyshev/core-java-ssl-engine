package sslengine.server.impl;

import sslengine.dto.SimpleRequestDto;
import sslengine.dto.SimpleResponseDto;
import sslengine.server.ServerRequestProcessor;

import java.util.Date;

public class SimpleServerRequestProcessor implements ServerRequestProcessor<SimpleRequestDto, SimpleResponseDto> {

    @Override
    public SimpleResponseDto process(SimpleRequestDto simpleRequestDto) {
        return new SimpleResponseDto(simpleRequestDto, new Date());
    }
}
