package tp1.impl.server.resourceAbstraction;

import jakarta.inject.Singleton;
import tp1.api.User;
import tp1.api.service.util.Result;
import tp1.api.service.util.Users;
import tp1.impl.server.rest.SpreadsheetServer;
import tp1.impl.util.Mediator;
import tp1.impl.util.discovery.Discovery;

import java.net.URI;
import java.util.*;
import java.util.logging.Logger;

@Singleton
public class UsersResource implements Users {


    private String domain;
    private final Map<String, User> users = new HashMap<>();


    private static final Logger Log = Logger.getLogger(UsersResource.class.getName());

    public UsersResource() {
    }

    public UsersResource(String domain) {
        this.domain = domain;
    }

    @Override
    public Result<String> createUser(User user) {
        Log.info("createUser : " + user);

        // Check if user is valid, if not return HTTP BAD_REQUEST (400)
        if (user.getUserId() == null || user.getPassword() == null || user.getFullName() == null
                || user.getEmail() == null) {
            Log.info("User object invalid.");
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }

        synchronized (users) {
            // Check if userId exists, if not return HTTP CONFLICT (409)
            if (this.users.containsKey(user.getUserId())) {
                Log.info("User already exists.");
                return Result.error(Result.ErrorCode.CONFLICT);
            }

            // Add the user to the map of users
            this.users.put(user.getUserId(), user);
        }

        return Result.ok(user.getUserId());
    }

    @Override
    public Result<User> getUser(String userId, String password) {
        Log.info("getUser : user = " + userId + "; pwd = " + password);

        // Check if user is valid, if not return HTTP BAD_REQUEST (400)
        if (userId == null) {
            Log.info("UserId or password null.");
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }

        User user;

        synchronized (users) {
            user = this.users.get(userId);
            // Check if user exists, if not return HTTP NOT_FOUND (404)
            if (user == null) {
                Log.info("User does not exist.");
                return Result.error(Result.ErrorCode.NOT_FOUND);
            }
            // Check if the password is correct, if not return HTTP FORBIDDEN (403)
            if (!user.getPassword().equals(password)) {
                Log.info("Password is incorrect.");
                return Result.error(Result.ErrorCode.FORBIDDEN);
            }
        }

        return Result.ok(user);
    }

    @Override
    public Result<User> updateUser(String userId, String password, User user) {
        Log.info("updateUser : user = " + userId + "; pwd = " + password + " ; user = " + user);

        // Check if data is valid, if not return HTTP BAD_REQUEST (400)
        if (userId == null || user == null) {
            Log.info("UserId, password or user object null.");
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }

        User tempUser;
        synchronized (users) {
            tempUser = this.users.get(userId);
            // Check if userId exists, if not return HTTP NOT_FOUND (404)
            if (tempUser == null) {
                Log.info("User doesn't exist.");
                return Result.error(Result.ErrorCode.NOT_FOUND);
            }


            // Check if the password is correct, if not return HTTP FORBIDDEN (403)
            if (!tempUser.getPassword().equals(password)) {
                Log.info("Password is incorrect.");
                return Result.error(Result.ErrorCode.FORBIDDEN);
            }

            if (user.getEmail() != null) tempUser.setEmail(user.getEmail());
            if (user.getPassword() != null) tempUser.setPassword(user.getPassword());
            if (user.getFullName() != null) tempUser.setFullName(user.getFullName());
        }

        return Result.ok(tempUser);
    }

    @Override
    public Result<User> deleteUser(String userId, String password) {
        Log.info("deleteUser : user = " + userId + "; pwd = " + password);

        // Check if data is valid, if not return HTTP CONFLICT (409)
        if (userId == null) {
            Log.info("UserId or password null.");
            return Result.error(Result.ErrorCode.CONFLICT);
        }

        User user;
        synchronized (users) {
            user = this.users.get(userId);
            // Check if userId exists, if not return HTTP NOT_FOUND (404)
            if (user == null) {
                Log.info("User doesn't exist.");
                return Result.error(Result.ErrorCode.NOT_FOUND);
            }
            // Check if the password is correct, if not return HTTP FORBIDDEN (403)
            if (!user.getPassword().equals(password)) {
                Log.info("Password is incorrect.");
                return Result.error(Result.ErrorCode.FORBIDDEN);
            }
            this.users.remove(userId);
        }

        // Asynchronously delete user's spreadsheets
        deleteSpreadsheets(userId, password);

        return Result.ok(user);
    }

    /**
     * Auxiliary method to call the endpoint which deletes the user's spreadsheets
     *
     * @param userId
     * @param password
     */
    private void deleteSpreadsheets(String userId, String password) {
        new Thread(() -> {
            String serviceName = this.domain + ":" + SpreadsheetServer.SERVICE;
            URI[] knownURIs = Discovery.getInstance().knownUrisOf(serviceName);

            Mediator.deleteSpreadsheets(knownURIs[0].toString(), userId, password);
        }).start();
    }

    @Override
    public Result<List<User>> searchUsers(String pattern) {
        Log.info("searchUsers : pattern = " + pattern);

        if (pattern == null) {
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }

        List<User> list;
        Set<Map.Entry<String, User>> map;
        synchronized (users) {
            map = this.users.entrySet();

            list = new LinkedList<>();

            for (Map.Entry<String, User> entry : map) {
                if (entry.getValue().getFullName().toLowerCase().contains(pattern.toLowerCase())) {
                    list.add(safeUser(entry.getValue()));
                }
            }
        }
        return Result.ok(list);
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
