package gateway;

import gateway.TestSupport.RecordingSender;
import gateway.notification.BatchReport;
import gateway.notification.DeliveryFailedException;
import gateway.notification.EncryptedNotification;
import gateway.notification.MarketingNotification;
import gateway.notification.Notification;
import gateway.notification.ScheduledBatchNotification;
import gateway.notification.UrgentNotification;
import gateway.sender.EmailMessageSender;
import gateway.sender.KafkaEventSender;
import gateway.sender.MessageSender;
import gateway.sender.PushNotificationSender;
import gateway.sender.SmsMessageSender;
import gateway.sender.TelegramMessageSender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static gateway.TestSupport.SILENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Independent Extension (task section 6): PushNotificationSender and MarketingNotification were added
 * later and work with the old code WITHOUT modifying it.
 */
class IndependentExtensionTest {

    static Stream<MessageSender> allProviders() {
        return Stream.of(
                new TelegramMessageSender(SILENT),
                new EmailMessageSender(SILENT),
                new SmsMessageSender(SILENT),
                new KafkaEventSender(SILENT),
                new PushNotificationSender(SILENT));
    }

    // ---------- new provider with old notifications ----------

    @Test
    @DisplayName("New provider: PushNotificationSender works with UrgentNotification")
    void pushWithUrgent() {
        PushNotificationSender push = new PushNotificationSender(SILENT);

        new UrgentNotification(push).send("device:1", "Evacuate");

        assertEquals(1, push.getSentCount());
        assertTrue(push.getSentMessages().get(0).rendered().contains("[URGENT] Evacuate"));
    }

    @Test
    @DisplayName("New provider: PushNotificationSender works with EncryptedNotification and Batch")
    void pushWithEncryptedAndBatch() {
        PushNotificationSender push = new PushNotificationSender(SILENT);

        new EncryptedNotification(push).send("device:1", "secret");
        BatchReport report = new ScheduledBatchNotification(push, List.of("device:1", "device:2"))
                .sendBatch("Crew", "Hello");

        assertEquals(3, push.getSentCount());
        assertEquals(2, report.successful());
    }

    @Test
    @DisplayName("New provider can be used as a fallback of UrgentNotification")
    void pushAsFallback() {
        TelegramMessageSender telegram = new TelegramMessageSender(SILENT);
        telegram.setAvailable(false);
        PushNotificationSender push = new PushNotificationSender(SILENT);
        UrgentNotification urgent = new UrgentNotification(telegram, List.of(push));

        urgent.send("device:1", "Help");

        assertEquals("Push", urgent.getLastUsedChannel());
    }

    @Test
    @DisplayName("Push body length limit is enforced")
    void pushLengthLimit() {
        PushNotificationSender push = new PushNotificationSender(SILENT);
        Notification marketing = new MarketingNotification(push, "Promo");

        assertThrows(DeliveryFailedException.class,
                () -> marketing.send("device:1", "x".repeat(PushNotificationSender.MAX_BODY_LENGTH)));
    }

    // ---------- new notification type with old providers ----------

    @Test
    @DisplayName("New type: MarketingNotification + PushNotificationSender")
    void marketingWithPush() {
        PushNotificationSender push = new PushNotificationSender(SILENT);

        new MarketingNotification(push, "Night Tour").send("device:1", "Book now");

        String rendered = push.getSentMessages().get(0).rendered();
        assertTrue(rendered.contains("Promo: Night Tour"));
        assertTrue(rendered.contains("[PROMO] Book now"));
        assertTrue(rendered.contains("Reply STOP to unsubscribe"));
    }

    @ParameterizedTest
    @MethodSource("allProviders")
    @DisplayName("New type: MarketingNotification works with EVERY existing provider, unmodified")
    void marketingWorksWithEveryProvider(MessageSender provider) {
        Notification marketing = new MarketingNotification(provider, "Night Tour");

        marketing.send("recipient", "Book now");

        assertEquals(1, ((gateway.sender.AbstractSimulatedSender) provider).getSentCount());
    }

    @ParameterizedTest
    @MethodSource("allProviders")
    @DisplayName("N x M: Urgent notification works with EVERY provider through the same interface")
    void urgentWorksWithEveryProvider(MessageSender provider) {
        Notification urgent = new UrgentNotification(provider);

        urgent.send("recipient", "Alert");

        assertEquals(1, ((gateway.sender.AbstractSimulatedSender) provider).getSentCount());
    }

    @Test
    @DisplayName("New type works with a provider that did not exist when it was written (test-only sender)")
    void marketingWithCustomProvider() {
        RecordingSender custom = new RecordingSender("Smoke signals");

        new MarketingNotification(custom, "Night Tour").send("everyone", "Book now");

        assertEquals(1, custom.bodies.size());
        assertTrue(custom.bodies.get(0).startsWith(MarketingNotification.PREFIX));
    }

    @Test
    @DisplayName("Failure: Marketing over an unavailable provider throws")
    void marketingFailsOnOutage() {
        PushNotificationSender push = new PushNotificationSender(SILENT);
        push.setAvailable(false);

        assertThrows(DeliveryFailedException.class,
                () -> new MarketingNotification(push, "Night Tour").send("device:1", "Book now"));
    }

    @Test
    @DisplayName("Marketing campaign name is kept and can be switched to another provider at runtime")
    void marketingRuntimeSwitch() {
        PushNotificationSender push = new PushNotificationSender(SILENT);
        EmailMessageSender email = new EmailMessageSender(SILENT);
        MarketingNotification marketing = new MarketingNotification(push, "Night Tour");

        marketing.send("device:1", "Book now");
        marketing.setSender(email);
        marketing.send("aliya", "Book now");

        assertEquals("Night Tour", marketing.getCampaign());
        assertEquals(1, push.getSentCount());
        assertEquals(1, email.getSentCount());
    }
}
