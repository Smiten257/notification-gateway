package gateway.sender;

import java.io.PrintStream;

/**
 * E-mail: has a subject (the title is mandatory) and automatically appends a signature.
 */
public class EmailMessageSender extends AbstractSimulatedSender {

    public static final String DEFAULT_SIGNATURE = "Orbital Station Control | notifications@orbital-station.kz";

    private final String signature;

    public EmailMessageSender() {
        this(DEFAULT_SIGNATURE, System.out);
    }

    public EmailMessageSender(PrintStream out) {
        this(DEFAULT_SIGNATURE, out);
    }

    public EmailMessageSender(String signature, PrintStream out) {
        super("Email", out);
        this.signature = signature;
    }

    @Override
    public boolean sendMessage(String recipient, String title, String body) {
        String problem = findCommonProblem(recipient, body);
        if (problem != null) {
            return reject(recipient, problem);
        }
        if (title == null || title.isBlank()) {
            return reject(recipient, "e-mail needs a subject (title)");
        }
        String rendered = "To: " + recipient + "\n"
                + "Subject: " + title + "\n\n"
                + body + "\n\n"
                + "-- \n" + signature;
        return accept(recipient, title, body, rendered);
    }
}
