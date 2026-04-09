/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.controller;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.cluster.common.ClusterControllerEventReason;
import io.stackgres.cluster.common.StackGresClusterContext;
import io.stackgres.cluster.configuration.ClusterControllerPropertyContext;
import io.stackgres.common.ClusterControllerProperty;
import io.stackgres.common.ClusterPath;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPods;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodsPersistentVolume;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodsPersistentVolumeIoLimits;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.operatorframework.reconciliation.ReconciliationResult;
import io.stackgres.operatorframework.reconciliation.SafeReconciliator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class IoLimitsReconciliator extends SafeReconciliator<StackGresClusterContext, Void> {

  private static final Logger LOGGER = LoggerFactory.getLogger(IoLimitsReconciliator.class);

  private static final Path PG_BASE_PATH =
      Paths.get(ClusterPath.PG_BASE_PATH.path());
  private static final Path PROC_PATH = Path.of("/proc");
  private static final Path HOST_CGROUP_PATH = Path.of(ClusterPath.HOST_CGROUP_PATH.path());

  private static final long MEBI_BYTES = 1024L * 1024L;

  private final EventController eventController;
  private final boolean isIoLimitsSet;
  private final String podUid;

  String deviceMajMin = null;
  Path ioMaxPath = null;

  @Dependent
  public static class Parameters {
    @Inject EventController eventController;
    @Inject ClusterControllerPropertyContext propertyContext;
  }

  @Inject
  public IoLimitsReconciliator(Parameters parameters) {
    this.eventController = parameters.eventController;
    this.isIoLimitsSet = parameters.propertyContext.getBoolean(
        ClusterControllerProperty.CLUSTER_CONTROLLER_APPLY_IO_LIMITS);
    this.podUid = parameters.propertyContext.getString(
        ClusterControllerProperty.CLUSTER_CONTROLLER_POD_UID);
  }

  @Override
  public ReconciliationResult<Void> safeReconcile(KubernetesClient client,
      StackGresClusterContext context) {
    if (!this.isIoLimitsSet) {
      return new ReconciliationResult<>();
    }
    try {
      reconcileIoLimits(client, context);
      return new ReconciliationResult<>();
    } catch (IOException | RuntimeException ex) {
      LOGGER.error("An error occurred while reconciling patroni", ex);
      try {
        eventController.sendEvent(ClusterControllerEventReason.CLUSTER_CONTROLLER_ERROR,
            "An error occurred while reconciling I/O limits: " + ex.getMessage(),
            client);
      } catch (Exception eventEx) {
        LOGGER.error("An error occurred while sending an event", eventEx);
      }
      return new ReconciliationResult<>(ex);
    }
  }

  private void reconcileIoLimits(KubernetesClient client, StackGresClusterContext context)
      throws IOException {
    final var ioLimits = Optional.of(context.getCluster())
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getPods)
        .map(StackGresClusterPods::getPersistentVolume)
        .map(StackGresClusterPodsPersistentVolume::getIoLimits);
    final Long rbps = ioLimits
        .map(StackGresClusterPodsPersistentVolumeIoLimits::getReadMiBps)
        .map(this::toBps)
        .orElse(null);
    final Long wbps = ioLimits
        .map(StackGresClusterPodsPersistentVolumeIoLimits::getWriteMiBps)
        .map(this::toBps)
        .orElse(null);
    final Integer riops = ioLimits
        .map(StackGresClusterPodsPersistentVolumeIoLimits::getReadIops)
        .orElse(null);
    final Integer wiops = ioLimits
        .map(StackGresClusterPodsPersistentVolumeIoLimits::getWriteIops)
        .orElse(null);
    if (deviceMajMin == null) {
      deviceMajMin = resolveDeviceMajMin(PG_BASE_PATH);
      LOGGER.info("Device mounted at " + PG_BASE_PATH + " has maj:min " + deviceMajMin);
    }
    if (ioMaxPath == null) {
      ioMaxPath = resolveCgroupIoMaxPath(podUid);
      LOGGER.info("Found io.max path for Pod with uid " + podUid + " at " + ioMaxPath);
    }
    String desiredLine = formatIoMaxLine(deviceMajMin, rbps, wbps, riops, wiops);
    if (isAlreadyApplied(ioMaxPath, desiredLine, deviceMajMin, rbps, wbps, riops, wiops)) {
      return;
    }
    try (var os = new FileOutputStream(ioMaxPath.toFile())) {
      os.write((desiredLine + "\n").getBytes(StandardCharsets.UTF_8));
    }
    LOGGER.info("Set io.max to " + desiredLine);
  }

  private Long toBps(Integer mbps) {
    return mbps * MEBI_BYTES;
  }

  /**
   * Resolves the major:minor device number for the block device mounted at
   * {@code mountPath}.
   * Parses /proc/self/mountinfo.
   *
   * <p>
   * mountinfo line format (space-separated):
   * 36 35 98:0 /mnt1 /mnt2 rw,noatime master:1 - ext3 /dev/root
   * rw,errors=continue
   * [0] [1] [2] [3] [4] [5] ... - fstype source options
   * </p>
   *
   * <p>
   * Field index 2 is major:minor, field index 4 is the mount point.
   * We use longest-prefix matching so that /data matches /data even when the
   * caller passes /data/subdir.
   * </p>
   */
  String resolveDeviceMajMin(Path mountPath) throws IOException {
    Path mountInfoPath = PROC_PATH.resolve("self").resolve("mountinfo");
    if (!Files.exists(mountInfoPath)) {
      throw new RuntimeException("No mountinfo found for path: " + mountInfoPath);
    }

    List<String> lines = Files.readAllLines(mountInfoPath);
    Path bestMount = null;
    String bestMajMin = null;

    Path normalizedTarget = mountPath;

    for (String line : lines) {
      String[] fields = line.split("\\s+");
      if (fields.length < 6) {
        continue;
      }
      String majMin = fields[2];
      String mountPoint = fields[4];
      Path normalizedMount = Path.of(mountPoint);

      if (normalizedTarget.equals(normalizedMount)) {
        if (bestMount == null || normalizedMount.toString().length() > bestMount.toString().length()) {
          bestMount = normalizedMount;
          bestMajMin = majMin;
        }
      }
    }

    if (bestMajMin == null) {
      throw new RuntimeException("No mount entry found for path: " + mountPath);
    }
    Path partitionFile = Path.of("/sys/dev/block/" + bestMajMin + "/partition");
    if (Files.exists(partitionFile)) {
      bestMajMin = Files.readString(Path.of("/sys/dev/block/" + bestMajMin + "/../dev")).trim();
    }
    // Validate format
    String[] parts = bestMajMin.split(":");
    if (parts.length != 2) {
      throw new RuntimeException("Unexpected major:minor format: " + bestMajMin);
    }
    try {
      Integer.parseInt(parts[0]);
      Integer.parseInt(parts[1]);
    } catch (NumberFormatException ex) {
      throw new RuntimeException("Non-numeric major:minor: " + bestMajMin, ex);
    }

    return bestMajMin;
  }

  /**
   * Reads /host-cgroup to find the cgroup v2 path of the Pod and returns the
   * full path to the io.max file under host cgroupFs.
   */
  Path resolveCgroupIoMaxPath(String podUid) throws IOException {
    String podSliceSuffix = "pod" + podUid.replace("-", "_") + ".slice";

    try (var walker = Files.walk(HOST_CGROUP_PATH, 5)) {
      return walker
          .filter(p -> p.getFileName().toString().equals("io.max"))
          .filter(p -> p.getParent().getFileName().toString().endsWith(podSliceSuffix))
          .reduce((a, b) -> {
            throw new IllegalStateException(
                "Multiple io.max matches found: " + a + " and " + b);
          })
          .orElseThrow(() -> new IOException(
              "No io.max found for Pod UID " + podUid + " under " + HOST_CGROUP_PATH));
    }
  }

  /**
   * Formats the io.max line.
   * Example: "8:16 rbps=1048576 wbps=max riops=1000 wiops=max"
   */
  static String formatIoMaxLine(
      String majMin,
      Long rbps,
      Long wbps,
      Integer riops,
      Integer wiops) {
    return majMin
        + " rbps=" + valueOrMax(rbps)
        + " wbps=" + valueOrMax(wbps)
        + " riops=" + valueOrMax(riops)
        + " wiops=" + valueOrMax(wiops);
  }

  private static String valueOrMax(Number value) {
    return value == null ? "max" : value.toString();
  }

  /**
   * Checks if io.max already contains a line matching the desired configuration.
   */
  private boolean isAlreadyApplied(
      Path ioMaxPath,
      String desiredLine,
      String majMin,
      Long rbps,
      Long wbps,
      Integer riops,
      Integer wiops) throws IOException {
    if (!Files.exists(ioMaxPath)) {
      return false;
    }
    List<String> lines = Files.readAllLines(ioMaxPath);
    boolean deviceFound = false;
    final String devicePrefix = majMin + " ";
    for (String line : lines) {
      if (line.startsWith(devicePrefix)) {
        deviceFound = true;
      }
      if (line.trim().equals(desiredLine.trim())) {
        return true;
      }
    }
    return !deviceFound && rbps == null && wbps == null && riops == null && riops == null;
  }

}
