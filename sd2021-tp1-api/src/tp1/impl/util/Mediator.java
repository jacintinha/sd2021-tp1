package tp1.impl.util;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import tp1.api.service.rest.RestSpreadsheets;
import tp1.api.service.rest.RestUsers;

public class Mediator {

    public final static int MAX_RETRIES = 3;
    public final static long RETRY_PERIOD = 10000;
    public final static int CONNECTION_TIMEOUT = 10000;
    public final static int REPLY_TIMEOUT = 10000;

    public static int getUser(String serverUrl, String userId, String password) {
        System.out.println("Sending request to server.");

        ClientConfig config = new ClientConfig();
        // how much time until we timeout when opening the TCP connection to the server
        config.property(ClientProperties.CONNECT_TIMEOUT, CONNECTION_TIMEOUT);
        // how much time do we wait for the reply of the server after sending the
        // request
        config.property(ClientProperties.READ_TIMEOUT, REPLY_TIMEOUT);
        Client client = ClientBuilder.newClient(config);

        WebTarget target = client.target(serverUrl).path(RestUsers.PATH);

        short retries = 0;
        boolean success = false;

        while (!success && retries < MAX_RETRIES) {

            try {
                Response r = target.path(userId).queryParam("password", password).request()
                        .accept(MediaType.APPLICATION_JSON).get();

                success = true;
                return r.getStatus();

            } catch (ProcessingException pe) {
                System.out.println("Timeout occurred");
                pe.printStackTrace();
                retries++;
                try {
                    Thread.sleep(RETRY_PERIOD);
                } catch (InterruptedException e) {
                    // nothing to be done here, if this happens we will just retry sooner.
                }
                System.out.println("Retrying to execute request.");
            }
        }
        return 500;
    }

    public static String[][] getSpreadsheetRange(String serverUrl, String userId, String sheetId, String range) {
        System.out.println("Sending request to server.");

        ClientConfig config = new ClientConfig();
        // how much time until we timeout when opening the TCP connection to the server
        config.property(ClientProperties.CONNECT_TIMEOUT, CONNECTION_TIMEOUT);
        // how much time do we wait for the reply of the server after sending the
        // request
        config.property(ClientProperties.READ_TIMEOUT, REPLY_TIMEOUT);
        Client client = ClientBuilder.newClient(config);

        WebTarget target = client.target(serverUrl).path("/import");

        short retries = 0;
        boolean success = false;

        while (!success && retries < MAX_RETRIES) {

            try {
                Response r = target.queryParam("userId", userId).queryParam("range", range).request()
                        .accept(MediaType.APPLICATION_JSON).get();

                if (r.getStatus() == 200 && r.hasEntity()) {
                    return r.readEntity(String[][].class);
                } else
                    System.out.println("Error, HTTP error status: " + r.getStatus());

                success = true;
//				return r.getStatus();

            } catch (ProcessingException pe) {
                System.out.println("Timeout occurred");
                pe.printStackTrace();
                retries++;
                try {
                    Thread.sleep(RETRY_PERIOD);
                } catch (InterruptedException e) {
                    // nothing to be done here, if this happens we will just retry sooner.
                }
                System.out.println("Retrying to execute request.");
            }
        }
        return null;
//		return 500;
    }

    public static int deleteSpreadsheets(String serverUrl, String userId, String password) {
        System.out.println("Sending request to server.");

        ClientConfig config = new ClientConfig();
        // how much time until we timeout when opening the TCP connection to the server
        config.property(ClientProperties.CONNECT_TIMEOUT, CONNECTION_TIMEOUT);
        // how much time do we wait for the reply of the server after sending the
        // request
        config.property(ClientProperties.READ_TIMEOUT, REPLY_TIMEOUT);
        Client client = ClientBuilder.newClient(config);

        WebTarget target = client.target(serverUrl).path(RestSpreadsheets.PATH + "/delete");

        short retries = 0;
        boolean success = false;

        while (!success && retries < MAX_RETRIES) {
            try {
                Response r = target.path(userId).queryParam("password", password).request().delete();

                success = true;
                return r.getStatus();

            } catch (ProcessingException pe) {
                System.out.println("Timeout occurred");
                pe.printStackTrace();
                retries++;
                try {
                    Thread.sleep(RETRY_PERIOD);
                } catch (InterruptedException e) {
                    // nothing to be done here, if this happens we will just retry sooner.
                }
                System.out.println("Retrying to execute request.");
            }
        }
        return 500;
    }
}
