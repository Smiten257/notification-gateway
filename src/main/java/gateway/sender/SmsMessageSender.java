package gateway.sender;

import java.io.PrintStream;

/**
 * SMS: plain text only (no title) and a HARD length limit - 160 characters by default.
 * A longer message is rejected, not truncated.
 */
public class SmsMessageSender extends AbstractSimulatedSender {

    public static final int DEFAULT_MAX_LENGTH = 160;

    private final int maxLength;

    public SmsMessageSender() {
        this(DEFAULT_MAX_LENGTH, System.out);
    }

    public SmsMessageSender(PrintStream out) {
        this(DEFAULT_MAX_LENGTH, out);
    }

    public SmsMessageSender(int maxLength, PrintStream out) {
        super("SMS", out);
        if (maxLength <= 0) {
            throw new IllegalArgumentException("maxLength must be positive");
        }
        this.maxLength = maxLength;
    }

    public int getMaxLength() {
        return maxLength;
    }

    @Override
    public boolean sendMessage(String recipient, String title, String body) {
        String problem = findCommonProblem(recipient, body);
        if (problem != null) {
            return reject(recipient, problem);
        }
        if (body.length() > maxLength) {
            return reject(recipient, "text has " + body.length() + " characters, SMS limit is " + maxLength);
        }
        // SMS has no title: only the body is transmitted.
        return accept(recipient, title, body, body);
    }
}
