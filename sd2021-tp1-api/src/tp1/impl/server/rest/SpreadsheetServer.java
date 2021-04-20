package tp1.impl.server.rest;

import jakarta.inject.Singleton;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import tp1.impl.server.resourceAbstraction.SpreadsheetResource;
import tp1.impl.server.rest.resources.SpreadsheetRest;
import tp1.impl.util.discovery.Discovery;

import java.net.InetAddress;
import java.net.URI;
import java.util.logging.Logger;

@Singleton
public class SpreadsheetServer {

    private static final Logger Log = Logger.getLogger(SpreadsheetServer.class.getName());

    static {
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s\n");
    }

    public static final int PORT = 8080;
    public static final String SERVICE = "sheets";

    public static void main(String[] args) {
        try {
            String ip = InetAddress.getLocalHost().getHostAddress();
            String domain = args[0];
            String serverURI = String.format("http://%s:%s/rest", ip, PORT);

            ResourceConfig config = new ResourceConfig();
            config.register(new SpreadsheetRest(domain, serverURI));


            JdkHttpServerFactory.createHttpServer(URI.create(serverURI), config);

            Log.info(String.format("%s Server ready @ %s\n", SERVICE, serverURI));

            Discovery.getInstance().start(domain, Discovery.DISCOVERY_ADDR, SERVICE, serverURI);

        } catch (Exception e) {
            Log.severe(e.getMessage());
        }
    }
}
