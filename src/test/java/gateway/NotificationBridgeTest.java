package gateway;

import gateway.TestSupport.RecordingSender;
import gateway.notification.BatchReport;
import gateway.notification.DeliveryFailedException;
import gateway.notification.EncryptedNotification;
import gateway.notification.Notification;
import gateway.notification.ScheduledBatchNotification;
import gateway.notification.UrgentNotification;
import gateway.sender.EmailMessageSender;
import gateway.sender.SmsMessageSender;
import gateway.sender.TelegramMessageSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static gateway.TestSupport.SILENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Abstraction side: Urgent / Encrypted / ScheduledBatch working through the Bridge. */
class NotificationBridgeTest {

    private TelegramMessageSender telegram;
    private EmailMessageSender email;
    private SmsMessageSender sms;

    @BeforeEach
    void setUp() {
        telegram = new TelegramMessageSender(SILENT);
        email = new EmailMessageSender(SILENT);
        sms = new SmsMessageSender(SILENT);
    }

    // ---------- successful delivery ----------

    @Test
    @DisplayName("Urgent + Telegram: message gets the [URGENT] marker and is delivered")
    void urgentDeliversWithMarker() {
        Notification urgent = new UrgentNotification(telegram);

        urgent.send("Aigerim", "Evacuate Module B");

        assertEquals(1, telegram.getSentCount());
        assertEquals("[URGENT] Evacuate Module B", telegram.getSentMessages().get(0).body());
    }

    // ---------- unavailable provider / failure ----------

    @Test
    @DisplayName("Failure: Urgent without any working provider throws DeliveryFailedException")
    void urgentFailsWhenNothingAvailable() {
        telegram.setAvailable(false);
        UrgentNotification urgent = new UrgentNotification(telegram);

        assertThrows(DeliveryFailedException.class, () -> urgent.send("Aigerim", "Help"));
        assertNull(urgent.getLastUsedChannel());
        assertEquals(0, telegram.getSentCount());
    }

    @Test
    @DisplayName("Failure: all providers in the fallback chain are unavailable")
    void urgentFailsWhenWholeChainDown() {
        telegram.setAvailable(false);
        sms.setAvailable(false);
        UrgentNotification urgent = new UrgentNotification(telegram, List.of(sms));

        DeliveryFailedException ex =
                assertThrows(DeliveryFailedException.class, () -> urgent.send("Aigerim", "Help"));
        assertTrue(ex.getMessage().contains("Telegram"));
        assertTrue(ex.getMessage().contains("SMS"));
    }

    @Test
    @DisplayName("Non-urgent notification throws when the provider refuses the message")
    void encryptedFailsWhenProviderRefuses() {
        telegram.setAvailable(false);
        Notification encrypted = new EncryptedNotification(telegram);

        assertThrows(DeliveryFailedException.class, () -> encrypted.send("Aigerim", "secret"));
    }

    @Test
    @DisplayName("Constructor rejects a null provider")
    void nullSenderRejected() {
        assertThrows(NullPointerException.class, () -> new UrgentNotification(null));
        Notification urgent = new UrgentNotification(telegram);
        assertThrows(NullPointerException.class, () -> urgent.setSender(null));
    }

    // ---------- fallback ----------

    @Test
    @DisplayName("Fallback: Telegram unavailable -> message goes through SMS")
    void urgentFallsBackToSms() {
        telegram.setAvailable(false);
        UrgentNotification urgent = new UrgentNotification(telegram, List.of(sms));

        urgent.send("Timur", "Coolant leak");

        assertEquals(0, telegram.getSentCount());
        assertEquals(1, sms.getSentCount());
        assertEquals("SMS", urgent.getLastUsedChannel());
        assertTrue(urgent.wasFallbackUsed());
    }

    @Test
    @DisplayName("No fallback is used while the primary provider works")
    void primaryUsedWhenAvailable() {
        UrgentNotification urgent = new UrgentNotification(telegram, List.of(sms));

        urgent.send("Timur", "Coolant leak");

        assertEquals(1, telegram.getSentCount());
        assertEquals(0, sms.getSentCount());
        assertFalse(urgent.wasFallbackUsed());
    }

