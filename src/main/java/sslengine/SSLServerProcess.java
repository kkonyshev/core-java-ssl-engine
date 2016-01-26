package sslengine;

import org.apache.log4j.Logger;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class SSLServerProcess {

    private Executor serverThread = Executors.newSingleThreadExecutor();
    private ServerThread command;

    public static SSLServerProcess createInstance(NioSslServerThreaded server) {
        return new SSLServerProcess(server);
    }

    protected SSLServerProcess(NioSslServerThreaded server) {
        command = new ServerThread(server);
        serverThread.execute(command);
    }

    public void stop() {
        command.stopThread();
    }

    private static class ServerThread implements Runnable {

        protected final Logger LOG = Logger.getLogger(getClass());

        private NioSslServerThreaded server;

        public ServerThread(NioSslServerThreaded server) {
            this.server = server;
        }

        @Override
        public void run() {
            try {
                LOG.debug("Starting server thread process");
                server.start();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                stopThread();
            }
        }

        public void stopThread() {
            if (server!=null) {
                server.stop();
            }
            LOG.debug("Server stopped");
        }
    }
}
