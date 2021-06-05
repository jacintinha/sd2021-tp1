package tp1.impl.serialization;

public class DeleteUserSpreadsheetOperation {

    String userId, secret;

    public DeleteUserSpreadsheetOperation() {
    }

    public DeleteUserSpreadsheetOperation(String userId, String secret) {
        this.userId = userId;
        this.secret = secret;
    }

    public String getUserId() {
        return this.userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getSecret() {
        return this.secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }
}
