package gateway.notification;

import java.util.List;

/**
 * Summary of one batch run.
 *
 * @param total             number of recipients
 * @param successful        deliveries accepted by the provider
 * @param failed            deliveries refused by the provider
 * @param failedRecipients  who did not get the message
 */
public record BatchReport(int total, int successful, int failed, List<String> failedRecipients) {

    public BatchReport {
        failedRecipients = List.copyOf(failedRecipients);
    }

    @Override
    public String toString() {
        return "Total: " + total + "\nSuccessful: " + successful + "\nFailed: " + failed;
    }
}
