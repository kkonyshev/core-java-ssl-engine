package sslengine;

import javax.net.ssl.SSLContext;
import java.security.SecureRandom;

public class Demo {
	
	ServerRunnable serverRunnable;
	
	public Demo() {
		//System.setProperty("javax.net.debug", "all");

		serverRunnable = new ServerRunnable();
		Thread server = new Thread(serverRunnable);
		server.start();
	}
	
	public void runDemo() throws Exception {

		SSLContext context = SSLContext.getInstance("TLSv1.2");
		context.init(
				SSLUtils.createKeyManagers("./src/main/resources/client.jks", "storepass", "keypass"),
				SSLUtils.createTrustManagers("./src/main/resources/trustedCerts.jks", "storepass"),
				new SecureRandom()
		);

		NioSslClient client = new NioSslClient("localhost", 9222, context);

        byte[] dataToSend = "Hello! I am a client!".getBytes("UTF-8");
        client.connect();
        client.write(dataToSend);
        byte[] receivedData = client.read();

        System.out.println("RESULT: " + new String(receivedData));

        /*
		NioSslClient client2 = new NioSslClient("localhost", 9222, context);
		//NioSslClient client3 = new NioSslClient("TLSv1.2", "localhost", 9222);
		//NioSslClient client4 = new NioSslClient("TLSv1.2", "localhost", 9222);

		client2.connect();
		client2.write("Hello! I am another client!".getBytes("UTF-8"));
		client2.read();
		client2.shutdown();
		*/

		/*
		client3.connect();
		client4.connect();
		client3.write("Hello from client3!!!");
		client4.write("Hello from client4!!!");
		client3.read();
		client4.read();
		client3.shutdown();
		client4.shutdown();
		*/

        Thread.sleep(5000);
        client.shutdown();
		serverRunnable.stop();
	}
	
	public static void main(String[] args) throws Exception {
		Demo demo = new Demo();
		Thread.sleep(1000);	// Give the server some time to start.
		demo.runDemo();
	}
	
}
