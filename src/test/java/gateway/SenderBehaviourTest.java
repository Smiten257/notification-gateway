package gateway;

import gateway.sender.EmailMessageSender;
import gateway.sender.KafkaEventSender;
import gateway.sender.PushNotificationSender;
import gateway.sender.SentMessage;
import gateway.sender.SmsMessageSender;
import gateway.sender.TelegramMessageSender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static gateway.TestSupport.SILENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Each concrete Implementor must behave differently. */
class SenderBehaviourTest {

    @Test
    @DisplayName("Telegram renders the title as bold formatted text")
    void telegramFormatsTitle() {
        TelegramMessageSender telegram = new TelegramMessageSender(SILENT);

        assertTrue(telegram.sendMessage("Aigerim", "Alert", "Hello"));

        SentMessage sent = telegram.getSentMessages().get(0);
        assertEquals("*Alert*\nHello", sent.rendered());
    }

    @Test
    @DisplayName("Email has a subject and always appends the signature")
    void emailHasTitleAndSignature() {
        EmailMessageSender email = new EmailMessageSender("Station Control", SILENT);

        assertTrue(email.sendMessage("nurlan", "Docking", "Window moved"));

        String rendered = email.getSentMessages().get(0).rendered();
        assertTrue(rendered.contains("Subject: Docking"));
        assertTrue(rendered.contains("Window moved"));
        assertTrue(rendered.endsWith("Station Control"));
    }

    @Test
    @DisplayName("Email without a title (subject) is rejected")
    void emailRequiresSubject() {
        EmailMessageSender email = new EmailMessageSender(SILENT);

        assertFalse(email.sendMessage("nurlan", " ", "Body"));
        assertEquals(0, email.getSentCount());
    }

    @Test
    @DisplayName("SMS: exactly 160 characters are accepted, 161 are rejected")
    void smsLengthLimit() {
        SmsMessageSender sms = new SmsMessageSender(SILENT);

        assertTrue(sms.sendMessage("Aigerim", "ignored", "x".repeat(160)));
        assertFalse(sms.sendMessage("Aigerim", "ignored", "x".repeat(161)));
        assertEquals(1, sms.getSentCount());
    }

    @Test
    @DisplayName("SMS limit is configurable (e.g. 120 characters)")
    void smsCustomLimit() {
        SmsMessageSender sms = new SmsMessageSender(120, SILENT);

        assertFalse(sms.sendMessage("Aigerim", "t", "x".repeat(121)));
        assertTrue(sms.sendMessage("Aigerim", "t", "x".repeat(120)));
    }

    @Test
    @DisplayName("SMS sends only the body, the title is dropped")
    void smsDropsTitle() {
        SmsMessageSender sms = new SmsMessageSender(SILENT);

        sms.sendMessage("Aigerim", "Some title", "Short text");

        assertEquals("Short text", sms.getSentMessages().get(0).rendered());
    }

    @Test
    @DisplayName("Kafka publishes an event with a per-partition offset")
    void kafkaPublishesEvents() {
        KafkaEventSender kafka = new KafkaEventSender("alerts", SILENT);

        kafka.sendMessage("module-b", "t", "first");
        kafka.sendMessage("module-b", "t", "second");

        String first = kafka.getSentMessages().get(0).rendered();
        String second = kafka.getSentMessages().get(1).rendered();
        assertTrue(first.contains("\"topic\":\"alerts\""));
        assertTrue(first.contains("\"offset\":0"));
        assertTrue(second.contains("\"offset\":1"));   // same key -> same partition -> next offset
    }

    @Test
    @DisplayName("Push keeps a badge counter per device")
    void pushBadgeCounter() {
        PushNotificationSender push = new PushNotificationSender(SILENT);

        push.sendMessage("device:1", "t", "one");
        push.sendMessage("device:1", "t", "two");
        push.sendMessage("device:2", "t", "other");

        assertTrue(push.getSentMessages().get(1).rendered().endsWith("badge=2"));
        assertTrue(push.getSentMessages().get(2).rendered().endsWith("badge=1"));
    }

    @Test
    @DisplayName("Providers differ: the same input is rendered differently")
    void providersRenderDifferently() {
        TelegramMessageSender telegram = new TelegramMessageSender(SILENT);
        EmailMessageSender email = new EmailMessageSender(SILENT);
        SmsMessageSender sms = new SmsMessageSender(SILENT);

        telegram.sendMessage("a", "Title", "Body");
        email.sendMessage("a", "Title", "Body");
        sms.sendMessage("a", "Title", "Body");

        String t = telegram.getSentMessages().get(0).rendered();
        String e = email.getSentMessages().get(0).rendered();
        String s = sms.getSentMessages().get(0).rendered();
        assertNotEquals(t, e);
        assertNotEquals(t, s);
        assertNotEquals(e, s);
    }

    @Test
    @DisplayName("Unavailable provider refuses to send")
    void unavailableProviderRefuses() {
        TelegramMessageSender telegram = new TelegramMessageSender(SILENT);
        telegram.setAvailable(false);

        assertFalse(telegram.isAvailable());
        assertFalse(telegram.sendMessage("Aigerim", "t", "body"));
        assertEquals(0, telegram.getSentCount());
    }

    @Test
    @DisplayName("Empty recipient and empty body are refused")
    void invalidInputRefused() {
        SmsMessageSender sms = new SmsMessageSender(SILENT);

        assertFalse(sms.sendMessage("", "t", "body"));
        assertFalse(sms.sendMessage(null, "t", "body"));
        assertFalse(sms.sendMessage("Aigerim", "t", ""));
        assertFalse(sms.sendMessage("Aigerim", "t", null));
    }

    @Test
    @DisplayName("SMS limit must be positive")
    void smsLimitValidation() {
        assertThrows(IllegalArgumentException.class, () -> new SmsMessageSender(0, SILENT));
    }
}
