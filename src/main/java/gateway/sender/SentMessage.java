package gateway.sender;

/**
 * One message accepted by a simulated provider.
 *
 * @param recipient who the message was addressed to
 * @param title     title as passed by the notification
 * @param body      body exactly as the provider received it
 * @param rendered  how the provider actually "put it on the wire" (formatting, signature, JSON...)
 */
public record SentMessage(String recipient, String title, String body, String rendered) {
}
