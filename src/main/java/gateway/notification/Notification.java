package gateway.notification;

import gateway.sender.MessageSender;

import java.util.Objects;

/**
 * ABSTRACTION of the Bridge pattern.
 *
 * <p>Describes <b>what</b> kind of notification is sent (urgent, encrypted, batch, marketing...).
 * It does not know <b>how</b> the message travels: that is the job of the {@link MessageSender}
 * it holds. The {@code sender} field is the BRIDGE between the two hierarchies.</p>
 */
public abstract class Notification {

    /** The Bridge: a reference from the abstraction to the implementor (has-a). */
    protected MessageSender sender;

    protected Notification(MessageSender sender) {
        this.sender = Objects.requireNonNull(sender, "sender must not be null");
    }

    /** Runtime provider switching: no change to any Notification class is needed. */
    public void setSender(MessageSender sender) {
        this.sender = Objects.requireNonNull(sender, "sender must not be null");
    }

    public MessageSender getSender() {
        return sender;
    }

    public abstract void send(String recipient, String message);

    /**
     * Sends through the current {@link #sender} and fails loudly if the provider refuses.
     *
     * @throws DeliveryFailedException when the provider returns {@code false}
     */
    protected void deliverOrThrow(String recipient, String title, String body) {
        if (!sender.sendMessage(recipient, title, body)) {
            throw new DeliveryFailedException(
                    "Delivery to '" + recipient + "' via " + sender.getChannelName() + " failed");
        }
    }
}
