package tp1.impl.server.rest;

import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import tp1.impl.server.rest.resources.SpreadsheetProxy;
import tp1.impl.server.rest.resources.SpreadsheetRest;
import tp1.impl.util.InsecureHostnameVerifier;
import tp1.impl.util.discovery.Discovery;
import tp1.impl.util.dropbox.DropboxAPI;
import tp1.impl.util.dropbox.arguments.PathV2Args;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.net.InetAddress;
import java.net.URI;

import java.util.List;
import java.util.logging.Logger;

public class ProxyServer {
    private static final Logger Log = Logger.getLogger(SpreadsheetProxy.class.getName());

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
            String secret = args[2];
            String serverURI = String.format("https://%s:%s/rest", ip, PORT);

            DropboxAPI dropbox = new DropboxAPI();
            String directory = domain;
            if (args[1].equalsIgnoreCase("true")) {
                dropbox.delete(directory);
            }

            boolean success = dropbox.createDirectory(directory);
            if (success)
                System.out.println("Directory '" + directory + "' created successfuly.");
            else
                System.out.println("Failed to create directory '" + directory + "'");

            // This allows client code executed by this server to ignore hostname verification
            HttpsURLConnection.setDefaultHostnameVerifier(new InsecureHostnameVerifier());

            ResourceConfig config = new ResourceConfig();
            config.register(new SpreadsheetProxy(domain, serverURI, secret));

            JdkHttpServerFactory.createHttpServer(URI.create(serverURI), config, SSLContext.getDefault());

            Log.info(String.format("%s Server ready @ %s\n", SERVICE, serverURI));

            Discovery.getInstance().start(domain, Discovery.DISCOVERY_ADDR, SERVICE, serverURI);

        } catch (Exception e) {
            Log.severe(e.getMessage());
        }
    }
}
