package tp1.impl.server.rest.resources;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import tp1.api.User;
import tp1.api.service.rest.RestUsers;
import tp1.api.service.util.Result;
import tp1.impl.server.resourceAbstraction.UsersResource;

import java.util.List;

public class UsersRest implements RestUsers {

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
       return this.parseResult(resource.createUser(user));
    }

    @Override
    public User getUser(String userId, String password) throws WebApplicationException {
        return this.parseResult(resource.getUser(userId, password));
    }

    @Override
    public User updateUser(String userId, String password, User user) throws WebApplicationException {
        return this.parseResult(resource.updateUser(userId, password, user));
    }

    @Override
    public User deleteUser(String userId, String password) throws WebApplicationException {
        return this.parseResult(resource.deleteUser(userId, password));
    }

    @Override
    public List<User> searchUsers(String pattern) throws WebApplicationException {
        return this.parseResult(resource.searchUsers(pattern));
    }

}
