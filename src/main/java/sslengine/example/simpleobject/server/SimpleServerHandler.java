package sslengine.example.simpleobject.server;

import sslengine.example.simpleobject.dto.SimpleRequestDto;
import sslengine.example.simpleobject.dto.SimpleResponseDto;
import sslengine.server.ServerHandler;
import sslengine.utils.EncodeUtils;

public class SimpleServerHandler implements ServerHandler<SimpleRequestDto, SimpleResponseDto> {

    @Override
    public byte[] encodeRes(SimpleResponseDto requestDto) {
        return EncodeUtils.toBytes(requestDto);
    }

    @Override
    public SimpleRequestDto decodeReq(byte[] bytes) {
        return (SimpleRequestDto)EncodeUtils.toObject(bytes);
    }
}
