package sslengine.client.impl;

import sslengine.handler.HandshakeHandler;
import sslengine.common.SSLSocketLayer;
import sslengine.utils.SSLUtils;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;


public class ClientConnection extends SSLSocketLayer {

	private String remoteAddress;

	private int port;

    private SSLEngine engine;

    private SocketChannel socketChannel;

    public ClientConnection(String remoteAddress, int port, SSLContext context)  {
    	this.remoteAddress = remoteAddress;
    	this.port = port;

        engine = context.createSSLEngine(remoteAddress, port);
        engine.setUseClientMode(true);

        SSLSession session = engine.getSession();
        myAppData = ByteBuffer.allocate(1024*16);
        myNetData = ByteBuffer.allocate(session.getPacketBufferSize());
        peerAppData = ByteBuffer.allocate(1024*16);
        peerNetData = ByteBuffer.allocate(session.getPacketBufferSize());
    }


    public boolean connect() throws Exception {
    	socketChannel = SocketChannel.open();
    	socketChannel.configureBlocking(false);
    	socketChannel.connect(new InetSocketAddress(remoteAddress, port));
    	while (!socketChannel.finishConnect()) {
    		// can do something here...
    	}

    	engine.beginHandshake();
    	return HandshakeHandler.doHandshake(socketChannel, engine);
    }


    public void write(byte[] data) throws IOException {
        write(socketChannel, engine, data);
    }


    @Override
    protected void write(SocketChannel socketChannel, SSLEngine engine, byte[] data) throws IOException {

        LOG.debug("About to write to the server...");

        myAppData.clear();
        myAppData.put(data);
        myAppData.flip();
        while (myAppData.hasRemaining()) {
            // The loop has a meaning for (outgoing) messages larger than 16KB.
            // Every wrap call will remove 16KB from the original message and send it to the remote peer.
            myNetData.clear();
            SSLEngineResult result = engine.wrap(myAppData, myNetData);
            switch (result.getStatus()) {
            case OK:
                myNetData.flip();
                while (myNetData.hasRemaining()) {
                    socketChannel.write(myNetData);
                }
                LOG.debug("Message sent to the server, size: " + data.length);
                break;
            case BUFFER_OVERFLOW:
                myNetData = SSLUtils.enlargePacketBuffer(engine, myNetData);
                break;
            case BUFFER_UNDERFLOW:
                throw new SSLException("Buffer underflow occurred after a wrap. I don't think we should ever get here.");
            case CLOSED:
                closeConnection(socketChannel, engine);
                return;
            default:
                throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
            }
        }

    }

    public void writeStream(InputStream ios) throws IOException {
        writeFromBuffer(socketChannel, engine, ios);
    }

    protected void writeFromBuffer(SocketChannel socketChannel, SSLEngine engine, InputStream ios) throws IOException {
        LOG.debug("About to write to the server...");

        byte[] buffer = new byte[1024];
        int read = 0;
        int totalRead = 0;

        //myAppData.clear();
        //myAppData.put(buffer);
        //myAppData.flip();
        while ((read = ios.read(buffer)) != -1) {
            // The loop has a meaning for (outgoing) messages larger than 16KB.
            // Every wrap call will remove 16KB from the original message and send it to the remote peer.
            myAppData.clear();
            myAppData.put(buffer);
            myAppData.flip();
            totalRead = totalRead + read;
            LOG.debug("total bytes read: " + totalRead);

            myNetData.clear();
            SSLEngineResult result = engine.wrap(myAppData, myNetData);
            switch (result.getStatus()) {
                case OK:
                    myNetData.flip();
                    while (myNetData.hasRemaining()) {
                        socketChannel.write(myNetData);
                    }
                    //LOG.debug("Message sent to the server: " + new String(buffer));
                    break;
                case BUFFER_OVERFLOW:
                    myNetData = SSLUtils.enlargePacketBuffer(engine, myNetData);
                    break;
                case BUFFER_UNDERFLOW:
                    throw new SSLException("Buffer underflow occured after a wrap. I don't think we should ever get here.");
                case CLOSED:
                    closeConnection(socketChannel, engine);
                    return;
                default:
                    throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
            }
        }

    }


    public byte[] read() throws Exception {
        return read(socketChannel, engine);
    }


    @Override
    protected byte[] read(SocketChannel socketChannel, SSLEngine engine) throws Exception  {

        LOG.debug("About to read from the server...");

        byte[] data = new byte[0];

        peerNetData.clear();
        int waitToReadMillis = 50;
        boolean exitReadLoop = false;
        while (!exitReadLoop) {
            int bytesRead = socketChannel.read(peerNetData);
            if (bytesRead > 0) {
                peerNetData.flip();
                while (peerNetData.hasRemaining()) {
                    peerAppData.clear();
                    SSLEngineResult result = engine.unwrap(peerNetData, peerAppData);
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
                        peerAppData = SSLUtils.enlargeApplicationBuffer(engine, peerAppData);
                        break;
                    case BUFFER_UNDERFLOW:
                        peerNetData = SSLUtils.handleBufferUnderflow(engine, peerNetData);
                        break;
                    case CLOSED:
                        closeConnection(socketChannel, engine);
                        return data;
                    default:
                        shutdown();
                        throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
                    }
                }
            } else if (bytesRead < 0) {
                handleEndOfStream(socketChannel, engine);
                return data;
            }
            Thread.sleep(waitToReadMillis);
        }

        return data;
    }

    public void shutdown() throws IOException {
        LOG.debug("About to close connection with the server...");
        closeConnection(socketChannel, engine);
        LOG.debug("Goodbye!");
    }

}
