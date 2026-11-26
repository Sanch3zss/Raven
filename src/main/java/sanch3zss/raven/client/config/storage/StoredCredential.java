package sanch3zss.raven.client.config.storage;

public class StoredCredential {
    public String serverAddress;
    public String username;
    public String password;

    public StoredCredential() {
    }

    public StoredCredential(String serverAddress, String username, String password) {
        this.serverAddress = serverAddress;
        this.username = username;
        this.password = password;
    }
}
