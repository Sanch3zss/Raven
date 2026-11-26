package sanch3zss.raven.client.config.serialization;

import java.util.List;

import com.google.gson.reflect.TypeToken;

import sanch3zss.raven.client.config.storage.*;

public class CredentialListTypeToken extends TypeToken<List<StoredCredential>> {
    public CredentialListTypeToken(CredentialStore credentialStore) {
    }
}
