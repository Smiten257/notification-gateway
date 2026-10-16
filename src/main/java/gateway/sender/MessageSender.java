package gateway.sender;

/**
 * IMPLEMENTOR of the Bridge pattern.
 *
 * <p>Describes <b>how</b> a message is physically delivered (Telegram, e-mail, SMS, Kafka, push...).
 * The notification classes (the Abstraction side) only know this interface and never
 * the concrete providers, so both sides can evolve independently.</p>
 */
public interface MessageSender {

    /**
     * Tries to deliver one message.
     *
     * @return {@code true} if the provider accepted the message, {@code false} otherwise
     */
    boolean sendMessage(String recipient, String title, String body);

    /** @return {@code true} if the provider is currently able to deliver messages. */
    boolean isAvailable();

    /** @return human-readable provider name, e.g. "Telegram". */
    String getChannelName();
}
