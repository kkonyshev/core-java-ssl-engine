package sslengine;

import javax.net.ssl.SSLEngine;
import java.nio.channels.SocketChannel;

public class SSLSocketChannelData extends SocketChannelData {
    public final SSLEngine socketSSLEngine;
    public SSLSocketChannelData(SocketChannel socketChannel, SSLEngine socketSSLEngine) {
        super(socketChannel);
        this.socketSSLEngine = socketSSLEngine;
    }
}
