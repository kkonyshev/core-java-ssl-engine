package sslengine.simpleobject.client;

import sslengine.client.ClientHandler;
import sslengine.simpleobject.dto.SimpleRequestDto;
import sslengine.simpleobject.dto.SimpleResponseDto;
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
