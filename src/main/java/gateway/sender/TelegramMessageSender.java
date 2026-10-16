package gateway.sender;

import java.io.PrintStream;

/**
 * Telegram: supports FORMATTED messages (the title is rendered in bold, Markdown style)
 * and has a large message limit.
 */
public class TelegramMessageSender extends AbstractSimulatedSender {

    public static final int MAX_LENGTH = 4096;

    public TelegramMessageSender() {
        this(System.out);
    }

    public TelegramMessageSender(PrintStream out) {
        super("Telegram", out);
    }

    @Override
    public boolean sendMessage(String recipient, String title, String body) {
        String problem = findCommonProblem(recipient, body);
        if (problem != null) {
            return reject(recipient, problem);
        }
        String safeTitle = nullToEmpty(title);
        String rendered = safeTitle.isBlank() ? body : "*" + safeTitle + "*\n" + body;
        if (rendered.length() > MAX_LENGTH) {
            return reject(recipient, "message is longer than " + MAX_LENGTH + " characters");
        }
        return accept(recipient, title, body, rendered);
    }
}
