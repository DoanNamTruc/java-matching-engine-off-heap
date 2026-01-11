package exchange.backup;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public interface Snapshotting {
    void snapshot(DataOutputStream out) throws IOException;
    void restore(DataInputStream in) throws IOException;
}
