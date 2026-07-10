/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.controller;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.cluster.common.ClusterControllerEventReason;
import io.stackgres.cluster.common.StackGresClusterContext;
import io.stackgres.cluster.configuration.ClusterControllerPropertyContext;
import io.stackgres.common.ClusterControllerProperty;
import io.stackgres.common.ClusterPath;
import io.stackgres.common.CustomPersistentVolumeUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.operatorframework.reconciliation.ReconciliationResult;
import io.stackgres.operatorframework.reconciliation.SafeReconciliator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks the WAL path applied to this Pod in {@code SGCluster.status.podStatuses[].walPath}
 * and, when {@code SGCluster.spec.configurations.postgres.walPath} changes, moves the WAL
 * directory to the new location during the Pod startup, before patroni is signaled to start
 * (when PostgreSQL is guaranteed to be stopped).
 *
 * <p>The applied WAL path is detected from the filesystem: when {@code $PGDATA/pg_wal} is a
 * symbolic link its target is the applied WAL path, when it is a plain directory the WAL
 * directory is placed, as by default, under the PostgreSQL data directory.</p>
 *
 * <p>The relocation is implemented so that a crash at any point resumes cleanly on the next
 * start: the content is first copied to a sibling of the target directory (with the
 * {@code .relocating} suffix), fsynced and atomically renamed to the target; the switch of
 * {@code $PGDATA/pg_wal} is performed creating the new symbolic link (or directory) with the
 * {@code pg_wal.new} name, renaming {@code pg_wal} to {@code pg_wal.old} and renaming
 * {@code pg_wal.new} to {@code pg_wal}; the old content is removed last.</p>
 */
