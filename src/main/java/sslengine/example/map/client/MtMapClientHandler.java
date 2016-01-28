package sslengine.example.map.client;

import sslengine.client.ClientHandler;
import sslengine.example.map.dto.MtTransferReq;
import sslengine.example.map.dto.MtTransferRes;
import sslengine.example.simpleobject.dto.SimpleRequestDto;
import sslengine.example.simpleobject.dto.SimpleResponseDto;
import sslengine.utils.EncodeUtils;

public class MtMapClientHandler implements ClientHandler<MtTransferReq, MtTransferRes> {

    @Override
    public byte[] encodeReq(MtTransferReq reqDto) {
        return EncodeUtils.toBytes(reqDto);
    }

    @Override
    public MtTransferRes decodeRes(byte[] bytes) {
        return (MtTransferRes)EncodeUtils.toObject(bytes);
    }
}
