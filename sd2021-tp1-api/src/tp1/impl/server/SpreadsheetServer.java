package tp1.impl.server;

import java.net.InetAddress;
import java.net.URI;
import java.util.function.Supplier;
import java.util.logging.Logger;

import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import tp1.impl.discovery.Discovery;
import tp1.impl.server.resources.SpreadsheetResource;

public class SpreadsheetServer {

	private static final Logger Log = Logger.getLogger(SpreadsheetServer.class.getName());

	static {
		System.setProperty("java.net.preferIPv4Stack", "true");
		System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s\n");
	}

	public static final int PORT = 8080;
	public static final String SERVICE = "sheets";
	public static String serverURI = "";
	public static String domain = "";

	public static void main(String[] args) {
		try {
			String ip = InetAddress.getLocalHost().getHostAddress();

			domain = args[0];

			ResourceConfig config = new ResourceConfig();
			config.register(SpreadsheetResource.class);

			serverURI = String.format("http://%s:%s/rest", ip, PORT);
			JdkHttpServerFactory.createHttpServer(URI.create(serverURI), config);

			Log.info(String.format("%s Server ready @ %s\n", SERVICE, serverURI));

			// More code can be executed here...
			Discovery.getInstance().start(domain, Discovery.DISCOVERY_ADDR, SERVICE, serverURI);

		} catch (Exception e) {
			Log.severe(e.getMessage());
		}
	}
}
