package sslengine.server;

public interface ServerHandler<Req, Res> {
    byte[] encodeRes(Res bytes);
    Req decodeReq(byte[] bytes);
}
