package io.stackgres.slony.postgres;

import io.stackgres.postgres.PostgresCluster;
import io.stackgres.postgres.SlonyLinuxInstance;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.*;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Stream;

public class Directories {

    public String logFileName(PostgresCluster cluster) {
        return cluster.getName() + ".log";
    }

    public String etcdLogFileName() {
        return "etcd.log";
    }

    public Path etcdDataPath(PostgresCluster cluster) {
        // TODO make path changeable
        return Paths.get("/var/lib/stackgres/etcd", cluster.getId().toString());
    }

    public Path logPath(PostgresCluster cluster, SlonyLinuxInstance instance) {
        return instance.getLogDir().resolve(logFileName(cluster));
    }

    public void deleteLogsDir(SlonyLinuxInstance instance) {
        deleteRecursively(instance.getLogDir());
    }

    private static void deleteRecursively(Path path) {
        try (Stream<Path> walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not delete directory " + path, e);
        }
    }

    private static void delete(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not delete " + path, e);
        }
    }

    public void createDirectories(SlonyLinuxInstance instance) {
        try {
            if (!Files.exists(instance.getDataDir()))
                Files.createDirectories(instance.getDataDir());
            Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rwxrwxrwx");
            Files.setPosixFilePermissions(instance.getDataDir(), perms);

            UserPrincipalLookupService lookupService = instance.getDataDir().getFileSystem().getUserPrincipalLookupService();
            Files.setOwner(instance.getDataDir(), lookupService.lookupPrincipalByName("70"));
            Files.getFileAttributeView(instance.getDataDir(), PosixFileAttributeView.class).setGroup(lookupService.lookupPrincipalByGroupName("1000"));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create the data directory " + instance.getDataDir(), e);
        }
    }

}