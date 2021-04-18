package tp1.impl.server.resources;

import jakarta.inject.Singleton;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status;
import tp1.api.User;
import tp1.api.service.rest.RestUsers;
import tp1.impl.clients.Mediator;
import tp1.impl.discovery.Discovery;
import tp1.impl.server.SpreadsheetServer;
import tp1.impl.server.UsersServer;

import java.net.URI;
import java.util.*;
import java.util.logging.Logger;

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

        deleteSpreadsheets(userId, password);

        return user;
    }

    private void deleteSpreadsheets(String userId, String password) {
        new Thread(() -> {
            // TODO
            String serviceName = UsersServer.domain + ":" + SpreadsheetServer.SERVICE;
            URI[] knownURIs = Discovery.getInstance().knownUrisOf(serviceName);

            Mediator.deleteSpreadsheets(knownURIs[0].toString(), userId, password);
        }).start();
    }

    @Override
    public List<User> searchUsers(String pattern) {
        Log.info("searchUsers : pattern = " + pattern);

        if (pattern == null) {
        	throw new WebApplicationException(Status.NOT_FOUND);
		}
        List<User> list;
        Set<Map.Entry<String, User>> map;
        synchronized (this) {
            map = this.users.entrySet();

            list = new LinkedList<>();

            for (Map.Entry<String, User> entry : map) {
                if (entry.getValue().getFullName().toLowerCase().contains(pattern.toLowerCase())) {
                    list.add(safeUser(entry.getValue()));
                }
            }
        }
        return list;
    }

    /**
     * Gives a passwordless version of a user
     *
     * @param user - user whose safe version we want
     * @return User with empty password
     */
    private User safeUser(User user) {
        User safeUser = new User(user);
        safeUser.setPassword("");
        return safeUser;
    }
}