    @Test
    @DisplayName("Fallback chain: first AVAILABLE fallback wins, the rest stay untouched")
    void firstAvailableFallbackWins() {
        telegram.setAvailable(false);
        sms.setAvailable(false);
        UrgentNotification urgent = new UrgentNotification(telegram);
        urgent.addFallback(sms);
        urgent.addFallback(email);

        urgent.send("Timur", "Coolant leak");

        assertEquals("Email", urgent.getLastUsedChannel());
        assertEquals(1, email.getSentCount());
    }

    @Test
    @DisplayName("Fallback also works when the provider is available but REJECTS the message (SMS too long)")
    void fallbackWhenPrimaryRejects() {
        SmsMessageSender tinySms = new SmsMessageSender(20, SILENT);
        UrgentNotification urgent = new UrgentNotification(tinySms, List.of(email));

        urgent.send("Timur", "This message is definitely longer than twenty characters");

        assertEquals(0, tinySms.getSentCount());
        assertEquals(1, email.getSentCount());
        assertTrue(urgent.wasFallbackUsed());
    }

    @Test
    @DisplayName("Fallback depends only on the MessageSender interface (works with a test-only provider)")
    void fallbackWorksWithAnyImplementor() {
        telegram.setAvailable(false);
        RecordingSender custom = new RecordingSender("Carrier pigeon");
        UrgentNotification urgent = new UrgentNotification(telegram, List.of(custom));

        urgent.send("Timur", "Coolant leak");

        assertEquals(List.of("[URGENT] Coolant leak"), custom.bodies);
        assertEquals("Carrier pigeon", urgent.getLastUsedChannel());
    }

    @Test
    @DisplayName("Primary provider listed again as a fallback is not retried")
    void duplicateFallbackIgnored() {
        SmsMessageSender strict = new SmsMessageSender(5, SILENT);
        UrgentNotification urgent = new UrgentNotification(strict, List.of(strict));

        assertThrows(DeliveryFailedException.class, () -> urgent.send("Timur", "too long for this SMS"));
    }

    // ---------- SMS length restriction through a notification ----------

    @Test
    @DisplayName("SMS length limit applies to the final text, including the [URGENT] prefix")
    void smsLimitIncludesPrefix() {
        UrgentNotification urgent = new UrgentNotification(sms);
        String message = "x".repeat(160 - UrgentNotification.PREFIX.length());

        urgent.send("Aigerim", message);                   // exactly 160 -> OK
        assertEquals(1, sms.getSentCount());

        assertThrows(DeliveryFailedException.class,
                () -> urgent.send("Aigerim", message + "!"));   // 161 -> rejected
    }

    // ---------- encrypted ----------

    @Test
    @DisplayName("Encrypted: provider receives ciphertext, receiver can decrypt it")
    void encryptedMessageIsTransformed() {
        EncryptedNotification encrypted = new EncryptedNotification(email);
        String secret = "Access code: AURORA-7";

        encrypted.send("Nurlan", secret);

        String onTheWire = email.getSentMessages().get(0).body();
        assertNotEquals(secret, onTheWire);
        assertFalse(onTheWire.contains("AURORA"));
        assertEquals(secret, encrypted.decrypt(onTheWire));
    }

    @Test
    @DisplayName("Encrypted: a different key gives a different ciphertext")
    void encryptionDependsOnKey() {
        EncryptedNotification a = new EncryptedNotification(email, "key-A");
        EncryptedNotification b = new EncryptedNotification(email, "key-B");

        assertNotEquals(a.encrypt("same text"), b.encrypt("same text"));
        assertThrows(IllegalArgumentException.class, () -> new EncryptedNotification(email, ""));
    }

    @Test
    @DisplayName("Encrypted: non-ASCII text survives the round trip")
    void encryptionHandlesUnicode() {
        EncryptedNotification encrypted = new EncryptedNotification(email);
        String text = "Сәлем, Астана! Hello 123";

        assertEquals(text, encrypted.decrypt(encrypted.encrypt(text)));
    }

