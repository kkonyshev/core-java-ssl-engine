package sslengine;

import javax.net.ssl.SSLContext;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by ka on 16/01/16.
 */
public class DemoClient {
    public static void main(String[] argv) throws Exception {
        SSLContext context = SSLContext.getInstance("TLSv1.2");
        context.init(
                SSLUtils.createKeyManagers("./src/main/resources/client.jks", "storepass", "keypass"),
                SSLUtils.createTrustManagers("./src/main/resources/trustedCerts.jks", "storepass"),
                new SecureRandom()
        );

        ExecutorService srv = Executors.newFixedThreadPool(10);
        for (int i=0; i<1; i++) {
            srv.submit(new ClientCommand(context));
        }

        srv.shutdown();
        //Thread.sleep(5000);
    }

    private static class ClientCommand implements Runnable {
        private static int cnt = 0;
        private NioSslClientThreadLocal client;
        public ClientCommand(SSLContext context) throws Exception {
            this.client = new NioSslClientThreadLocal("localhost", 9222, context);
        }
        @Override
        public void run() {
            try {
                client.connect();
                for (int j=0; j<20; j++) {
                    cnt++;
                    String toSend = "Hello! I am a client #" + cnt;
                    sendAndCheck(toSend);
                }
                client.shutdown();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void sendAndCheck(String toSend) throws Exception {
            byte[] receivedData = send(toSend);

            String res = new String(receivedData);
            System.out.println("RESULT: " + res);
            if (!toSend.equalsIgnoreCase(res)) {
                throw new RuntimeException();
            }
        }

        private byte[] send(String toSend) throws Exception {
            byte[] dataToSend = toSend.getBytes("UTF-8");
            client.write(dataToSend);
            return client.read();
        }
    }
}