@ApplicationScoped
public class ClusterWalRelocationReconciliator
    extends SafeReconciliator<StackGresClusterContext,
        ClusterWalRelocationReconciliator.WalRelocationResult> {

  private static final Logger LOGGER = LoggerFactory.getLogger(
      ClusterWalRelocationReconciliator.class);

  static final String RELOCATING_SUFFIX = ".relocating";
  static final String PG_WAL = "pg_wal";
  static final String PG_WAL_NEW = "pg_wal.new";
  static final String PG_WAL_OLD = "pg_wal.old";
  static final long FREE_SPACE_HEADROOM_BYTES = 64L * 1024L * 1024L;

  public record WalRelocationResult(boolean clusterUpdated, boolean patroniStartAllowed) {
  }

  private final EventController eventController;
  private final String podName;

  Path pgDataPath = Paths.get(ClusterPath.PG_DATA_PATH.path());
  Path patroniStartFilePath = Paths.get(ClusterPath.PATRONI_START_FILE_PATH.path());

  @Dependent
  public static class Parameters {
    @Inject EventController eventController;
    @Inject ClusterControllerPropertyContext propertyContext;
  }

  @Inject
  public ClusterWalRelocationReconciliator(Parameters parameters) {
    this.eventController = parameters.eventController;
    this.podName = parameters.propertyContext.getString(
        ClusterControllerProperty.CLUSTER_CONTROLLER_POD_NAME);
  }

  @Override
  public ReconciliationResult<WalRelocationResult> safeReconcile(KubernetesClient client,
      StackGresClusterContext context) {
    final StackGresCluster cluster = context.getCluster();
    try {
      return new ReconciliationResult<>(reconcileWalPath(client, cluster));
    } catch (IOException | RuntimeException ex) {
      LOGGER.error("An error occurred while reconciling the WAL path", ex);
      try {
        eventController.sendEvent(ClusterControllerEventReason.CLUSTER_CONTROLLER_ERROR,
            "An error occurred while reconciling the WAL path: " + ex.getMessage(),
            client);
      } catch (Exception eventEx) {
        LOGGER.error("An error occurred while sending an event", eventEx);
      }
      return new ReconciliationResult<>(new WalRelocationResult(false, false), ex);
    }
  }

  private WalRelocationResult reconcileWalPath(KubernetesClient client,
      StackGresCluster cluster) throws IOException {
    if (!Files.isDirectory(pgDataPath)) {
      // The instance has not been bootstrapped yet: initdb, pg_basebackup and the restore
      // scripts place the WAL directory following the spec directly.
      return new WalRelocationResult(false, true);
    }

    String actualWalPath = readActualWalPath();
    final String specWalPath = CustomPersistentVolumeUtil.getWalPath(cluster).orElse(null);

    boolean patroniStartAllowed = true;
    if (!Objects.equals(specWalPath, actualWalPath)) {
      if (Files.exists(patroniStartFilePath)) {
        // PostgreSQL may be running: the relocation will be performed during the Pod startup
        // once the Pod is restarted (the operator flags the Pod with the PendingRestart
        // condition when the applied WAL path differs from the spec).
        LOGGER.debug("WAL path changed from {} to {}, waiting for the Pod to be restarted",
            actualWalPath, specWalPath);
      } else {
        patroniStartAllowed = relocateWal(client, cluster, actualWalPath, specWalPath);
        if (patroniStartAllowed) {
          actualWalPath = specWalPath;
        }
      }
    }

    final boolean clusterUpdated = updatePodStatusWalPath(cluster, actualWalPath);
    return new WalRelocationResult(clusterUpdated, patroniStartAllowed);
  }

  String readActualWalPath() throws IOException {
    final Path pgWalPath = pgDataPath.resolve(PG_WAL);
    if (Files.isSymbolicLink(pgWalPath)) {
      return Files.readSymbolicLink(pgWalPath).toString();
    }
    return null;
  }

  private boolean relocateWal(KubernetesClient client, StackGresCluster cluster,
      String actualWalPath, String specWalPath) throws IOException {
    final Path pgWalPath = pgDataPath.resolve(PG_WAL);
    final Path pgWalNewPath = pgDataPath.resolve(PG_WAL_NEW);
    final Path pgWalOldPath = pgDataPath.resolve(PG_WAL_OLD);

    // Resume of a previously crashed relocation: when pg_wal is missing the crash happened
    // in the middle of the switch, finish it.
    if (!Files.exists(pgWalPath, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
      if (Files.exists(pgWalNewPath, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
        LOGGER.info("Resuming WAL relocation: renaming {} to {}", pgWalNewPath, pgWalPath);
        Files.move(pgWalNewPath, pgWalPath, StandardCopyOption.ATOMIC_MOVE);
        deleteOldWal(pgWalOldPath);
        return true;
      }
      throw new IOException(PG_WAL + " not found in " + pgDataPath
          + " and no relocation to resume was found");
    }

    // The source of the content is the current WAL directory as seen by this container.
    final Path sourcePath;
    if (actualWalPath == null) {
      sourcePath = pgWalPath;
    } else {
      final Optional<Path> translatedSourcePath = CustomPersistentVolumeUtil
          .translatePatroniPathToController(cluster, actualWalPath);
      if (translatedSourcePath.isEmpty()) {
        blockWithMessage(client, "The applied WAL path " + actualWalPath + " is not under the"
            + " data volume nor under a mount of a custom persistent volume so the WAL"
            + " directory can not be moved from it");
        return false;
      }
      sourcePath = translatedSourcePath.get();
    }

    // The destination is the spec WAL path or, when it is unset, a directory that will be
    // placed back under the PostgreSQL data directory.
    final Path destinationPath;
    if (specWalPath == null) {
      destinationPath = pgWalNewPath;
    } else {
      final Optional<Path> translatedDestinationPath = CustomPersistentVolumeUtil
          .translatePatroniPathToController(cluster, specWalPath);
      if (translatedDestinationPath.isEmpty()) {
        blockWithMessage(client, "The WAL path " + specWalPath + " is not under the data"
            + " volume nor under a mount of a custom persistent volume of the patroni"
            + " container so the WAL directory can not be moved to it");
        return false;
      }
      destinationPath = translatedDestinationPath.get();
    }

    LOGGER.info("Relocating the WAL directory from {} to {}",
        actualWalPath == null ? pgWalPath : actualWalPath,
        specWalPath == null ? pgWalPath : specWalPath);

    if (!copyWalContentIfRequired(client, sourcePath, destinationPath)) {
      return false;
    }

    // Switch $PGDATA/pg_wal: create the new entry with a temporary name, then swap it with
    // two atomic renames.
    if (specWalPath != null
        && !Files.exists(pgWalNewPath, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
      Files.createSymbolicLink(pgWalNewPath, Paths.get(specWalPath));
    }
    Files.move(pgWalPath, pgWalOldPath, StandardCopyOption.ATOMIC_MOVE);
    Files.move(pgWalNewPath, pgWalPath, StandardCopyOption.ATOMIC_MOVE);

    // Remove the old content last, it is the only data destroying step.
    if (actualWalPath != null) {
      deleteDirectoryContent(sourcePath);
    }
    deleteOldWal(pgWalOldPath);

    LOGGER.info("WAL directory relocated to {}",
        specWalPath == null ? pgWalPath : specWalPath);
    return true;
  }

  /**
   * Copy the WAL content from the source to the destination using a sibling of the
   * destination with the {@code .relocating} suffix that is atomically renamed to the
   * destination once the copy has been fsynced. Returns false (blocking patroni start) when
   * the target filesystem has not enough free space.
   */
  private boolean copyWalContentIfRequired(KubernetesClient client, Path sourcePath,
      Path destinationPath) throws IOException {
    if (destinationPath.getFileName().toString().equals(PG_WAL_NEW)) {
      // Relocating back under the PostgreSQL data directory: pg_wal.new is the copy target.
      if (Files.exists(destinationPath, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
        deleteRecursively(destinationPath);
      }
      if (!checkFreeSpace(client, sourcePath, pgDataPath)) {
        return false;
      }
      copyRecursivelyAndSync(sourcePath, destinationPath);
      return true;
    }
    if (Files.exists(destinationPath, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
      try (Stream<Path> content = Files.list(destinationPath)) {
        if (content.findAny().isEmpty()) {
          Files.delete(destinationPath);
        } else {
          // A previous relocation crashed after the copy was atomically renamed to the
          // destination but before the switch of $PGDATA/pg_wal: the destination content is
          // complete, resume from the switch.
          LOGGER.warn("The WAL relocation destination {} already exists and is not empty,"
              + " assuming it is the complete copy of a previously interrupted relocation",
              destinationPath);
          return true;
        }
      }
    }
    final Path relocatingPath = destinationPath
        .resolveSibling(destinationPath.getFileName() + RELOCATING_SUFFIX);
    if (Files.exists(relocatingPath, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
      deleteRecursively(relocatingPath);
    }
    if (!checkFreeSpace(client, sourcePath, destinationPath.getParent())) {
      return false;
    }
    Files.createDirectories(destinationPath.getParent());
    copyRecursivelyAndSync(sourcePath, relocatingPath);
    Files.move(relocatingPath, destinationPath, StandardCopyOption.ATOMIC_MOVE);
    return true;
  }

  private boolean checkFreeSpace(KubernetesClient client, Path sourcePath, Path targetPath)
      throws IOException {
    final long requiredBytes = directorySize(sourcePath) + FREE_SPACE_HEADROOM_BYTES;
    final long usableBytes = usableSpace(targetPath);
    if (usableBytes < requiredBytes) {
      blockWithMessage(client, "Not enough free space to relocate the WAL directory: "
          + requiredBytes + " bytes are required but only " + usableBytes + " bytes are"
          + " available on the filesystem of " + targetPath + ". Expand the volume or free"
          + " some space, the relocation will be retried.");
      return false;
    }
    return true;
  }

  long directorySize(Path path) throws IOException {
    try (Stream<Path> walk = Files.walk(path)) {
      return walk
          .filter(Files::isRegularFile)
          .mapToLong(file -> {
            try {
              return Files.size(file);
            } catch (IOException ex) {
              return 0L;
            }
          })
          .sum();
    }
  }

  long usableSpace(Path path) throws IOException {
    Path existingPath = path;
    while (!Files.exists(existingPath)) {
      existingPath = existingPath.getParent();
    }
    return Files.getFileStore(existingPath).getUsableSpace();
  }

  void copyRecursivelyAndSync(Path sourcePath, Path destinationPath) throws IOException {
    final List<Path> copiedDirectories = new ArrayList<>();
    Files.createDirectories(destinationPath);
    copiedDirectories.add(destinationPath);
    try (Stream<Path> walk = Files.walk(sourcePath)) {
      for (Path source : walk.skip(1).toList()) {
        final Path destination = destinationPath.resolve(sourcePath.relativize(source));
        if (Files.isDirectory(source)) {
          Files.createDirectories(destination);
          copiedDirectories.add(destination);
        } else {
          Files.copy(source, destination,
              StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
          try (FileChannel channel = FileChannel.open(destination, StandardOpenOption.WRITE)) {
            channel.force(true);
          }
        }
      }
    }
    for (Path copiedDirectory : copiedDirectories) {
      try (FileChannel channel = FileChannel.open(copiedDirectory, StandardOpenOption.READ)) {
        channel.force(true);
      } catch (IOException ex) {
        // Some filesystems do not support fsync on directories opened for read
        LOGGER.debug("Could not fsync directory {}", copiedDirectory, ex);
      }
    }
  }

  private void deleteOldWal(Path pgWalOldPath) throws IOException {
    if (Files.exists(pgWalOldPath, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
      deleteRecursively(pgWalOldPath);
    }
  }

  void deleteRecursively(Path path) throws IOException {
    if (Files.isSymbolicLink(path) || !Files.isDirectory(path)) {
      Files.delete(path);
      return;
    }
    try (Stream<Path> walk = Files.walk(path)) {
      for (Path file : walk.sorted(Comparator.reverseOrder()).toList()) {
        Files.delete(file);
      }
    }
  }

  void deleteDirectoryContent(Path path) throws IOException {
    try (Stream<Path> content = Files.list(path)) {
      for (Path file : content.toList()) {
        deleteRecursively(file);
      }
    }
  }

  private void blockWithMessage(KubernetesClient client, String message) {
    final String fullMessage = "WAL relocation failed, patroni start is blocked: " + message;
    LOGGER.error(fullMessage);
    try {
      eventController.sendEvent(ClusterControllerEventReason.CLUSTER_WAL_RELOCATION_FAILED,
          fullMessage, client);
    } catch (Exception ex) {
      LOGGER.error("An error occurred while sending an event", ex);
    }
  }

  private boolean updatePodStatusWalPath(StackGresCluster cluster, String actualWalPath) {
    if (cluster.getStatus() == null) {
      cluster.setStatus(new StackGresClusterStatus());
    }
    if (cluster.getStatus().getPodStatuses() == null) {
      cluster.getStatus().setPodStatuses(new ArrayList<>());
    }
    final Optional<StackGresClusterPodStatus> foundPodStatus = cluster.getStatus()
        .getPodStatuses()
        .stream()
        .filter(podStatus -> Objects.equals(podName, podStatus.getName()))
        .findFirst();
    final StackGresClusterPodStatus podStatus = foundPodStatus
        .orElseGet(() -> {
          StackGresClusterPodStatus newPodStatus = new StackGresClusterPodStatus();
          newPodStatus.setName(podName);
          newPodStatus.setPrimary(false);
          newPodStatus.setPendingRestart(false);
          cluster.getStatus().getPodStatuses().add(newPodStatus);
          return newPodStatus;
        });
    if (!Objects.equals(podStatus.getWalPath(), actualWalPath)) {
      podStatus.setWalPath(actualWalPath);
      return true;
    }
    return foundPodStatus.isEmpty();
  }

}
