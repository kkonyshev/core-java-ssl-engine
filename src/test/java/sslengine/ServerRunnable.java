package sslengine;

import javax.net.ssl.SSLContext;
import java.security.SecureRandom;

/**
 * This class provides a runnable that can be used to initialize a {@link NioSslServer} thread.
 * <p/>
 * Run starts the server, which will start listening to the configured IP address and port for
 * new SSL/TLS connections and serve the ones already connected to it.
 * <p/>
 * Also a stop method is provided in order to gracefully close the server and stop the thread.
 * 
 * @author <a href="mailto:travelling.with.code@gmail.com">Alex</a>
 */
public class ServerRunnable implements Runnable {

    NioSslServer server;
	
	@Override
	public void run() {
		try {
			SSLContext context = SSLContext.getInstance("TLSv1.2");
			context.init(
					SSLUtils.createKeyManagers("./src/main/resources/server.jks", "storepass", "keypass"),
					SSLUtils.createTrustManagers("./src/main/resources/trustedCerts.jks", "storepass"),
					new SecureRandom()
			);

			server = new NioSslServer("localhost", 9222, context);
			server.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Should be called in order to gracefully stop the server.
	 */
	public void stop() {
		server.stop();
	}
	
}
