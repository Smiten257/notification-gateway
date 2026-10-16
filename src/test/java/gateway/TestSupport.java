package gateway;

import gateway.sender.MessageSender;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/** Shared helpers for the tests. */
final class TestSupport {

    /** Swallows the console output of the simulated providers, so test logs stay clean. */
    static final PrintStream SILENT = new PrintStream(OutputStream.nullOutputStream());

    private TestSupport() {
    }

    /**
     * A provider written ONLY for the tests. It implements the MessageSender interface and nothing else.
     * If the Notification classes work with it, they depend on the interface and not on concrete providers.
     */
    static final class RecordingSender implements MessageSender {
        final List<String> bodies = new ArrayList<>();
        private final String name;
        private final boolean accepts;
        private boolean available = true;

        RecordingSender(String name) {
            this(name, true);
        }

        RecordingSender(String name, boolean accepts) {
            this.name = name;
            this.accepts = accepts;
        }

        void setAvailable(boolean available) {
            this.available = available;
        }

        @Override
        public boolean sendMessage(String recipient, String title, String body) {
            if (!available || !accepts) {
                return false;
            }
            bodies.add(body);
            return true;
        }

        @Override
        public boolean isAvailable() {
            return available;
        }

        @Override
        public String getChannelName() {
            return name;
        }
    }
}
