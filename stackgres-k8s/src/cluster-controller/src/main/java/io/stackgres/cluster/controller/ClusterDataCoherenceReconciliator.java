/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.controller;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.cluster.common.ClusterControllerEventReason;
import io.stackgres.cluster.common.StackGresClusterContext;
import io.stackgres.cluster.configuration.ClusterControllerPropertyContext;
import io.stackgres.common.ClusterControllerProperty;
import io.stackgres.common.ClusterPath;
import io.stackgres.common.CustomPersistentVolumeUtil;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodsCustomPersistentVolume;
import io.stackgres.operatorframework.reconciliation.ReconciliationResult;
import io.stackgres.operatorframework.reconciliation.SafeReconciliator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Checks, before patroni is signaled to start, that all the volumes of the coherent data set
 * (the data volume plus every custom persistent volume with {@code coherentData} set to
 * {@code true}) belong together, so that PostgreSQL is never started after one of the
 * PersistentVolumeClaims of the set has been deleted and re-created empty (what would corrupt
 * or lose data since those volumes are written simultaneously by PostgreSQL).
 *
 * <p>The check is based on marker files stored at the root of each PersistentVolumeClaim of
 * the set (the data volume is mounted at its root on {@code /var/lib/postgresql} and each
 * coherent custom persistent volume is mounted at its root under
 * {@code /stackgres/custom-persistent-volumes/<name>} in this container):</p>
 *
 * <ul>
 * <li>{@code .data-coherent-id}: the first line is a random identifier shared by all the
 * volumes of the set of this Pod, the second line is the uid of the PersistentVolumeClaim the
 * marker was written for. When the recorded uid differs from the current
 * PersistentVolumeClaim uid the content of the volume was restored (e.g. from a
 * VolumeSnapshot) and the whole set is re-initialized instead of blocking.</li>
 * <li>{@code .data-coherent-mount-paths}: the list of the members of the set, each recording
 * the volume name, the mount path and the subPath used in the patroni container, so that a
 * member whose marker disappeared (or whose subPath changed, silently swapping the directory
 * PostgreSQL sees for a fresh one) is detected using the copies stored on the surviving
 * volumes.</li>
 * </ul>
 *
 * <p>The result of the reconciliation is {@code true} when patroni can be signaled to start
 * and {@code false} when it must be blocked. The check is only active until patroni has been
 * signaled for the first time (a PersistentVolumeClaim can not be deleted and re-created
 * while the Pod is running) and when at least a custom persistent volume with
 * {@code coherentData} set to {@code true} exists.</p>
 */
