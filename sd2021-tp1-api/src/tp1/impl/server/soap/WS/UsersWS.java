package tp1.impl.server.soap.WS;

import jakarta.jws.WebService;
import tp1.api.User;
import tp1.api.service.soap.SoapUsers;
import tp1.api.service.soap.UsersException;
import tp1.api.service.util.Result;
import tp1.impl.server.resourceAbstraction.UsersResource;

import java.util.List;

@WebService(serviceName = SoapUsers.NAME,
        targetNamespace = SoapUsers.NAMESPACE,
        endpointInterface = SoapUsers.INTERFACE)
public class UsersWS implements SoapUsers {

    private UsersResource resource;

    public UsersWS() {
    }

    public UsersWS(String domain) {
        this.resource = new UsersResource(domain);
    }

    private <T> T parseResult(Result<T> result) throws UsersException {
        if (result.isOK()) {
            return result.value();
        }
        throw new UsersException(result.error().name());
    }

    @Override
    public String createUser(User user) throws UsersException {
        return this.parseResult(this.resource.createUser(user));
    }

    @Override
    public User getUser(String userId, String password) throws UsersException {
        return this.parseResult(this.resource.getUser(userId, password));
    }

    @Override
    public User updateUser(String userId, String password, User user) throws UsersException {
        return this.parseResult(this.resource.updateUser(userId, password, user));
    }

    @Override
    public User deleteUser(String userId, String password) throws UsersException {
        return this.parseResult(this.resource.deleteUser(userId, password));
    }

    @Override
    public List<User> searchUsers(String pattern) throws UsersException {
        return this.parseResult(this.resource.searchUsers(pattern));
    }
}
