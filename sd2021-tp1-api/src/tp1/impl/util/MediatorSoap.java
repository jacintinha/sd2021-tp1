package tp1.impl.util;

import com.sun.xml.ws.client.BindingProviderProperties;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.WebServiceException;
import tp1.api.User;
import tp1.api.service.soap.SheetsException;
import tp1.api.service.soap.SoapSpreadsheets;
import tp1.api.service.soap.SoapUsers;
import tp1.api.service.soap.UsersException;

import javax.xml.namespace.QName;
import java.net.MalformedURLException;
import java.net.URL;

public class MediatorSoap {

    public final static String USERS_WSDL = "/users/?wsdl";
    public final static String SPREADSHEETS_WSDL = "/spreadsheets/?wsdl";

    public final static int MAX_RETRIES = 3;
    public final static long RETRY_PERIOD = 1000;
    public final static int CONNECTION_TIMEOUT = 1000;
    public final static int REPLY_TIMEOUT = 600;

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
                // TODO
                return Integer.parseInt(e.getMessage());
//                return 403; // TODO
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

    // TODO
    public static String[][] getSpreadsheetRange(String serverUrl, String userId, String sheetId, String range) {
        System.out.println("Sending request to server.");

        SoapSpreadsheets spreadsheets = null;

        try {
            QName QNAME = new QName(SoapSpreadsheets.NAMESPACE, SoapSpreadsheets.NAME);
            Service service = Service.create( new URL(serverUrl + SPREADSHEETS_WSDL), QNAME);
            spreadsheets = service.getPort( tp1.api.service.soap.SoapSpreadsheets.class );
        } catch (WebServiceException | MalformedURLException e) {
            System.err.println("Could not contact the server: " + e.getMessage());
            System.exit(1);
        }

        //Set timeouts for executing operations
        ((BindingProvider) spreadsheets).getRequestContext().put(BindingProviderProperties.CONNECT_TIMEOUT, CONNECTION_TIMEOUT);
        ((BindingProvider) spreadsheets).getRequestContext().put(BindingProviderProperties.REQUEST_TIMEOUT, REPLY_TIMEOUT);

        System.out.println("Sending request to server.");

        short retries = 0;
        boolean success = false;

        while(!success && retries < MAX_RETRIES) {

            try {
                String[][] values = spreadsheets.importValues(sheetId, userId, range);
                System.out.println("Importing " + range);

                return values;
            } catch (SheetsException e) {
                System.out.println("Could not import range: " + range);
                success = true;
                return null; // TODO
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

        return null;
    }

    public static int deleteSpreadsheets(String serverUrl, String userId, String password) {
        System.out.println("Sending request to server.");

        SoapSpreadsheets spreadsheets = null;

        try {
            QName QNAME = new QName(SoapSpreadsheets.NAMESPACE, SoapSpreadsheets.NAME);
            Service service = Service.create( new URL(serverUrl + SPREADSHEETS_WSDL), QNAME);
            spreadsheets = service.getPort( tp1.api.service.soap.SoapSpreadsheets.class );
        } catch (WebServiceException | MalformedURLException e) {
            System.err.println("Could not contact the server: " + e.getMessage());
            System.exit(1);
        }

        //Set timeouts for executing operations
        ((BindingProvider) spreadsheets).getRequestContext().put(BindingProviderProperties.CONNECT_TIMEOUT, CONNECTION_TIMEOUT);
        ((BindingProvider) spreadsheets).getRequestContext().put(BindingProviderProperties.REQUEST_TIMEOUT, REPLY_TIMEOUT);

        System.out.println("Sending request to server.");

        short retries = 0;
        boolean success = false;

        while(!success && retries < MAX_RETRIES) {

            try {
                spreadsheets.deleteUserSpreadsheets(userId, password);
                System.out.println("Deleting " + userId + "'s spreadsheets.");
                success = true;
                return 200;
            } catch (SheetsException e) {
                System.out.println("Could not delete spreadsheets: " + e.getMessage());
                success = true;
                return 400; // TODO
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
}
