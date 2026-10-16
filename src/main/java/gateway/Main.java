package gateway;

import gateway.notification.BatchReport;
import gateway.notification.EncryptedNotification;
import gateway.notification.MarketingNotification;
import gateway.notification.Notification;
import gateway.notification.ScheduledBatchNotification;
import gateway.notification.UrgentNotification;
import gateway.sender.EmailMessageSender;
import gateway.sender.KafkaEventSender;
import gateway.sender.MessageSender;
import gateway.sender.PushNotificationSender;
import gateway.sender.SentMessage;
import gateway.sender.SmsMessageSender;
import gateway.sender.TelegramMessageSender;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Mandatory demo of the Multi-Channel Notification Gateway (Bridge pattern).
 *
 * <p>Story: the control center of an orbital station notifies its crew.
 * Note that no scenario needs a class like "UrgentTelegramNotification":
 * a notification type and a provider are simply combined at runtime.</p>
 */
public class Main {

    public static void main(String[] args) {
        scenario1UrgentTelegram();
        scenario2EncryptedEmail();
        scenario3BatchSms();
        scenario4RuntimeSwitching();
        scenario5FallbackToSms();
        scenario6MarketingPush();
        bonusEncryptedKafka();
        printBridgeSummary();
    }

    // 1. UrgentNotification + Telegram
    private static void scenario1UrgentTelegram() {
        header(1, "UrgentNotification + Telegram");
        MessageSender telegram = new TelegramMessageSender();
        Notification urgent = new UrgentNotification(telegram);
        urgent.send("Commander Aigerim", "Pressure drop in Module B. Move to Module A immediately.");
    }

    // 2. EncryptedNotification + Email
    private static void scenario2EncryptedEmail() {
        header(2, "EncryptedNotification + Email");
        EmailMessageSender email = new EmailMessageSender();
        EncryptedNotification encrypted = new EncryptedNotification(email);
        String secret = "Docking window moved to 04:30 UTC. Access code: AURORA-7.";
        encrypted.send("Flight Director Nurlan", secret);

        SentMessage received = email.getSentMessages().get(0);
        System.out.println("  Plain text    : " + secret);
        System.out.println("  On the wire   : " + received.body());
        System.out.println("  Decrypted back: " + encrypted.decrypt(received.body()));
    }

    // 3. ScheduledBatchNotification + SMS
    private static void scenario3BatchSms() {
        header(3, "ScheduledBatchNotification + SMS");
        SmsMessageSender sms = new SmsMessageSender();
        // two crew members have their phones switched off -> two failures
        sms.markUnreachable("Bekzat");
        sms.markUnreachable("Dana");

        List<String> crew = List.of("Aigerim", "Nurlan", "Bekzat", "Dana", "Madina",
                "Yerlan", "Saule", "Timur", "Aliya", "Askar");
        ScheduledBatchNotification batch =
                new ScheduledBatchNotification(sms, crew, LocalDateTime.of(2026, 10, 16, 6, 0));

        System.out.println("  Planned for   : " + batch.getScheduledFor() + " (simulated, dispatching now)");
        BatchReport report = batch.sendBatch("Crew of the station",
                "Solar storm warning: shelter in Module C from 06:00 UTC.");
        System.out.println();
        System.out.println(indent(report.toString()));
        System.out.println("  Not reached   : " + report.failedRecipients());
    }

    // 4. Runtime switching: Telegram -> Email (same Notification object)
    private static void scenario4RuntimeSwitching() {
        header(4, "Runtime switching: Telegram -> Email");
        MessageSender telegram = new TelegramMessageSender();
        MessageSender email = new EmailMessageSender();

        Notification notification = new UrgentNotification(telegram);
        System.out.println("  Provider now: " + notification.getSender().getChannelName());
        notification.send("Dr. Madina", "Oxygen generator maintenance starts in 10 minutes.");

        notification.setSender(email);   // <- the only line that changes the provider
        System.out.println();
        System.out.println("  Provider now: " + notification.getSender().getChannelName());
        notification.send("Dr. Madina", "Oxygen generator maintenance starts in 10 minutes.");
    }

    // 5. Telegram unavailable -> fallback to SMS
    private static void scenario5FallbackToSms() {
        header(5, "Telegram unavailable -> fallback to SMS");
        TelegramMessageSender telegram = new TelegramMessageSender();
        SmsMessageSender sms = new SmsMessageSender();
        telegram.setAvailable(false);   // simulate an outage

        UrgentNotification urgent = new UrgentNotification(telegram, List.of(sms));
        System.out.println("  Telegram available? " + telegram.isAvailable());
        urgent.send("Engineer Timur", "Coolant leak in Module D. Report to the control room.");
        System.out.println();
        System.out.println("  Delivered via : " + urgent.getLastUsedChannel()
                + " (fallback used: " + urgent.wasFallbackUsed() + ")");
    }

    // 6. MarketingNotification + PushNotificationSender (both added later, nothing modified)
    private static void scenario6MarketingPush() {
        header(6, "MarketingNotification + PushNotificationSender");
        PushNotificationSender push = new PushNotificationSender();
        Notification marketing = new MarketingNotification(push, "Observation Deck Tour");
        marketing.send("device:aliya-phone", "Book the new panoramic tour of the Earth's night side.");
    }

    // Bonus: the fourth required provider (Kafka) is also used through the same Notification abstraction
    private static void bonusEncryptedKafka() {
        header(7, "Bonus: EncryptedNotification + Kafka");
        Notification encrypted = new EncryptedNotification(new KafkaEventSender());
        encrypted.send("module-b", "Telemetry snapshot #42 archived.");
    }

    private static void printBridgeSummary() {
        System.out.println();
        System.out.println("=".repeat(70));
        System.out.println("  WHY BRIDGE?");
        System.out.println("=".repeat(70));
        System.out.println("  Notification types : 4  (Urgent, Encrypted, ScheduledBatch, Marketing)");
        System.out.println("  Providers          : 5  (Telegram, Email, SMS, Kafka, Push)");
        System.out.println("  Without Bridge     : 4 x 5 = 20 combination classes");
        System.out.println("  With Bridge        : 4 + 5 =  9 classes (all 20 combinations work)");
        System.out.println("=".repeat(70));
    }

    private static void header(int number, String title) {
        System.out.println();
        System.out.println("-".repeat(70));
        System.out.println("  Scenario " + number + ": " + title);
        System.out.println("-".repeat(70));
    }

    private static String indent(String text) {
        return "  " + text.replace("\n", "\n  ");
    }
}
