package sslengine.client;

public interface ObjectToBytesDecoder<T> {
    byte[] encode(T object);
}
