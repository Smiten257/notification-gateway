package gateway.notification;

import gateway.sender.MessageSender;

import java.util.Objects;

/**
 * REFINED ABSTRACTION added AFTER the initial implementation (Independent Extension, task section 6).
 * It works with every existing {@link MessageSender} without modifying any of them.
 */
public class MarketingNotification extends Notification {

    public static final String PREFIX = "[PROMO] ";
    public static final String OPT_OUT = " (Reply STOP to unsubscribe)";

    private final String campaign;

    public MarketingNotification(MessageSender sender, String campaign) {
        super(sender);
        this.campaign = Objects.requireNonNull(campaign, "campaign");
    }

    public String getCampaign() {
        return campaign;
    }

    @Override
    public void send(String recipient, String message) {
        String title = "Promo: " + campaign;
        String body = PREFIX + message + OPT_OUT;
        deliverOrThrow(recipient, title, body);
    }
}
