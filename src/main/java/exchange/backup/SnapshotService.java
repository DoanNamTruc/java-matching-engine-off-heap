package exchange.backup;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public final class SnapshotService {

    public static void save(Path path, Snapshotting book) throws IOException {
        Path tmp = Paths.get(path.toString() + ".tmp");

        try (DataOutputStream out =
                     new DataOutputStream(
                             new BufferedOutputStream(
                                     Files.newOutputStream(tmp)))) {

            book.snapshot(out);
            out.flush();
        }

        Files.move(tmp, path,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE);
    }

    public static void load(Path path, Snapshotting book) throws IOException {
        if (!Files.exists(path)) return;

        try (DataInputStream in =
                     new DataInputStream(
                             new BufferedInputStream(
                                     Files.newInputStream(path)))) {

            book.restore(in);
        }
    }
}
