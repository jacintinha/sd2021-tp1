package tp1.impl.server.rest.resources;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import tp1.api.User;
import tp1.api.service.rest.RestUsers;
import tp1.api.service.util.Result;
import tp1.impl.server.resourceAbstraction.UsersResource;

import java.util.List;

public class UsersRest implements RestUsers {
//    private final Map<String, User> users = new HashMap<>();

//    private static final Logger Log = Logger.getLogger(UsersWS.class.getName());

    private UsersResource resource;

    public UsersRest() {
    }

    public UsersRest(String domain) {
        this.resource = new UsersResource(domain);
    }

    private <T> T parseResult(Result<T> result) throws WebApplicationException {
        if (result.isOK()) {
            return result.value();
        }
        throw new WebApplicationException(Response.Status.valueOf(result.error().name()));
    }

    @Override
    public String createUser(User user) throws WebApplicationException {
//        Log.info("createUser : " + user);
//
//        // Check if user is valid, if not return HTTP BAD_REQUEST (400)
//        if (user.getUserId() == null || user.getPassword() == null || user.getFullName() == null
//                || user.getEmail() == null) {
//            Log.info("User object invalid.");
//            throw new UsersException("User object invalid.");
//        }
//
//        synchronized (this) {
//            // Check if userId exists, if not return HTTP CONFLICT (409)
//            if (this.users.containsKey(user.getUserId())) {
//                Log.info("User already exists.");
//                throw new UsersException("User already exists.");
//            }
//
//            // Add the user to the map of users
//            this.users.put(user.getUserId(), user);
//        }
//
//        return user.getUserId();
       return this.parseResult(resource.createUser(user));
    }

    @Override
    public User getUser(String userId, String password) throws WebApplicationException {
//        Log.info("getUser : user = " + userId + "; pwd = " + password);
//
//        // Check if user is valid, if not return HTTP BAD_REQUEST (400)
//        if (userId == null /*|| password == null*/) {
//            Log.info("UserId or password null.");
//            throw new UsersException("UserId or password null.");
//        }
//
//        User user;
//
//        synchronized (this) {
//            user = this.users.get(userId);
//            // Check if user exists, if not return HTTP NOT_FOUND (404)
//            if (user == null) {
//                Log.info("User does not exist.");
//                throw new UsersException("404");
//            }
//            // Check if the password is correct, if not return HTTP FORBIDDEN (403)
//            if (!user.getPassword().equals(password)) {
//                Log.info("Password is incorrect.");
//                throw new UsersException("403");
//            }
//        }
//
//        return user;
        return this.parseResult(resource.getUser(userId, password));
    }

    @Override
    public User updateUser(String userId, String password, User user) throws WebApplicationException {
//        Log.info("updateUser : user = " + userId + "; pwd = " + password + " ; user = " + user);
//
//        // Check if data is valid, if not return HTTP BAD_REQUEST (400)
//        if (userId == null || /*password == null ||*/ user == null) {
//            Log.info("UserId, password or user object null.");
//            throw new UsersException("UserId, password or user object null.");
//        }
//
//        User tempUser;
//        synchronized (this) {
//            tempUser = this.users.get(userId);
//            // Check if userId exists, if not return HTTP NOT_FOUND (404)
//            if (tempUser == null) {
//                Log.info("User doesn't exist.");
//                throw new UsersException("User doesn't exist.");
//            }
//
//            // Check if the password is correct, if not return HTTP FORBIDDEN (403)
//            if (!tempUser.getPassword().equals(password)) {
//                Log.info("Password is incorrect.");
//                throw new UsersException("Password is incorrect.");
//            }
//
//            // TODO refactor?
//            if (user.getEmail() != null) tempUser.setEmail(user.getEmail());
//            if (user.getPassword() != null) tempUser.setPassword(user.getPassword());
//            if (user.getFullName() != null) tempUser.setFullName(user.getFullName());
//        }
//
//        return tempUser;
        return this.parseResult(resource.updateUser(userId, password, user));
    }

    @Override
    public User deleteUser(String userId, String password) throws WebApplicationException {
//        Log.info("deleteUser : user = " + userId + "; pwd = " + password);
//
//        // Check if data is valid, if not return HTTP CONFLICT (409)
//        if (userId == null /*|| password == null*/) {
//            Log.info("UserId or password null.");
//            throw new UsersException("UserId or password null.");
//        }
//
//        User user;
//
//        synchronized (this) {
//            user = this.users.get(userId);
//            // Check if userId exists, if not return HTTP NOT_FOUND (404)
//            if (user == null) {
//                Log.info("User doesn't exist.");
//                throw new UsersException("User doesn't exist.");
//            }
//            // Check if the password is correct, if not return HTTP FORBIDDEN (403)
//            if (!user.getPassword().equals(password)) {
//                Log.info("Password is incorrect.");
//                throw new UsersException("Password is incorrect.");
//            }
//            this.users.remove(userId);
//        }
//
//        deleteSpreadsheets(userId, password);
//
//        return user;
        return this.parseResult(resource.deleteUser(userId, password));
    }

//    private void deleteSpreadsheets(String userId, String password) {
//        new Thread(() -> {
//            // TODO
//            String serviceName = UsersServer.domain + ":" + SpreadsheetServer.SERVICE;
//            URI[] knownURIs = Discovery.getInstance().knownUrisOf(serviceName);
//
//            MediatorSoap.deleteSpreadsheets(knownURIs[0].toString(), userId, password);
//        }).start();
//    }

    @Override
    public List<User> searchUsers(String pattern) throws WebApplicationException {
//        Log.info("searchUsers : pattern = " + pattern);
//
//        if (pattern == null) {
//            throw new UsersException("Invalid pattern.");
//        }
//        List<User> list;
//        Set<Map.Entry<String, User>> map;
//        synchronized (this) {
//            map = this.users.entrySet();
//
//            list = new LinkedList<>();
//
//            for (Map.Entry<String, User> entry : map) {
//                if (entry.getValue().getFullName().toLowerCase().contains(pattern.toLowerCase())) {
//                    list.add(safeUser(entry.getValue()));
//                }
//            }
//        }
//        return list;
        return this.parseResult(resource.searchUsers(pattern));
    }

//    /**
//     * Gives a passwordless version of a user
//     *
//     * @param user - user whose safe version we want
//     * @return User with empty password
//     */
//    private User safeUser(User user) {
//        User safeUser = new User(user);
//        safeUser.setPassword("");
//        return safeUser;
//    }
}
