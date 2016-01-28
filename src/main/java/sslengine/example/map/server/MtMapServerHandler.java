package sslengine.example.map.server;

import sslengine.example.map.dto.MtTransferReq;
import sslengine.example.map.dto.MtTransferRes;
import sslengine.example.simpleobject.dto.SimpleRequestDto;
import sslengine.example.simpleobject.dto.SimpleResponseDto;
import sslengine.server.ServerHandler;
import sslengine.utils.EncodeUtils;

public class MtMapServerHandler implements ServerHandler<MtTransferReq, MtTransferRes> {

    @Override
    public byte[] encodeRes(MtTransferRes transferRes) {
        return EncodeUtils.toBytes(transferRes);
    }

    @Override
    public MtTransferReq decodeReq(byte[] bytes) {
        return (MtTransferReq)EncodeUtils.toObject(bytes);
    }
}