@ApplicationScoped
public class ClusterDataCoherenceReconciliator
    extends SafeReconciliator<StackGresClusterContext, Boolean> {

  private static final Logger LOGGER = LoggerFactory.getLogger(
      ClusterDataCoherenceReconciliator.class);

  static final String DATA_COHERENT_ID_FILE = ".data-coherent-id";
  static final String DATA_COHERENT_MOUNT_PATHS_FILE = ".data-coherent-mount-paths";
  static final String DATA_COHERENT_MOUNT_PATHS_VERSION = "#v1";
  static final String DATA_VOLUME_NAME = "data";

  private final EventController eventController;
  private final String podName;

  Path pgBasePath = Paths.get(ClusterPath.PG_BASE_PATH.path());
  Path customVolumesBasePath = Paths.get(ClusterPath.CUSTOM_PERSISTENT_VOLUMES_PATH.path());
  Path patroniStartFilePath = Paths.get(ClusterPath.PATRONI_START_FILE_PATH.path());

  @Dependent
  public static class Parameters {
    @Inject EventController eventController;
    @Inject ClusterControllerPropertyContext propertyContext;
  }

  @Inject
  public ClusterDataCoherenceReconciliator(Parameters parameters) {
    this.eventController = parameters.eventController;
    this.podName = parameters.propertyContext.getString(
        ClusterControllerProperty.CLUSTER_CONTROLLER_POD_NAME);
  }

  record CoherenceMember(String volumeName, String mountPath, String subPath) {
    static CoherenceMember fromLine(String line) {
      String[] fields = line.split("\t", -1);
      if (fields.length != 3) {
        throw new IllegalArgumentException("Illegal member line: " + line);
      }
      return new CoherenceMember(fields[0], fields[1], fields[2]);
    }

    String toLine() {
      return volumeName + "\t" + mountPath + "\t" + subPath;
    }
  }

  record VolumeMarker(String coherenceId, String pvcUid) {
  }

  @Override
  public ReconciliationResult<Boolean> safeReconcile(KubernetesClient client,
      StackGresClusterContext context) {
    final StackGresCluster cluster = context.getCluster();
    try {
      return new ReconciliationResult<>(reconcileDataCoherence(client, cluster));
    } catch (IOException | RuntimeException ex) {
      LOGGER.error("An error occurred while checking the data coherence", ex);
      try {
        eventController.sendEvent(ClusterControllerEventReason.CLUSTER_CONTROLLER_ERROR,
            "An error occurred while checking the data coherence: " + ex.getMessage(),
            client);
      } catch (Exception eventEx) {
        LOGGER.error("An error occurred while sending an event", eventEx);
      }
      return new ReconciliationResult<>(false, ex);
    }
  }

  private boolean reconcileDataCoherence(KubernetesClient client, StackGresCluster cluster)
      throws IOException {
    final List<StackGresClusterPodsCustomPersistentVolume> coherentVolumes =
        CustomPersistentVolumeUtil.getCustomPersistentVolumes(cluster)
        .stream()
        .filter(CustomPersistentVolumeUtil::isCoherentData)
        .toList();
    if (coherentVolumes.isEmpty()) {
      return true;
    }
    if (Files.exists(patroniStartFilePath)) {
      // Once patroni has been signaled to start the PersistentVolumeClaims can not be deleted
      // and re-created while the Pod is running so the check is not needed anymore.
      return true;
    }

    final List<CoherenceMember> expectedMembers = expectedMembers(cluster, coherentVolumes);
    final Set<String> expectedVolumeNames = expectedMembers.stream()
        .map(CoherenceMember::volumeName)
        .collect(Collectors.toSet());

    for (String volumeName : expectedVolumeNames) {
      if (!Files.isDirectory(volumeRoot(volumeName))) {
        String message = "Data coherence check failed, patroni start is blocked: the volume "
            + volumeName + " is not mounted at " + volumeRoot(volumeName)
            + " in the cluster controller container";
        LOGGER.error(message);
        sendEvent(client, ClusterControllerEventReason.CLUSTER_DATA_COHERENCE_BLOCKED, message);
        return false;
      }
    }

    final Map<String, VolumeMarker> markers = new HashMap<>();
    final Map<String, List<CoherenceMember>> recordedMembers = new HashMap<>();
    for (String volumeName : expectedVolumeNames) {
      readMarker(volumeName).ifPresent(marker -> markers.put(volumeName, marker));
      List<CoherenceMember> members = readMountPathsFile(volumeRoot(volumeName));
      if (!members.isEmpty()) {
        recordedMembers.put(volumeName, members);
      }
    }

    final Map<String, String> pvcUids = new HashMap<>();
    for (String volumeName : expectedVolumeNames) {
      pvcUids.put(volumeName, resolvePvcUid(client, cluster, volumeName));
    }

    final List<String> restoredVolumes = markers.entrySet().stream()
        .filter(marker -> !Objects.equals(
            marker.getValue().pvcUid(), pvcUids.get(marker.getKey())))
        .map(Map.Entry::getKey)
        .toList();
    if (!restoredVolumes.isEmpty()) {
      String message = "Data coherence markers of volumes " + restoredVolumes + " belong to"
          + " other PersistentVolumeClaims: assuming the content of the volumes was restored"
          + " (e.g. from a VolumeSnapshot) and re-initializing the data coherence markers";
      LOGGER.warn(message);
      sendEvent(client, ClusterControllerEventReason.CLUSTER_DATA_COHERENCE_REINITIALIZED,
          message);
      reconcileMarkers(expectedMembers, expectedVolumeNames, pvcUids, null, true);
      return true;
    }

    if (markers.isEmpty() && recordedMembers.isEmpty()) {
      LOGGER.info("No data coherence markers found, initializing them for a fresh instance");
      reconcileMarkers(expectedMembers, expectedVolumeNames, pvcUids, null, false);
      return true;
    }

    final String referenceId = Optional.ofNullable(markers.get(DATA_VOLUME_NAME))
        .map(VolumeMarker::coherenceId)
        .orElseGet(() -> markers.values().stream()
            .map(VolumeMarker::coherenceId)
            .findFirst()
            .orElse(null));

    final Set<CoherenceMember> recordedUnion = recordedMembers.values().stream()
        .flatMap(List::stream)
        .collect(Collectors.toCollection(LinkedHashSet::new));

    final List<String> violations = new ArrayList<>();
    final Set<String> volumesToReinitialize = new HashSet<>();
    for (CoherenceMember recordedMember : recordedUnion) {
      final String volumeName = recordedMember.volumeName();
      if (!expectedVolumeNames.contains(volumeName)) {
        // The volume was removed from the coherent data set in the spec (a change gated by
        // allowCoherentDataRemoval in the validation webhook): the lists are reconciled below.
        continue;
      }
      final boolean allowRemoval = coherentVolumes.stream()
          .filter(coherentVolume -> coherentVolume.getName().equals(volumeName))
          .anyMatch(CustomPersistentVolumeUtil::isAllowCoherentDataRemoval);
      final VolumeMarker marker = markers.get(volumeName);
      if (marker == null) {
        if (allowRemoval) {
          volumesToReinitialize.add(volumeName);
        } else {
          violations.add("the volume " + volumeName + " was part of the coherent data set of"
              + " this Pod but its data coherence marker is missing: its"
              + " PersistentVolumeClaim was probably deleted and re-created empty");
        }
        continue;
      }
      if (!Objects.equals(marker.coherenceId(), referenceId)) {
        if (allowRemoval) {
          volumesToReinitialize.add(volumeName);
        } else {
          violations.add("the volume " + volumeName + " does not belong to the coherent data"
              + " set of this Pod: its data coherence identifier differs from the one of the"
              + " other volumes");
        }
        continue;
      }
      if (expectedMembers.stream().noneMatch(recordedMember::equals)) {
        if (allowRemoval) {
          volumesToReinitialize.add(volumeName);
        } else {
          violations.add("the mount of volume " + volumeName + " changed from mount path "
              + recordedMember.mountPath() + " and subPath \"" + recordedMember.subPath()
              + "\" making PostgreSQL see a different directory for its content");
        }
      }
    }

    if (!violations.isEmpty()) {
      String message = "Data coherence check failed, patroni start is blocked:\n"
          + violations.stream().map(violation -> "* " + violation + "\n")
              .collect(Collectors.joining())
          + "The volumes of the coherent data set (the data volume and every custom persistent"
          + " volume with coherentData set to true) are written simultaneously by PostgreSQL"
          + " and none of them can be deleted and re-created empty while the others retain"
          + " data, otherwise data will be corrupted or lost. To recover you can: restore the"
          + " content of the offending PersistentVolumeClaim from a backup, delete ALL the"
          + " PersistentVolumeClaims of this Pod so the instance is re-initialized from"
          + " scratch, or, if you know what you are doing, set allowCoherentDataRemoval to"
          + " true on the offending custom persistent volume so the data coherence markers"
          + " are re-initialized.";
      LOGGER.error(message);
      sendEvent(client, ClusterControllerEventReason.CLUSTER_DATA_COHERENCE_BLOCKED, message);
      return false;
    }

    if (!volumesToReinitialize.isEmpty()) {
      String message = "Data coherence markers of volumes " + volumesToReinitialize
          + " are missing or incoherent but allowCoherentDataRemoval is set to true:"
          + " re-initializing the data coherence markers";
      LOGGER.warn(message);
      sendEvent(client, ClusterControllerEventReason.CLUSTER_DATA_COHERENCE_REINITIALIZED,
          message);
    }

    reconcileMarkers(expectedMembers, expectedVolumeNames, pvcUids, referenceId, false);
    return true;
  }

  private List<CoherenceMember> expectedMembers(
      StackGresCluster cluster,
      List<StackGresClusterPodsCustomPersistentVolume> coherentVolumes) {
    final List<CoherenceMember> expectedMembers = new ArrayList<>();
    expectedMembers.add(new CoherenceMember(
        DATA_VOLUME_NAME, ClusterPath.PG_BASE_PATH.path(), ""));
    for (var coherentVolume : coherentVolumes) {
      for (var mount : CustomPersistentVolumeUtil
          .patroniMountsOf(cluster, coherentVolume.getName())) {
        expectedMembers.add(new CoherenceMember(
            coherentVolume.getName(),
            mount.getMountPath(),
            Optional.ofNullable(mount.getSubPath()).orElse("")));
      }
    }
    return expectedMembers;
  }

  Path volumeRoot(String volumeName) {
    if (DATA_VOLUME_NAME.equals(volumeName)) {
      return pgBasePath;
    }
    return customVolumesBasePath.resolve(volumeName);
  }

  Optional<VolumeMarker> readMarker(String volumeName) throws IOException {
    final Path markerPath = volumeRoot(volumeName).resolve(DATA_COHERENT_ID_FILE);
    if (!Files.exists(markerPath)) {
      return Optional.empty();
    }
    List<String> lines = Files.readAllLines(markerPath, StandardCharsets.UTF_8);
    if (lines.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(new VolumeMarker(
        lines.get(0),
        lines.size() > 1 ? lines.get(1) : null));
  }

  List<CoherenceMember> readMountPathsFile(Path volumeRoot) throws IOException {
    final Path mountPathsPath = volumeRoot.resolve(DATA_COHERENT_MOUNT_PATHS_FILE);
    if (!Files.exists(mountPathsPath)) {
      return List.of();
    }
    return Files.readAllLines(mountPathsPath, StandardCharsets.UTF_8).stream()
        .filter(line -> !line.isBlank())
        .filter(line -> !line.startsWith("#"))
        .map(CoherenceMember::fromLine)
        .toList();
  }

  /**
   * Write the data coherence markers on all the volumes of the coherent data set. The
   * identifiers are written before the mount paths lists so that a crash between the two
   * steps can never produce a member recorded in a list without its identifier (what would
   * trigger the check on the next start). When an identifier already exists it is reused so
   * that a crash in the middle of the writes converges on the next cycle; when the set is
   * re-initialized after a restore the mount paths lists are removed first so that any crash
   * state converges to the re-initialization path.
   */
  private void reconcileMarkers(
      List<CoherenceMember> expectedMembers,
      Set<String> expectedVolumeNames,
      Map<String, String> pvcUids,
      String referenceId,
      boolean reset) throws IOException {
    final String coherenceId = Optional.ofNullable(referenceId)
        .orElseGet(() -> UUID.randomUUID().toString());
    if (reset) {
      for (String volumeName : expectedVolumeNames) {
        Files.deleteIfExists(volumeRoot(volumeName).resolve(DATA_COHERENT_MOUNT_PATHS_FILE));
      }
    }
    for (String volumeName : expectedVolumeNames) {
      writeFileAtomically(
          volumeRoot(volumeName).resolve(DATA_COHERENT_ID_FILE),
          coherenceId + "\n" + Optional.ofNullable(pvcUids.get(volumeName)).orElse("") + "\n");
    }
    createDeclaredSubPathDirectories(expectedMembers);
    final String mountPathsContent = DATA_COHERENT_MOUNT_PATHS_VERSION + "\n"
        + expectedMembers.stream()
            .map(CoherenceMember::toLine)
            .sorted()
            .map(line -> line + "\n")
            .collect(Collectors.joining());
    for (String volumeName : expectedVolumeNames) {
      writeFileAtomically(
          volumeRoot(volumeName).resolve(DATA_COHERENT_MOUNT_PATHS_FILE),
          mountPathsContent);
    }
  }

  private void createDeclaredSubPathDirectories(List<CoherenceMember> expectedMembers)
      throws IOException {
    for (CoherenceMember member : expectedMembers) {
      if (!member.subPath().isEmpty()) {
        Files.createDirectories(volumeRoot(member.volumeName()).resolve(member.subPath()));
      }
    }
  }

  void writeFileAtomically(Path path, String content) throws IOException {
    if (Files.exists(path)
        && Files.readString(path, StandardCharsets.UTF_8).equals(content)) {
      return;
    }
    final Path temporaryPath = path.resolveSibling(path.getFileName() + ".tmp");
    try (FileChannel channel = FileChannel.open(temporaryPath,
        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
        StandardOpenOption.WRITE)) {
      channel.write(java.nio.ByteBuffer.wrap(content.getBytes(StandardCharsets.UTF_8)));
      channel.force(true);
    }
    Files.move(temporaryPath, path,
        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
  }

  String resolvePvcUid(KubernetesClient client, StackGresCluster cluster, String volumeName) {
    final String pvcName;
    if (DATA_VOLUME_NAME.equals(volumeName)) {
      pvcName = StackGresUtil.statefulSetDataPersistentVolumeClaimName(cluster)
          + "-" + podName;
    } else {
      pvcName = CustomPersistentVolumeUtil.volumeName(volumeName) + "-" + podName;
    }
    return Optional.ofNullable(client.persistentVolumeClaims()
        .inNamespace(cluster.getMetadata().getNamespace())
        .withName(pvcName)
        .get())
        .map(PersistentVolumeClaim::getMetadata)
        .map(metadata -> metadata.getUid())
        .orElse(null);
  }

  private void sendEvent(KubernetesClient client, ClusterControllerEventReason reason,
      String message) {
    try {
      eventController.sendEvent(reason, message, client);
    } catch (Exception ex) {
      LOGGER.error("An error occurred while sending an event", ex);
    }
  }

}
