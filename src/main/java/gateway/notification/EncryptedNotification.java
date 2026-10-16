package gateway.notification;

import gateway.sender.MessageSender;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

/**
 * REFINED ABSTRACTION: the message is transformed before it is handed to the provider.
 *
 * <p>Demo "encryption" = XOR with a repeating key, then Base64. This is NOT real cryptography
 * (the assignment does not require it); it only shows that the abstraction may transform the
 * message while the provider stays unaware of it.</p>
 */
public class EncryptedNotification extends Notification {

    public static final String DEFAULT_KEY = "orbit";
    public static final String TITLE = "Encrypted notification";

    private final byte[] key;

    public EncryptedNotification(MessageSender sender) {
        this(sender, DEFAULT_KEY);
    }

    public EncryptedNotification(MessageSender sender, String key) {
        super(sender);
        Objects.requireNonNull(key, "key");
        if (key.isEmpty()) {
            throw new IllegalArgumentException("key must not be empty");
        }
        this.key = key.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void send(String recipient, String message) {
        deliverOrThrow(recipient, TITLE, encrypt(message));
    }

    public String encrypt(String plainText) {
        byte[] data = xor(plainText.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(data);
    }

    public String decrypt(String cipherText) {
        byte[] data = xor(Base64.getDecoder().decode(cipherText));
        return new String(data, StandardCharsets.UTF_8);
    }

    private byte[] xor(byte[] input) {
        byte[] output = new byte[input.length];
        for (int i = 0; i < input.length; i++) {
            output[i] = (byte) (input[i] ^ key[i % key.length]);
        }
        return output;
    }
}
