package gateway.notification;

import gateway.sender.MessageSender;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * REFINED ABSTRACTION: urgent message.
 * <ul>
 *   <li>adds the {@code [URGENT]} marker to the text;</li>
 *   <li>checks {@link MessageSender#isAvailable()} before using a provider;</li>
 *   <li>falls back to the next provider if the primary one is unavailable or refuses the message.</li>
 * </ul>
 * The fallback chain works ONLY through the {@link MessageSender} interface
 * (no {@code instanceof}, no knowledge of concrete providers).
 */
public class UrgentNotification extends Notification {

    public static final String PREFIX = "[URGENT] ";
    public static final String TITLE = "Urgent notification";

    private final List<MessageSender> fallbackSenders = new ArrayList<>();
    private String lastUsedChannel;
    private boolean lastFallbackUsed;

    public UrgentNotification(MessageSender primary) {
        super(primary);
    }

    public UrgentNotification(MessageSender primary, List<MessageSender> fallbacks) {
        super(primary);
        Objects.requireNonNull(fallbacks, "fallbacks").forEach(this::addFallback);
    }

    /** Adds another provider to the end of the fallback chain. */
    public void addFallback(MessageSender fallback) {
        fallbackSenders.add(Objects.requireNonNull(fallback, "fallback must not be null"));
    }

    public List<MessageSender> getFallbackSenders() {
        return List.copyOf(fallbackSenders);
    }

    /** @return name of the provider that delivered the last message, or {@code null} if it failed. */
    public String getLastUsedChannel() {
        return lastUsedChannel;
    }

    /** @return {@code true} if the last message went through a fallback instead of the primary provider. */
    public boolean wasFallbackUsed() {
        return lastFallbackUsed;
    }

    @Override
    public void send(String recipient, String message) {
        String body = PREFIX + message;

        // primary first, then the fallbacks in order (without repeating the same provider)
        List<MessageSender> chain = new ArrayList<>();
        chain.add(sender);
        for (MessageSender fallback : fallbackSenders) {
            if (!chain.contains(fallback)) {
                chain.add(fallback);
            }
        }

        List<String> tried = new ArrayList<>();
        for (int i = 0; i < chain.size(); i++) {
            MessageSender candidate = chain.get(i);
            tried.add(candidate.getChannelName());
            if (!candidate.isAvailable()) {
                continue;
            }
            if (candidate.sendMessage(recipient, TITLE, body)) {
                lastUsedChannel = candidate.getChannelName();
                lastFallbackUsed = i > 0;
                return;
            }
        }

        lastUsedChannel = null;
        lastFallbackUsed = false;
        throw new DeliveryFailedException(
                "Urgent notification to '" + recipient + "' failed, providers tried: " + String.join(", ", tried));
    }
}
