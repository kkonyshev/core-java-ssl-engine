package sslengine.client.impl;

import sslengine.client.ClientHandler;
import sslengine.dto.SimpleRequestDto;
import sslengine.dto.SimpleResponseDto;
import sslengine.utils.EncodeUtils;

public class SimpleClientHandler implements ClientHandler<SimpleRequestDto, SimpleResponseDto> {

    @Override
    public byte[] encodeReq(SimpleRequestDto reqDto) {
        return EncodeUtils.toBytes(reqDto);
    }

    @Override
    public SimpleResponseDto decodeRes(byte[] bytes) {
        return (SimpleResponseDto)EncodeUtils.toObject(bytes);
    }
}
