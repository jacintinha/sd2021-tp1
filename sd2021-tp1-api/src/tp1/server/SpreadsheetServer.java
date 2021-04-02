package tp1.server;

import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import tp1.discovery.Discovery;
import tp1.server.resources.UsersResource;

import java.net.InetAddress;
import java.net.URI;
import java.util.logging.Logger;

public class SpreadsheetServer {

    private static final Logger Log = Logger.getLogger(SpreadsheetServer.class.getName());

    static {
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s\n");
    }

    public static final int PORT = 8081; //TODO it will be given
    public static final String SERVICE = "SpreadsheetService";

    public static void main(String[] args) {
        try {
            String ip = InetAddress.getLocalHost().getHostAddress();

            ResourceConfig config = new ResourceConfig();
            config.register(SpreadsheetServer.class);

            String serverURI = String.format("http://%s:%s/rest", ip, PORT);
            JdkHttpServerFactory.createHttpServer(URI.create(serverURI), config);

            Log.info(String.format("%s Server ready @ %s\n", SERVICE, serverURI));

            // More code can be executed here...
            Discovery disc = new Discovery(Discovery.DISCOVERY_ADDR, SERVICE, serverURI);
            disc.start();

        } catch (Exception e) {
            Log.severe(e.getMessage());
        }
    }
}
