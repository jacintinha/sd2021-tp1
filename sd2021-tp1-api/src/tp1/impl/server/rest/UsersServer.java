package tp1.impl.server.rest;

import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import tp1.impl.server.rest.resources.UsersRest;
import tp1.impl.util.discovery.Discovery;

import java.net.InetAddress;
import java.net.URI;
import java.util.logging.Logger;

public class UsersServer {

    private static final Logger Log = Logger.getLogger(UsersServer.class.getName());

    static {
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s\n");
    }

    public static final int PORT = 8080;
    public static final String SERVICE = "users";

    public static void main(String[] args) {
        try {
            String domain = args[0];

            String ip = InetAddress.getLocalHost().getHostAddress();

            ResourceConfig config = new ResourceConfig();
            config.register(new UsersRest(domain));

            String serverURI = String.format("http://%s:%s/rest", ip, PORT);
            JdkHttpServerFactory.createHttpServer(URI.create(serverURI), config);

            Log.info(String.format("%s Server ready @ %s\n", SERVICE, serverURI));

            Discovery.getInstance().start(domain, Discovery.DISCOVERY_ADDR, SERVICE, serverURI);

        } catch (Exception e) {
            Log.severe(e.getMessage());
        }
    }

}
