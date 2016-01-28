package sslengine;

import java.nio.channels.SocketChannel;

public class SocketChannelData {
    public final SocketChannel socketChannel;

    public SocketChannelData(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }
}
