package sanch3zss.raven.client.security;

import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class AesCipherUtil {
    public static final String algorithm = "AES";
    public static final String secretKey = "1Jh5pJ5NGgvXgKvI";

    public static String encrypt(String str) {
        if (str == null) {
            return null;
        }
        try {
            SecretKeySpec secretKeySpec = createKeySpec();
            Cipher cipher = Cipher.getInstance(algorithm);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            return Base64.getEncoder().encodeToString(cipher.doFinal(str.getBytes()));
        } catch (Exception e) {
            return str;
        }
    }

    public static String decrypt(String str) {
        if (str == null) {
            return null;
        }
        try {
            SecretKeySpec secretKeySpec = createKeySpec();
            Cipher cipher = Cipher.getInstance(algorithm);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
            return new String(cipher.doFinal(Base64.getDecoder().decode(str)));
        } catch (Exception e) {
            return str;
        }
    }

    public static SecretKeySpec createKeySpec() {
        return new SecretKeySpec(secretKey.getBytes(), algorithm);
    }
}
