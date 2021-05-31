package tp1.impl.util.google;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import tp1.config.GoogleSheetsConfig;

public class GoogleAPI {

    public final static int MAX_RETRIES = 3;
    public final static long RETRY_PERIOD = 1000;
    public final static int CONNECTION_TIMEOUT = 1000;
    public final static int REPLY_TIMEOUT = 600;

    public static final String GET_RANGE = "https://sheets.googleapis.com/v4/spreadsheets/%s/values/%s";

    public GoogleAPI () {
    }

    public String[][] getSpreadsheetRange(String sheetId, String range) {
        String url =  String.format(GET_RANGE, sheetId, range);

        ClientConfig config = new ClientConfig();
        // how much time until we timeout when opening the TCP connection to the server
        config.property(ClientProperties.CONNECT_TIMEOUT, CONNECTION_TIMEOUT);
        // how much time do we wait for the reply of the server after sending the
        // request
        config.property(ClientProperties.READ_TIMEOUT, REPLY_TIMEOUT);
        Client client = ClientBuilder.newClient(config);

        WebTarget target = client.target(url);

        int retries = 0;
        while( retries < MAX_RETRIES ) {
            try {
                Response r = target.queryParam("key", GoogleSheetsConfig.API_KEY).request().accept(MediaType.APPLICATION_JSON).get();
                GoogleResult list = r.readEntity(GoogleResult.class);

                return list.getValues();
            } catch (ProcessingException pe) {
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
    }
}
