package sslengine.server.impl;

import sslengine.dto.SimpleRequestDto;
import sslengine.dto.SimpleResponseDto;
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
