package gateway.notification;

import gateway.sender.MessageSender;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * REFINED ABSTRACTION: one message to a whole collection of recipients, followed by a summary.
 *
 * <p>The {@code recipient} argument of {@link #send(String, String)} is the <b>audience label</b>
 * (e.g. "Crew of Module B"): it goes into the title. The real recipients are the ones the batch holds.</p>
 *
 * <p>"Scheduled" is simulated: the batch knows its planned time ({@link #isDue(LocalDateTime)}),
 * but there is no real scheduler - {@code send} dispatches immediately.</p>
 */
public class ScheduledBatchNotification extends Notification {

    private final List<String> recipients = new ArrayList<>();
    private final LocalDateTime scheduledFor;
    private BatchReport lastReport;

    public ScheduledBatchNotification(MessageSender sender, Collection<String> recipients) {
        this(sender, recipients, null);
    }

    public ScheduledBatchNotification(MessageSender sender, Collection<String> recipients,
                                      LocalDateTime scheduledFor) {
        super(sender);
        this.recipients.addAll(Objects.requireNonNull(recipients, "recipients"));
        this.scheduledFor = scheduledFor;
    }

    public void addRecipient(String recipient) {
        recipients.add(recipient);
    }

    public List<String> getRecipients() {
        return List.copyOf(recipients);
    }

    public LocalDateTime getScheduledFor() {
        return scheduledFor;
    }

    /** @return {@code true} if no time was set or the planned time has been reached. */
    public boolean isDue(LocalDateTime now) {
        return scheduledFor == null || !now.isBefore(scheduledFor);
    }

    public BatchReport getLastReport() {
        return lastReport;
    }

    @Override
    public void send(String audience, String message) {
        sendBatch(audience, message);
    }

    /** Sends to every stored recipient and returns (and remembers) the summary. */
    public BatchReport sendBatch(String audience, String message) {
        String title = "Scheduled batch: " + audience;
        List<String> failedRecipients = new ArrayList<>();
        int successful = 0;

        for (String recipient : recipients) {
            try {
                deliverOrThrow(recipient, title, message);
                successful++;
            } catch (DeliveryFailedException e) {
                failedRecipients.add(recipient);
            }
        }

        lastReport = new BatchReport(recipients.size(), successful, failedRecipients.size(), failedRecipients);
        return lastReport;
    }
}
