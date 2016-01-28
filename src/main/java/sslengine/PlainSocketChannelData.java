package sslengine;

import java.nio.channels.SocketChannel;

public class PlainSocketChannelData {
    public final SocketChannel socketChannel;

    public PlainSocketChannelData(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }
}