    // ---------- batch ----------

    @Test
    @DisplayName("Batch: 10 recipients, 2 unreachable -> Total 10 / Successful 8 / Failed 2")
    void batchSummary() {
        sms.markUnreachable("r3");
        sms.markUnreachable("r7");
        List<String> recipients = List.of("r1", "r2", "r3", "r4", "r5", "r6", "r7", "r8", "r9", "r10");
        ScheduledBatchNotification batch = new ScheduledBatchNotification(sms, recipients);

        BatchReport report = batch.sendBatch("Crew", "Storm warning");

        assertEquals(10, report.total());
        assertEquals(8, report.successful());
        assertEquals(2, report.failed());
        assertEquals(List.of("r3", "r7"), report.failedRecipients());
        assertEquals("Total: 10\nSuccessful: 8\nFailed: 2", report.toString());
        assertSame(report, batch.getLastReport());
        assertEquals(8, sms.getSentCount());
    }

    @Test
    @DisplayName("Batch: if the provider is down every delivery fails but nothing is thrown")
    void batchWithUnavailableProvider() {
        sms.setAvailable(false);
        ScheduledBatchNotification batch = new ScheduledBatchNotification(sms, List.of("a", "b", "c"));

        BatchReport report = batch.sendBatch("Crew", "Hello");

        assertEquals(3, report.total());
        assertEquals(0, report.successful());
        assertEquals(3, report.failed());
    }

    @Test
    @DisplayName("Batch: send(...) from the Notification contract also produces the report")
    void batchViaAbstractSend() {
        Notification batch = new ScheduledBatchNotification(email, List.of("a", "b"));

        batch.send("Crew of Module B", "Hello");

        assertEquals(2, email.getSentCount());
        assertTrue(email.getSentMessages().get(0).rendered().contains("Scheduled batch: Crew of Module B"));
    }

    @Test
    @DisplayName("Batch: empty recipient list gives an empty report; recipients can be added later")
    void batchRecipientsManagement() {
        ScheduledBatchNotification batch = new ScheduledBatchNotification(sms, List.of());
        assertEquals(0, batch.sendBatch("Nobody", "Hi").total());

        batch.addRecipient("Aigerim");
        assertEquals(1, batch.sendBatch("Somebody", "Hi").successful());
    }

    @Test
    @DisplayName("Batch: 'scheduled' flag - due only when the planned time has been reached")
    void batchSchedule() {
        LocalDateTime planned = LocalDateTime.of(2026, 10, 16, 6, 0);
        ScheduledBatchNotification batch = new ScheduledBatchNotification(sms, List.of("a"), planned);

        assertFalse(batch.isDue(planned.minusMinutes(1)));
        assertTrue(batch.isDue(planned));
        assertTrue(new ScheduledBatchNotification(sms, List.of("a")).isDue(planned.minusYears(1)));
    }

    // ---------- runtime switching ----------

    @Test
    @DisplayName("Runtime switching: the SAME Notification object sends via Telegram, then via Email")
    void runtimeSenderSwitching() {
        Notification notification = new UrgentNotification(telegram);

        notification.send("Madina", "First");
        notification.setSender(email);
        notification.send("Madina", "Second");

        assertEquals(1, telegram.getSentCount());
        assertEquals(1, email.getSentCount());
        assertEquals("[URGENT] First", telegram.getSentMessages().get(0).body());
        assertEquals("[URGENT] Second", email.getSentMessages().get(0).body());
        assertSame(email, notification.getSender());
    }

    @Test
    @DisplayName("Runtime switching works for every notification type, not only Urgent")
    void switchingWorksForEncryptedToo() {
        EncryptedNotification encrypted = new EncryptedNotification(telegram);

        encrypted.send("Madina", "one");
        encrypted.setSender(sms);
        encrypted.send("Madina", "two");

        assertEquals(1, telegram.getSentCount());
        assertEquals(1, sms.getSentCount());
    }
}
