package tp1.impl.server.rest;

import jakarta.inject.Singleton;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import tp1.impl.server.rest.resources.SpreadsheetRest;
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
            String serverURI = String.format("https://%s:%s/rest", ip, PORT);

            // This allows client code executed by this server to ignore hostname verification
            HttpsURLConnection.setDefaultHostnameVerifier(new InsecureHostnameVerifier());

            ResourceConfig config = new ResourceConfig();
            config.register(new SpreadsheetRest(domain, serverURI, args[1]));

            JdkHttpServerFactory.createHttpServer(URI.create(serverURI), config, SSLContext.getDefault());

            Log.info(String.format("%s Server ready @ %s\n", SERVICE, serverURI));

            Discovery.getInstance().start(domain, Discovery.DISCOVERY_ADDR, SERVICE, serverURI);

            ZookeeperProcessor zk = ZookeeperProcessor.getInstance("localhost:2181,kafka:2181");
            String persistentPath = zk.write("/"+ domain, CreateMode.PERSISTENT);

            if(persistentPath != null) {
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
//                    lst.stream().forEach(e -> System.out.println(e));
//                    System.out.println();
                    isPrimary(newPath, domain, lst.get(0));
                }
            });

            isPrimary(newPath, domain, l.get(0));

        } catch (Exception e) {
            Log.severe(e.getMessage());
        }
    }

    public static void isPrimary(String newPath, String domain, String path) {
        System.out.println("log: checking if primary");
        System.out.println("isPrimary " + ("/"+domain+"/"+path).equals(newPath));
    }
}
