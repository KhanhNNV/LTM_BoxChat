package chatapp.service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.security.MessageDigest;

public class EncryptionService {

    private static final String MASTER_PASSWORD = "MySuperSecretKeyForChatApp12345";
    private static final String ALGORITHM = "AES";

    private final Cipher cipher;
    private final SecretKeySpec keySpec;

    public EncryptionService() throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] key = sha.digest(MASTER_PASSWORD.getBytes(StandardCharsets.UTF_8));
        this.keySpec = new SecretKeySpec(key, ALGORITHM);
        this.cipher = Cipher.getInstance(ALGORITHM);
    }

    public String encrypt(String data) {
        try {
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encryptedBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            System.err.println("Error encrypting data: " + e.getMessage());
            return null;
        }
    }

    public String decrypt(String encryptedData) {
        try {
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decodedBytes = Base64.getDecoder().decode(encryptedData);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("Error decrypting data: " + e.getMessage());
            return "[Encrypted Message - Decryption Failed]";
        }
    }
}

