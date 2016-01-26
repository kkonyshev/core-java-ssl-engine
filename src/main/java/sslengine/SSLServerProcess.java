package sslengine;

import org.apache.log4j.Logger;
import sslengine.server.ServerConnectionAcceprot;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class SSLServerProcess {

    private Executor serverThread = Executors.newSingleThreadExecutor();
    private ServerThread command;

    public static SSLServerProcess createInstance(ServerConnectionAcceprot server) {
        return new SSLServerProcess(server);
    }

    protected SSLServerProcess(ServerConnectionAcceprot server) {
        command = new ServerThread(server);
        serverThread.execute(command);
    }

    public void stop() {
        command.stopThread();
    }

    private static class ServerThread implements Runnable {

        protected final Logger LOG = Logger.getLogger(getClass());

        private ServerConnectionAcceprot server;

        public ServerThread(ServerConnectionAcceprot server) {
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
