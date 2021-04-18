package tp1.impl.util;

import com.sun.xml.ws.client.BindingProviderProperties;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.WebServiceException;
import tp1.api.User;
import tp1.api.service.soap.SoapUsers;
import tp1.api.service.soap.UsersException;

import javax.xml.namespace.QName;
import java.net.MalformedURLException;
import java.net.URL;

public class MediatorSoap {

    public final static String USERS_WSDL = "/users/?wsdl";

    public final static int MAX_RETRIES = 3;
    public final static long RETRY_PERIOD = 10000;
    public final static int CONNECTION_TIMEOUT = 10000;
    public final static int REPLY_TIMEOUT = 10000;

    public static int getUser(String serverUrl, String userId, String password) {
        System.out.println("Sending request to server.");

        SoapUsers users = null;

        try {
            QName QNAME = new QName(SoapUsers.NAMESPACE, SoapUsers.NAME);
            Service service = Service.create( new URL(serverUrl + USERS_WSDL), QNAME);
            users = service.getPort( tp1.api.service.soap.SoapUsers.class );
        } catch (WebServiceException | MalformedURLException e) {
            System.err.println("Could not contact the server: " + e.getMessage());
            System.exit(1);
        }

        //Set timeouts for executing operations
        ((BindingProvider) users).getRequestContext().put(BindingProviderProperties.CONNECT_TIMEOUT, CONNECTION_TIMEOUT);
        ((BindingProvider) users).getRequestContext().put(BindingProviderProperties.REQUEST_TIMEOUT, REPLY_TIMEOUT);

        System.out.println("Sending request to server.");

        short retries = 0;
        boolean success = false;

        while(!success && retries < MAX_RETRIES) {

            try {
                User u = users.getUser(userId, password);
                System.out.println("User information: " + u.toString());
                success = true;
                return 200;
            } catch (UsersException e) {
                System.out.println("Could not get user: " + e.getMessage());
                success = true;
                return 403; // TODO
            } catch (WebServiceException wse) {
                System.out.println("Communication error.");
                wse.printStackTrace();
                retries++;
                try { Thread.sleep( RETRY_PERIOD ); } catch (InterruptedException e) {
                    //nothing to be done here, if this happens we will just retry sooner.
                }
                System.out.println("Retrying to execute request.");
            }
        }
        return 500;
    }

//    public static String[][] getSpreadsheetRange(String serverUrl, String userId, String sheetId, String range) {
//        System.out.println("Sending request to server. RENGE");
//
//        ClientConfig config = new ClientConfig();
//        // how much time until we timeout when opening the TCP connection to the server
//        config.property(ClientProperties.CONNECT_TIMEOUT, CONNECTION_TIMEOUT);
//        // how much time do we wait for the reply of the server after sending the
//        // request
//        config.property(ClientProperties.READ_TIMEOUT, REPLY_TIMEOUT);
//        Client client = ClientBuilder.newClient(config);
//
//        WebTarget target = client.target(serverUrl).path("/import");
//
//        short retries = 0;
//        boolean success = false;
//
//        while (!success && retries < MAX_RETRIES) {
//
//            try {
//                Response r = target.queryParam("userId", userId).queryParam("range", range).request()
//                        .accept(MediaType.APPLICATION_JSON).get();
//
//                if (r.getStatus() == 200 && r.hasEntity()) {
//                    String[][] ass = r.readEntity(String[][].class);
//                    return ass;
//                } else
//                    System.out.println("Error, HTTP error status: " + r.getStatus());
//
//                success = true;
////				return r.getStatus();
//
//            } catch (ProcessingException pe) {
//                System.out.println("Timeout occurred");
//                pe.printStackTrace();
//                retries++;
//                try {
//                    Thread.sleep(RETRY_PERIOD);
//                } catch (InterruptedException e) {
//                    // nothing to be done here, if this happens we will just retry sooner.
//                }
//                System.out.println("Retrying to execute request.");
//            }
//        }
//        return null;
////		return 500;
//    }
//
//    public static int deleteSpreadsheets(String serverUrl, String userId, String password) {
//        System.out.println("Sending request to server.");
//
//        ClientConfig config = new ClientConfig();
//        // how much time until we timeout when opening the TCP connection to the server
//        config.property(ClientProperties.CONNECT_TIMEOUT, CONNECTION_TIMEOUT);
//        // how much time do we wait for the reply of the server after sending the
//        // request
//        config.property(ClientProperties.READ_TIMEOUT, REPLY_TIMEOUT);
//        Client client = ClientBuilder.newClient(config);
//
//        WebTarget target = client.target(serverUrl).path(RestSpreadsheets.PATH + "/delete");
//
//        short retries = 0;
//        boolean success = false;
//
//        while (!success && retries < MAX_RETRIES) {
//            try {
//                Response r = target.path(userId).queryParam("password", password).request().delete();
//
//                success = true;
//                return r.getStatus();
//
//            } catch (ProcessingException pe) {
//                System.out.println("Timeout occurred");
//                pe.printStackTrace();
//                retries++;
//                try {
//                    Thread.sleep(RETRY_PERIOD);
//                } catch (InterruptedException e) {
//                    // nothing to be done here, if this happens we will just retry sooner.
//                }
//                System.out.println("Retrying to execute request.");
//            }
//        }
//        return 500;
//    }
}
