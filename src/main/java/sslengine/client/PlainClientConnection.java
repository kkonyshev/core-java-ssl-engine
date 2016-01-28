package sslengine.client;

import sslengine.PlainSocketLayer;
import sslengine.SocketChannelData;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;


public class PlainClientConnection extends PlainSocketLayer implements ClientConnection {

	private String remoteAddress;
	private int port;

    private SocketChannelData socketChannelData;

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
        this.socketChannelData = new SocketChannelData(socketChannel);

        this.socketChannelData.socketChannel.configureBlocking(false);
        this.socketChannelData.socketChannel.connect(new InetSocketAddress(remoteAddress, port));
    	while (!this.socketChannelData.socketChannel.finishConnect()) {
            Thread.sleep(100);
            LOG.trace("wait for connection.");
        }
        LOG.info("Open connection to: " + this.socketChannelData.socketChannel.getLocalAddress());

    	return true;
    }


    public void write(byte[] data) throws IOException {
        write(this.socketChannelData, data);
    }


    @Override
    protected void write(SocketChannelData socketChannelData, byte[] data) throws IOException {
        LOG.debug("Client writing to the server...");
        myAppData.clear();
        myAppData.put(data);
        myAppData.flip();
        while (myAppData.hasRemaining()) {
            this.socketChannelData.socketChannel.write(myAppData);
        }
        LOG.debug("Message sent to the server, size: " + data.length);
    }

    public byte[] read() throws Exception {
        return read(this.socketChannelData);
    }


    @Override
    protected byte[] read(SocketChannelData socketChannelData) throws Exception  {
        LOG.debug("Client reading from the server...");

        byte[] data = new byte[0];

        peerNetData.clear();
        int waitToReadMillis = 50;
        boolean exitReadLoop = false;
        while (!exitReadLoop) {
            int bytesRead = socketChannelData.socketChannel.read(peerNetData);
            if (bytesRead > 0) {
                peerNetData.flip();
                while (peerNetData.hasRemaining()) {
                    peerAppData.clear();
                    byte[] array = peerNetData.array();
                    int arrayOffset = peerNetData.arrayOffset();
                    data = Arrays.copyOfRange(array, arrayOffset + peerNetData.position(), arrayOffset + peerNetData.limit());
                    LOG.debug("Server response length: " + data.length);
                    break;
                }
            } else if (bytesRead < 0) {
                handleEndOfStream(socketChannelData);
                return data;
            }
            Thread.sleep(waitToReadMillis);
        }

        return data;
    }

    public void close() throws IOException {
        LOG.debug("About to close connection with the server...");
        closeConnection(socketChannelData);
        LOG.debug("Goodbye!");
    }

}
