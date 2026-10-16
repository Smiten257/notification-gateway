package gateway.sender;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Small helper shared by the simulated providers (code reuse only - it is NOT part of the pattern).
 *
 * <p>It keeps the availability flag, a set of "unreachable" recipients and a log of everything
 * the provider accepted. Real external APIs are not used: every provider is a simulation.</p>
 */
public abstract class AbstractSimulatedSender implements MessageSender {

    private final String channelName;
    private final PrintStream out;
    private final List<SentMessage> sent = new ArrayList<>();
    private final Set<String> unreachable = new HashSet<>();
    private boolean available = true;

    protected AbstractSimulatedSender(String channelName, PrintStream out) {
        this.channelName = Objects.requireNonNull(channelName, "channelName");
        this.out = Objects.requireNonNull(out, "out");
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public String getChannelName() {
        return channelName;
    }

    /** Simulates an outage (false) or recovery (true) of the provider. */
    public void setAvailable(boolean available) {
        this.available = available;
    }

    /** Simulates a recipient who cannot be reached through this channel (switched-off phone, bounced mailbox...). */
    public void markUnreachable(String recipient) {
        unreachable.add(recipient);
    }

    /** @return every message this provider has accepted so far (oldest first). */
    public List<SentMessage> getSentMessages() {
        return List.copyOf(sent);
    }

    public int getSentCount() {
        return sent.size();
    }

    /**
     * Checks that apply to every channel.
     *
     * @return a human-readable problem, or {@code null} if the message may be sent
     */
    protected String findCommonProblem(String recipient, String body) {
        if (!available) {
            return "provider is unavailable";
        }
        if (recipient == null || recipient.isBlank()) {
            return "recipient is empty";
        }
        if (body == null || body.isBlank()) {
            return "message body is empty";
        }
        if (unreachable.contains(recipient)) {
            return "recipient is unreachable on this channel";
        }
        return null;
    }

    /** Registers a refusal and returns {@code false}. */
    protected boolean reject(String recipient, String reason) {
        out.printf("  [%s] REJECTED for %s: %s%n", channelName, recipient, reason);
        return false;
    }

    /** Registers a successful delivery and returns {@code true}. */
    protected boolean accept(String recipient, String title, String body, String rendered) {
        sent.add(new SentMessage(recipient, title, body, rendered));
        out.printf("  [%s] DELIVERED to %s%n", channelName, recipient);
        for (String line : rendered.split("\n", -1)) {
            out.println("        " + line);
        }
        return true;
    }

    protected static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
