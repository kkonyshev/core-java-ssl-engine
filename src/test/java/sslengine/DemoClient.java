package sslengine;

import org.apache.log4j.Logger;
import sslengine.client.ClientConnector;
import sslengine.handler.HandshakeHandler;
import sslengine.utils.SSLUtils;

import javax.net.ssl.SSLContext;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DemoClient {

    public static void createFile(String filePath, Long fileSize) {
        try {
            RandomAccessFile f = new RandomAccessFile(filePath, "rw");
            f.setLength(fileSize);
        } catch (Exception e) {
            Logger.getLogger(DemoClient.class).error(e.getMessage(), e);
        }
    }

    public static void main(String[] argv) throws Exception {
        SSLContext context = SSLContext.getInstance("TLSv1.2");
        context.init(
                SSLUtils.createKeyManagers("src/test/resources/client.private", "clientpw", "clientpw"),
                SSLUtils.createTrustManagers("src/test/resources/server.public", "public"),
                new SecureRandom()
        );

        ExecutorService srv = Executors.newFixedThreadPool(4);
        for (int i=0; i<1; i++) {
            srv.submit(new ClientCommand(context));
        }
        srv.shutdown();
    }

    private static class ClientCommand implements Runnable {
        private static int cnt = 0;
        private ClientConnector client;
        public ClientCommand(SSLContext context) throws Exception {
            this.client = new ClientConnector("localhost", 9222, context);
        }
        @Override
        public void run() {
            try {
                client.connect();
                for (int j=0; j<1; j++) {
                    cnt++;
                    String toSend = "Hello! I am a client #" + cnt;
                    sendAndCheck(toSend);
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    client.shutdown();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        private void sendAndCheck(String toSend) throws Exception {
            byte[] receivedData = send(toSend);

            String res = new String(receivedData);
            System.out.println("RESULT: " + res);
            /*
            if (toSend.equalsIgnoreCase(res)) {
                throw new RuntimeException();
            }
            */
        }

        private byte[] send(String toSend) throws Exception {
            String filePath = "src/main/resources/upload_test.out";
            createFile(filePath, 1024*16L);
            Path path = Paths.get(filePath);
            byte[] dataHuge = Files.readAllBytes(path);

            byte[] dataToSend = toSend.getBytes("UTF-8");
            //InputStream ios = new FileInputStream("src/main/resources/huge1");

            client.write(dataHuge);
            return client.read();
        }
    }
}
