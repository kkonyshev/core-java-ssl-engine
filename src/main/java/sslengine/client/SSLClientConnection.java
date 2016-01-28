package sslengine.client;

import sslengine.HandshakeHandler;
import sslengine.SSLSocketChannelData;
import sslengine.SSLSocketLayer;
import sslengine.utils.SSLUtils;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;


public class SSLClientConnection extends SSLSocketLayer implements ClientConnection {

	private String remoteAddress;
	private int port;

    private SSLSocketChannelData sslSocketChannelData;

    public SSLClientConnection(String remoteAddress, int port, SSLContext context)  {
    	this.remoteAddress = remoteAddress;
    	this.port = port;

        SSLEngine engine = context.createSSLEngine(remoteAddress, port);
        engine.setUseClientMode(true);

        SSLSession session = engine.getSession();
        myAppData = ByteBuffer.allocate(1024*16);
        myNetData = ByteBuffer.allocate(session.getPacketBufferSize());
        peerAppData = ByteBuffer.allocate(1024*16);
        peerNetData = ByteBuffer.allocate(session.getPacketBufferSize());

        this.sslSocketChannelData = new SSLSocketChannelData(null, engine);
    }


    public boolean connect() throws Exception {
        SocketChannel socketChannel = SocketChannel.open();
        this.sslSocketChannelData = new SSLSocketChannelData(socketChannel, this.sslSocketChannelData.socketSSLEngine);

        this.sslSocketChannelData.socketChannel.configureBlocking(false);
        this.sslSocketChannelData.socketChannel.connect(new InetSocketAddress(remoteAddress, port));
    	while (!this.sslSocketChannelData.socketChannel.finishConnect()) {
            Thread.sleep(100);
            LOG.trace("wait for connection.");
        }
        LOG.info("Open connection to: " + this.sslSocketChannelData.socketChannel.getLocalAddress());


        this.sslSocketChannelData.socketSSLEngine.beginHandshake();
    	return HandshakeHandler.doHandshake(socketChannel, this.sslSocketChannelData.socketSSLEngine);
    }


    public void write(byte[] data) throws IOException {
        write(this.sslSocketChannelData, data);
    }


    @Override
    protected void write(SSLSocketChannelData sslSocketChannelData, byte[] data) throws IOException {
        LOG.debug("Client writing to the server...");
        myAppData.clear();
        myAppData.put(data);
        myAppData.flip();
        while (myAppData.hasRemaining()) {
            // The loop has a meaning for (outgoing) messages larger than 16KB.
            // Every wrap call will remove 16KB from the original message and send it to the remote peer.
            myNetData.clear();
            SSLEngineResult result = this.sslSocketChannelData.socketSSLEngine.wrap(myAppData, myNetData);
            switch (result.getStatus()) {
            case OK:
                myNetData.flip();
                while (myNetData.hasRemaining()) {
                    this.sslSocketChannelData.socketChannel.write(myNetData);
                }
                LOG.debug("Message sent to the server, size: " + data.length);
                break;
            case BUFFER_OVERFLOW:
                myNetData = SSLUtils.enlargePacketBuffer(this.sslSocketChannelData.socketSSLEngine, myNetData);
                break;
            case BUFFER_UNDERFLOW:
                throw new SSLException("Buffer underflow occurred after a wrap. I don't think we should ever get here.");
            case CLOSED:
                closeConnection(this.sslSocketChannelData);
                return;
            default:
                throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
            }
        }

    }

    public byte[] read() throws Exception {
        return read(this.sslSocketChannelData);
    }


    @Override
    protected byte[] read(SSLSocketChannelData sslSocketChannelData) throws Exception  {
        LOG.debug("Client reading from the server...");

        byte[] data = new byte[0];

        peerNetData.clear();
        int waitToReadMillis = 50;
        boolean exitReadLoop = false;
        while (!exitReadLoop) {
            int bytesRead = sslSocketChannelData.socketChannel.read(peerNetData);
            if (bytesRead > 0) {
                peerNetData.flip();
                while (peerNetData.hasRemaining()) {
                    peerAppData.clear();
                    SSLEngineResult result = sslSocketChannelData.socketSSLEngine.unwrap(peerNetData, peerAppData);
                    switch (result.getStatus()) {
                    case OK:
                        peerAppData.flip();
                        //LOG.debug("Server response: " + new String(peerAppData.array(), peerAppData.position(), peerAppData.limit()));
                        byte[] array = peerAppData.array();
                        int arrayOffset = peerAppData.arrayOffset();
                        data = Arrays.copyOfRange(array, arrayOffset + peerAppData.position(), arrayOffset + peerAppData.limit());
                        exitReadLoop = true;
                        LOG.debug("Server response length: " + data.length);
                        break;
                    case BUFFER_OVERFLOW:
                        peerAppData = SSLUtils.enlargeApplicationBuffer(sslSocketChannelData.socketSSLEngine, peerAppData);
                        break;
                    case BUFFER_UNDERFLOW:
                        peerNetData = SSLUtils.handleBufferUnderflow(sslSocketChannelData.socketSSLEngine, peerNetData);
                        break;
                    case CLOSED:
                        closeConnection(sslSocketChannelData);
                        return data;
                    default:
                        close();
                        throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
                    }
                }
            } else if (bytesRead < 0) {
                handleEndOfStream(sslSocketChannelData);
                return data;
            }
            Thread.sleep(waitToReadMillis);
        }

        return data;
    }

    public void close() throws IOException {
        LOG.debug("About to close connection with the server...");
        closeConnection(this.sslSocketChannelData);
        LOG.debug("Goodbye!");
    }

}
