package tp1.impl.util;

import com.sun.xml.ws.client.BindingProviderProperties;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.WebServiceException;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import tp1.api.User;
import tp1.api.service.rest.RestSpreadsheets;
import tp1.api.service.rest.RestUsers;
import tp1.api.service.soap.SheetsException;
import tp1.api.service.soap.SoapSpreadsheets;
import tp1.api.service.soap.SoapUsers;
import tp1.api.service.soap.UsersException;

import javax.xml.namespace.QName;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public class Mediator {

    public final static String USERS_WSDL = "/users/?wsdl";
    public final static String SPREADSHEETS_WSDL = "/spreadsheets/?wsdl";

    public final static int MAX_RETRIES = 5;
    public final static long RETRY_PERIOD = 5000;
    public final static int CONNECTION_TIMEOUT = 5000;
    public final static int REPLY_TIMEOUT = 1000;

    private static WebTarget restSetUp(String serverUrl, String path) {

        ClientConfig config = new ClientConfig();
        // how much time until we timeout when opening the TCP connection to the server
        config.property(ClientProperties.CONNECT_TIMEOUT, CONNECTION_TIMEOUT);
        // how much time do we wait for the reply of the server after sending the request
        config.property(ClientProperties.READ_TIMEOUT, REPLY_TIMEOUT);
        Client client = ClientBuilder.newClient(config);

        WebTarget target = client.target(serverUrl).path(path);

        return target;
    }

    private static SoapUsers soapSetUp(String serverUrl) {
        SoapUsers users = null;

        try {
            QName QNAME = new QName(SoapUsers.NAMESPACE, SoapUsers.NAME);
            Service service = Service.create(new URL(serverUrl + USERS_WSDL), QNAME);
            users = service.getPort(SoapUsers.class);
        } catch (WebServiceException | MalformedURLException e) {
            System.err.println("Could not contact the server: " + e.getMessage());
            System.exit(1);
        }

        //Set timeouts for executing operations
        ((BindingProvider) users).getRequestContext().put(BindingProviderProperties.CONNECT_TIMEOUT, CONNECTION_TIMEOUT);
        ((BindingProvider) users).getRequestContext().put(BindingProviderProperties.REQUEST_TIMEOUT, REPLY_TIMEOUT);
        return users;
    }

    private static SoapSpreadsheets soapSetUpSheets(String serverUrl) {
        SoapSpreadsheets spreadsheets = null;

        try {
            QName QNAME = new QName(SoapSpreadsheets.NAMESPACE, SoapSpreadsheets.NAME);
            Service service = Service.create(new URL(serverUrl + SPREADSHEETS_WSDL), QNAME);
            spreadsheets = service.getPort(SoapSpreadsheets.class);
        } catch (WebServiceException | MalformedURLException e) {
            System.err.println("Could not contact the server: " + e.getMessage());
            System.exit(1);
        }

        //Set timeouts for executing operations
        ((BindingProvider) spreadsheets).getRequestContext().put(BindingProviderProperties.CONNECT_TIMEOUT, CONNECTION_TIMEOUT);
        ((BindingProvider) spreadsheets).getRequestContext().put(BindingProviderProperties.REQUEST_TIMEOUT, REPLY_TIMEOUT);
        return spreadsheets;
    }

    public static int getUser(String serverUrl, String userId, String password) {
        System.out.println("Sending request to server.");

        if (serverUrl.split("/")[3].equals("rest")) {
            return getUserRest(restSetUp(serverUrl, RestUsers.PATH), userId, password);
        }
        return getUserSoap(soapSetUp(serverUrl), userId, password);
    }

    private static int getUserRest(WebTarget target, String userId, String password) {
        System.out.println("Sending request to server.");

        short retries = 0;

        while (retries < MAX_RETRIES) {
            try {
                Response r = target.path(userId).queryParam("password", password).request()
                        .accept(MediaType.APPLICATION_JSON).get();

                return r.getStatus();
            } catch (ProcessingException pe) {
                System.out.println("@Users: Timeout occurred");
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

    private static int getUserSoap(SoapUsers users, String userId, String password) {
        System.out.println("Sending request to server.");

        short retries = 0;

        while (retries < MAX_RETRIES) {

            try {
                User u = users.getUser(userId, password);
                System.out.println("User information: " + u.toString());
                return 200;
            } catch (UsersException e) {
                System.out.println("Could not get user: " + e.getMessage());
                return Response.Status.valueOf(e.getMessage()).getStatusCode();
            } catch (WebServiceException wse) {
                System.out.println("Communication error.");
                wse.printStackTrace();
                retries++;
                try {
                    Thread.sleep(RETRY_PERIOD);
                } catch (InterruptedException e) {
                    //nothing to be done here, if this happens we will just retry sooner.
                }
                System.out.println("Retrying to execute request.");
            }
        }
        return 500;
    }

    public static RangeValues getSpreadsheetRange(String serverUrl, String userId, String sheetId, String range, String secret) {
        System.out.println("Sending request to server.");
        if (serverUrl.split("/")[3].equals("rest")) {
            return getSpreadsheetRangeRest(restSetUp(serverUrl, "/import"), userId, range, secret);
        }
        return getSpreadsheetRangeSoap(soapSetUpSheets(serverUrl), userId, sheetId, range, secret);
    }

    public static RangeValues getSpreadsheetRangeRest(WebTarget target, String userId, String range, String secret) {
        System.out.println("Sending request to server.");

        short retries = 0;

        while (retries < MAX_RETRIES) {
            try {
                Response r = target.queryParam("userId", userId).queryParam("range", range).queryParam("secret", secret).request()
                        .accept(MediaType.APPLICATION_JSON).get();

                if (r.getStatus() == 200 && r.hasEntity()) {
                    return r.readEntity(RangeValues.class);
                } else
                    System.out.println("Error, HTTP error status: " + r.getStatus());

                return null;
            } catch (ProcessingException pe) {
                System.out.println("@Sheets: Timeout occurred");
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

    public static RangeValues getSpreadsheetRangeSoap(SoapSpreadsheets spreadsheets, String userId, String sheetId, String range, String secret) {
        System.out.println("Sending request to server.");

        short retries = 0;

        while (retries < MAX_RETRIES) {

            try {
                return spreadsheets.importValues(sheetId, userId, range, secret);
            } catch (SheetsException e) {
                return null;
            } catch (WebServiceException wse) {
                System.out.println("Communication error.");
                wse.printStackTrace();
                retries++;
                try {
                    Thread.sleep(RETRY_PERIOD);
                } catch (InterruptedException e) {
                    //nothing to be done here, if this happens we will just retry sooner.
                }
                System.out.println("Retrying to execute request.");
            }
        }
        return null;
    }

    public static int sendOperation(String serverURI, String operation, String secret, Long currentVersion) {
        WebTarget target = restSetUp(serverURI, RestSpreadsheets.PATH + "/operation");

        short retries = 0;

        while (retries < MAX_RETRIES) {
            try {
                Response r = target.queryParam("secret", secret).request().header(RestSpreadsheets.HEADER_VERSION, currentVersion).post(Entity.entity(operation, MediaType.APPLICATION_JSON));

                if (r.getStatus() == 204) {
                    return r.getStatus();
                }
                retries++;
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

    public static int deleteSpreadsheets(String serverUrl, String userId, String password, String secret) {
        System.out.println("Sending request to server.");
        if (serverUrl.split("/")[3].equals("rest")) {
            return deleteSpreadsheetsRest(restSetUp(serverUrl, RestSpreadsheets.PATH + "/delete"), userId, password, secret);
        }
        return deleteSpreadsheetsSoap(soapSetUpSheets(serverUrl), userId, password, secret);
    }

    public static int deleteSpreadsheetsRest(WebTarget target, String userId, String password, String secret) {
        System.out.println("Sending request to server.");

        short retries = 0;

        // Deleting user's spreadsheets must be done eventually
        while (retries < MAX_RETRIES) {
            try {
                Response r = target.path(userId).queryParam("password", password).queryParam("secret", secret).request().delete();

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

    public static int deleteSpreadsheetsSoap(SoapSpreadsheets spreadsheets, String userId, String password, String secret) {
        System.out.println("Sending request to server.");

        short retries = 0;

        while (retries < MAX_RETRIES) {

            try {
                spreadsheets.deleteUserSpreadsheets(userId, secret);
                return 200;
            } catch (WebServiceException wse) {
                System.out.println("Communication error.");
                wse.printStackTrace();
                retries++;
                try {
                    Thread.sleep(RETRY_PERIOD);
                } catch (InterruptedException e) {
                    //nothing to be done here, if this happens we will just retry sooner.
                }
                System.out.println("Retrying to execute request.");
            }
        }

        return 500;
    }

    public static List<String> askForOperations(Long startVersion, String secret, String serverURI) {
        WebTarget target = restSetUp(serverURI, RestSpreadsheets.PATH + "/operation");

        short retries = 0;

        while (retries < MAX_RETRIES) {
            try {
                Response r = target.queryParam("version", startVersion).queryParam("secret", secret).request().get();

                if (r.getStatus() == 200) {
                    return r.readEntity(List.class);
                }
                retries++;
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
    }
}
