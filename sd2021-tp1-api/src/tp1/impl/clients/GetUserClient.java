package tp1.impl.clients;

import java.io.IOException;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import tp1.api.User;
import tp1.api.service.rest.RestUsers;

public class GetUserClient {

	public final static int MAX_RETRIES = 3;
	public final static long RETRY_PERIOD = 1000;
	public final static int CONNECTION_TIMEOUT = 1000;
	public final static int REPLY_TIMEOUT = 600;

	public static int getUser(String serverUrl, String userId, String password) throws IOException {
//		serverUrl = "http://172.22.0.3:8080/rest";
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

				// TODO!!!!!!!!

				if (r.getStatus() == Status.OK.getStatusCode() && r.hasEntity()) {
//					System.out.println("Success:");
//					User u = r.readEntity(User.class);
//					System.out.println("User : " + u);
					return r.getStatus();
				} else
					System.out.println("Error, HTTP error status: " + r.getStatus());

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
