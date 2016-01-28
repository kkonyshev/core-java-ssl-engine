package sslengine.client;

import sslengine.PlainSocketLayer;
import sslengine.PlainSocketChannelData;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;


public class PlainClientConnection extends PlainSocketLayer implements ClientConnection {

	private String remoteAddress;
	private int port;

    private PlainSocketChannelData plainSocketChannelData;

    public PlainClientConnection(String remoteAddress, int port)  {
    	this.remoteAddress = remoteAddress;
    	this.port = port;

        myAppData = ByteBuffer.allocate(1024*16);
        myNetData = ByteBuffer.allocate(1024*16);
        peerAppData = ByteBuffer.allocate(1024*16);
        peerNetData = ByteBuffer.allocate(1024*16);
    }


    public boolean connect() throws Exception {
        SocketChannel socketChannel = SocketChannel.open();
        this.plainSocketChannelData = new PlainSocketChannelData(socketChannel);

        this.plainSocketChannelData.socketChannel.configureBlocking(false);
        this.plainSocketChannelData.socketChannel.connect(new InetSocketAddress(remoteAddress, port));
    	while (!this.plainSocketChannelData.socketChannel.finishConnect()) {
            Thread.sleep(100);
            LOG.trace("wait for connection.");
        }
        LOG.info("Open connection to: " + this.plainSocketChannelData.socketChannel.getLocalAddress());

    	return true;
    }


    public void write(byte[] data) throws IOException {
        write(this.plainSocketChannelData, data);
    }


    @Override
    protected void write(PlainSocketChannelData plainSocketChannelData, byte[] data) throws IOException {
        LOG.debug("Client writing to the server...");
        myAppData.clear();
        myAppData.put(data);
        myAppData.flip();
        while (myAppData.hasRemaining()) {
            this.plainSocketChannelData.socketChannel.write(myAppData);
        }
        LOG.debug("Message sent to the server, size: " + data.length);
    }

    public byte[] read() throws Exception {
        return read(this.plainSocketChannelData);
    }


    @Override
    protected byte[] read(PlainSocketChannelData plainSocketChannelData) throws Exception  {
        LOG.debug("Client reading from the server...");

        byte[] data = new byte[0];

        peerNetData.clear();
        int waitToReadMillis = 50;
        boolean exitReadLoop = false;
        while (!exitReadLoop) {
            int bytesRead = plainSocketChannelData.socketChannel.read(peerNetData);
            if (bytesRead > 0) {
                peerNetData.flip();
                while (peerNetData.hasRemaining()) {
                    peerAppData.clear();
                    byte[] array = peerNetData.array();
                    int arrayOffset = peerNetData.arrayOffset();
                    data = Arrays.copyOfRange(array, arrayOffset + peerNetData.position(), arrayOffset + peerNetData.limit());
                    exitReadLoop=true;
                    LOG.debug("Server response length: " + data.length);
                    break;
                }
            } else if (bytesRead < 0) {
                handleEndOfStream(plainSocketChannelData);
                return data;
            }
            Thread.sleep(waitToReadMillis);
        }

        return data;
    }

    public void close() throws IOException {
        LOG.debug("About to close connection with the server...");
        closeConnection(plainSocketChannelData);
        LOG.debug("Goodbye!");
    }

}
