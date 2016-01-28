package sslengine.simpleobject.server;

import sslengine.simpleobject.dto.SimpleRequestDto;
import sslengine.simpleobject.dto.SimpleResponseDto;
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
