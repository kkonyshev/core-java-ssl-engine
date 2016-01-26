package sslengine.client;

public interface ClientHandler<Req, Res> {
    byte[] encodeReq(Req reqDto);
    Res decodeRes(byte[] bytes);
}
