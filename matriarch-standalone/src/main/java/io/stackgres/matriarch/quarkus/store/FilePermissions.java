package io.stackgres.matriarch.quarkus.store;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

/**
 * Best-effort restrict a file to owner read/write only ({@code 0600}) — for secrets persisted at rest
 * (the SQLite DB files and the secret keyfile, §3.7). A no-op (with a warning) on a non-POSIX filesystem.
 */
final class FilePermissions {

    private static final System.Logger LOG = System.getLogger(FilePermissions.class.getName());

    private FilePermissions() {
    }

    static void restrictToOwner(Path path) {
        if (!Files.exists(path)) {
            return;
        }
        try {
            Files.setPosixFilePermissions(path,
                    Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
        } catch (UnsupportedOperationException | IOException e) {
            LOG.log(System.Logger.Level.WARNING, "could not restrict permissions on {0}: {1}", path, e.toString());
        }
    }
}
