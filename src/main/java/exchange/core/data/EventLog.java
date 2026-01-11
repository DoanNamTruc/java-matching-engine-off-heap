package exchange.core.data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

public final class EventLog {

    private final Path path;

    public EventLog(Path path) {
        this.path = path;
    }

    public void append(OrderEvent e) throws IOException {
        // TODO: write binary append
    }

    public void replayFrom(long seq, Consumer<OrderEvent> consumer)
            throws IOException {

        if (!Files.exists(path)) return;

        // TODO:
        // read events
        // if (event.seq >= seq) consumer.accept(event);
    }
}
