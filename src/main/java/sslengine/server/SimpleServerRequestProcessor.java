package sslengine.server;

import sslengine.dto.SimpleRequestDto;
import sslengine.dto.SimpleResponseDto;

import java.util.Date;

public class SimpleServerRequestProcessor implements ServerRequestProcessor<SimpleRequestDto, SimpleResponseDto> {

    @Override
    public SimpleResponseDto process(SimpleRequestDto simpleRequestDto) {
        return new SimpleResponseDto(simpleRequestDto, new Date());
    }
}
