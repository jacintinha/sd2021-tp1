package tp1.impl.server.resources;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import jakarta.inject.Singleton;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status;
import tp1.api.User;
import tp1.api.service.rest.RestUsers;

@Singleton
public class UsersResource implements RestUsers {

	private final Map<String, User> users = new HashMap<>();

	private static final Logger Log = Logger.getLogger(UsersResource.class.getName());

	public UsersResource() {
	}

	@Override
	public String createUser(User user) {
		Log.info("createUser : " + user);

		// Check if user is valid, if not return HTTP BAD_REQUEST (400)
		if (user.getUserId() == null || user.getPassword() == null || user.getFullName() == null
				|| user.getEmail() == null) {
			Log.info("User object invalid.");
			throw new WebApplicationException(Status.BAD_REQUEST);
		}

		synchronized (this) {
			// Check if userId exists, if not return HTTP CONFLICT (409)
			if (this.users.containsKey(user.getUserId())) {
				Log.info("User already exists.");
				throw new WebApplicationException(Status.CONFLICT);
			}

			// Add the user to the map of users
			this.users.put(user.getUserId(), user);
		}

		return user.getUserId();
	}

	@Override
	public User getUser(String userId, String password) {
		Log.info("getUser : user = " + userId + "; pwd = " + password);

		// Check if user is valid, if not return HTTP BAD_REQUEST (400)
		if (userId == null /*|| password == null*/) {
			Log.info("UserId or password null.");
			throw new WebApplicationException(Status.BAD_REQUEST);
		}

		User user;

		synchronized (this) {
			user = this.users.get(userId);
			// Check if user exists, if not return HTTP NOT_FOUND (404)
			if (user == null) {
				Log.info("User does not exist.");
				throw new WebApplicationException(Status.NOT_FOUND);
			}
			// Check if the password is correct, if not return HTTP FORBIDDEN (403)
			if (!user.getPassword().equals(password)) {
				Log.info("Password is incorrect.");
				throw new WebApplicationException(Status.FORBIDDEN);
			}
		}

		return user;
	}

	@Override
	public User updateUser(String userId, String password, User user) {
		Log.info("updateUser : user = " + userId + "; pwd = " + password + " ; user = " + user);

		// Check if data is valid, if not return HTTP BAD_REQUEST (400)
		if (userId == null || /*password == null ||*/ user == null) {
			Log.info("UserId, password or user object null.");
			throw new WebApplicationException(Status.BAD_REQUEST);
		}

		User tempUser;
		synchronized (this) {
			tempUser = this.users.get(userId);
			// Check if userId exists, if not return HTTP NOT_FOUND (404)
			if (tempUser == null) {
				Log.info("User doesn't exist.");
				throw new WebApplicationException(Status.NOT_FOUND);
			}

			// Check if the password is correct, if not return HTTP FORBIDDEN (403)
			if (!tempUser.getPassword().equals(password)) {
				Log.info("Password is incorrect.");
				throw new WebApplicationException(Status.FORBIDDEN);
			}

			// TODO refactor?
			if (user.getEmail() != null) tempUser.setEmail(user.getEmail());
			if (user.getPassword() != null) tempUser.setPassword(user.getPassword());
			if (user.getFullName() != null) tempUser.setFullName(user.getFullName());
		}

		return tempUser;
	}

	@Override
	public User deleteUser(String userId, String password) {
		Log.info("deleteUser : user = " + userId + "; pwd = " + password);

		// Check if data is valid, if not return HTTP CONFLICT (409)
		if (userId == null /*|| password == null*/) {
			Log.info("UserId or password null.");
			throw new WebApplicationException(Status.CONFLICT);
		}

		User user;

		synchronized (this) {
			user = this.users.get(userId);
			// Check if userId exists, if not return HTTP NOT_FOUND (404)
			if (user == null) {
				Log.info("User doesn't exist.");
				throw new WebApplicationException(Status.NOT_FOUND);
			}
			// Check if the password is correct, if not return HTTP FORBIDDEN (403)
			if (!user.getPassword().equals(password)) {
				Log.info("Password is incorrect.");
				throw new WebApplicationException(Status.FORBIDDEN);
			}
			this.users.remove(userId);
		}
		return user;
	}

	@Override
	public List<User> searchUsers(String pattern) {
		Log.info("searchUsers : pattern = " + pattern);

		// TODO synch?

		if (pattern == null) {
			return new LinkedList<User>(this.users.values());
		} else {
			List<User> list = new LinkedList<User>();

			for (Map.Entry<String, User> entry : this.users.entrySet()) {
				if (entry.getValue().getFullName().toLowerCase().contains(pattern.toLowerCase())) {
					list.add(safeUser(entry.getValue()));
				}
			}

			return list;
		}
	}
	
	public User safeUser(User user) {
		User safeUser = new User(user);
		safeUser.setPassword("");
		return safeUser;
	}

}
