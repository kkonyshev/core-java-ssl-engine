package sslengine;

import org.apache.log4j.Logger;
import sslengine.server.ServerConnectionAcceptor;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ServerProcess {

    private Executor serverThread = Executors.newSingleThreadExecutor();
    private ServerThread command;

    public static ServerProcess createInstance(ServerConnectionAcceptor server) {
        return new ServerProcess(server);
    }

    protected ServerProcess(ServerConnectionAcceptor server) {
        command = new ServerThread(server);
        serverThread.execute(command);
    }

    public void stop() {
        command.stopThread();
    }

    private static class ServerThread implements Runnable {

        protected final Logger LOG = Logger.getLogger(getClass());

        private ServerConnectionAcceptor server;

        public ServerThread(ServerConnectionAcceptor server) {
            this.server = server;
        }

        @Override
        public void run() {
            try {
                LOG.info("Starting server thread process");
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
            LOG.info("Server stopped");
        }
    }
}
