package tp1.impl.server.soap;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import jakarta.xml.ws.Endpoint;
import tp1.impl.server.soap.WS.SpreadsheetWS;
import tp1.impl.util.InsecureHostnameVerifier;
import tp1.impl.util.discovery.Discovery;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class SpreadsheetServer {

    private static final Logger Log = Logger.getLogger(SpreadsheetServer.class.getName());

    static {
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s\n");
    }

    public static final int PORT = 8080;
    public static final String SERVICE = "sheets";
    public static final String SOAP_SPREADSHEETS_PATH = "/soap/spreadsheets";

    public static void main(String[] args) {
        try {
            String domain = args[0];

            String ip = InetAddress.getLocalHost().getHostAddress();
            String serverURI = String.format("https://%s:%s/soap", ip, PORT);

            // This allows client code executed by this server to ignore hostname verification
            HttpsURLConnection.setDefaultHostnameVerifier(new InsecureHostnameVerifier());

            // Create an https configurator to define the SSL/TLS context
            HttpsConfigurator configurator = new HttpsConfigurator(SSLContext.getDefault());

            HttpsServer server = HttpsServer.create(new InetSocketAddress(ip, PORT), 0);

            server.setHttpsConfigurator(configurator);

            server.setExecutor(Executors.newCachedThreadPool());

            Endpoint soapSpreadsheetsEndpoint = Endpoint.create(new SpreadsheetWS(domain, serverURI, args[1]));

            soapSpreadsheetsEndpoint.publish(server.createContext(SOAP_SPREADSHEETS_PATH));

            server.start();

            Log.info(String.format("%s Server ready @ %s\n", SERVICE, serverURI));

            Discovery.getInstance().start(domain, Discovery.DISCOVERY_ADDR, SERVICE, serverURI);

        } catch (Exception e) {
            Log.severe(e.getMessage());
        }
    }

}
