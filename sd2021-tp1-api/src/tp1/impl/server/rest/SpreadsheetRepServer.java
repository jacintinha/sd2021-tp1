package tp1.impl.server.rest;

import jakarta.inject.Singleton;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import tp1.impl.server.rest.resources.SpreadsheetRep;
import tp1.impl.util.InsecureHostnameVerifier;
import tp1.impl.util.discovery.Discovery;
import tp1.impl.versioning.ReplicationManager;
import tp1.impl.versioning.VersionFilter;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.net.InetAddress;
import java.net.URI;
import java.util.logging.Logger;

@Singleton
public class SpreadsheetRepServer {

    private static final Logger Log = Logger.getLogger(SpreadsheetRepServer.class.getName());

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
            String serverURI = String.format("https://%s:%s/rest", ip, PORT);

            // This allows client code executed by this server to ignore hostname verification
            HttpsURLConnection.setDefaultHostnameVerifier(new InsecureHostnameVerifier());

            ReplicationManager repManager = new ReplicationManager();
            ResourceConfig config = new ResourceConfig();
            config.register(new VersionFilter(repManager));
            config.register(new SpreadsheetRep(domain, serverURI, args[1], repManager));

            JdkHttpServerFactory.createHttpServer(URI.create(serverURI), config, SSLContext.getDefault());

            Log.info(String.format("%s Server ready @ %s\n", SERVICE, serverURI));

            Discovery.getInstance().start(domain, Discovery.DISCOVERY_ADDR, SERVICE, serverURI);

        } catch (Exception e) {
            Log.severe(e.getMessage());
        }
    }

}
