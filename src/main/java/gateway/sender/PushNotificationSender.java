package gateway.sender;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Push (mobile notification) - the provider added AFTER the initial implementation
 * (Independent Extension, task section 6). It was added without touching a single Notification class.
 *
 * <p>Push has a short lock-screen body limit and keeps a per-device badge counter.</p>
 */
public class PushNotificationSender extends AbstractSimulatedSender {

    public static final int MAX_BODY_LENGTH = 256;

    private final Map<String, Integer> badges = new HashMap<>();

    public PushNotificationSender() {
        this(System.out);
    }

    public PushNotificationSender(PrintStream out) {
        super("Push", out);
    }

    @Override
    public boolean sendMessage(String recipient, String title, String body) {
        String problem = findCommonProblem(recipient, body);
        if (problem != null) {
            return reject(recipient, problem);
        }
        if (body.length() > MAX_BODY_LENGTH) {
            return reject(recipient, "push body is longer than " + MAX_BODY_LENGTH + " characters");
        }
        int badge = badges.merge(recipient, 1, Integer::sum);
        String rendered = "PUSH | " + nullToEmpty(title) + " | " + body + " | badge=" + badge;
        return accept(recipient, title, body, rendered);
    }
}
