package tp1.impl.server.rest;

import jakarta.inject.Singleton;
import org.apache.zookeeper.*;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import tp1.impl.server.rest.resources.SpreadsheetRep;
import tp1.impl.util.InsecureHostnameVerifier;
import tp1.impl.util.discovery.Discovery;
import tp1.impl.util.zookeeper.ZookeeperProcessor;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.net.InetAddress;
import java.net.URI;
import java.util.List;
import java.util.logging.Logger;

@Singleton
public class SpreadsheetRepServer {

    private static final Logger Log = Logger.getLogger(SpreadsheetRepServer.class.getName());

    static {
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s\n");
    }

    public static final int PORT = 8082;
    public static final String SERVICE = "sheets";

    public static void main(String[] args) {
        try {
            String ip = InetAddress.getLocalHost().getHostAddress();
            String domain = args[0];
            String serverURI = String.format("https://%s:%s/rest", ip, PORT);

            ZookeeperProcessor zk = keepTheZoo(domain, serverURI);

            // This allows client code executed by this server to ignore hostname verification
            HttpsURLConnection.setDefaultHostnameVerifier(new InsecureHostnameVerifier());

            ResourceConfig config = new ResourceConfig();
            config.register(new SpreadsheetRep(domain, serverURI, args[1], zk));

            JdkHttpServerFactory.createHttpServer(URI.create(serverURI), config, SSLContext.getDefault());

            Log.info(String.format("%s Server ready @ %s\n", SERVICE, serverURI));

            Discovery.getInstance().start(domain, Discovery.DISCOVERY_ADDR, SERVICE, serverURI);



        } catch (Exception e) {
            Log.severe(e.getMessage());
        }
    }

    public static ZookeeperProcessor keepTheZoo(String domain, String serverURI) throws Exception {
        ZookeeperProcessor zk = ZookeeperProcessor.getInstance("localhost:2181,kafka:2181");

        String persistentPath = zk.write("/"+ domain, CreateMode.PERSISTENT);

        if (persistentPath != null) {
            Log.info("Created znode: " + persistentPath);
        } else {
            Log.info("Node already existed");
        }

        String newPath = zk.write("/" + domain + "/sheets_", CreateMode.EPHEMERAL_SEQUENTIAL);
        Log.info("Created child znode: " + newPath);

        System.out.println(newPath);

        // TODO ask
        Thread.sleep(5*1000);

        // TODO
        List<String> l = zk.getChildren("/" + domain, new Watcher() {
            @Override
            public void process(WatchedEvent event) {
                List<String> lst = zk.getChildren("/" + domain, this);
                lst.stream().forEach(e -> System.out.println(e));
                System.out.println();
                setPrimary(newPath, domain, lst.get(0), zk, serverURI);
            }
        });

        setPrimary(newPath, domain, l.get(0), zk, serverURI);

        return zk;
    }

    public static void setPrimary(String newPath, String domain, String path, ZookeeperProcessor zk, String serverURI) {
        String pathToCheck = "/" + domain + "/" + path;
        System.out.println(pathToCheck.equals(newPath));
        if (pathToCheck.equals(newPath)) {
            zk.setPrimary(serverURI);
        }
    }
}
