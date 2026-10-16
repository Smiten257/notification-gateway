package gateway.sender;

import java.io.PrintStream;

/**
 * Kafka: does not talk to a person - it SIMULATES publishing an event to a topic.
 * The recipient becomes the record key (which decides the partition); every partition keeps its own offset.
 */
public class KafkaEventSender extends AbstractSimulatedSender {

    public static final String DEFAULT_TOPIC = "station.notifications";
    public static final int PARTITIONS = 3;

    private final String topic;
    private final long[] nextOffset = new long[PARTITIONS];

    public KafkaEventSender() {
        this(DEFAULT_TOPIC, System.out);
    }

    public KafkaEventSender(PrintStream out) {
        this(DEFAULT_TOPIC, out);
    }

    public KafkaEventSender(String topic, PrintStream out) {
        super("Kafka", out);
        this.topic = topic;
    }

    @Override
    public boolean sendMessage(String recipient, String title, String body) {
        String problem = findCommonProblem(recipient, body);
        if (problem != null) {
            return reject(recipient, problem);
        }
        int partition = Math.floorMod(recipient.hashCode(), PARTITIONS);
        long offset = nextOffset[partition]++;
        String rendered = "{\"topic\":\"" + topic + "\",\"partition\":" + partition + ",\"offset\":" + offset
                + ",\"key\":\"" + json(recipient) + "\",\"title\":\"" + json(nullToEmpty(title))
                + "\",\"body\":\"" + json(body) + "\"}";
        return accept(recipient, title, body, rendered);
    }

    private static String json(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
