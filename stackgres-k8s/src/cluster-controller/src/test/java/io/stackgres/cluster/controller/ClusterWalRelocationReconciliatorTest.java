/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.cluster.common.ClusterControllerEventReason;
import io.stackgres.cluster.common.StackGresClusterContext;
import io.stackgres.cluster.configuration.ClusterControllerPropertyContext;
import io.stackgres.common.ClusterControllerProperty;
import io.stackgres.common.crd.CustomVolumeMount;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurationsPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterPods;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodsCustomPersistentVolume;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClusterWalRelocationReconciliatorTest {

  private static final String POD_NAME = "test-0";

  @Mock
  private EventController eventController;

  @Mock
  private ClusterControllerPropertyContext propertyContext;

  @Mock
  private StackGresClusterContext context;

  @Mock
  private KubernetesClient client;

  @TempDir
  private Path tempDir;

  private ClusterWalRelocationReconciliator reconciliator;

  private StackGresCluster cluster;

  private Path pgDataPath;

  private Path walVolumePath;

  @BeforeEach
  void setUp() throws Exception {
    when(propertyContext.getString(ClusterControllerProperty.CLUSTER_CONTROLLER_POD_NAME))
        .thenReturn(POD_NAME);
    var parameters = new ClusterWalRelocationReconciliator.Parameters();
    parameters.eventController = eventController;
    parameters.propertyContext = propertyContext;
    reconciliator = spy(new ClusterWalRelocationReconciliator(parameters));
    pgDataPath = tempDir.resolve("data-volume").resolve("data");
    walVolumePath = tempDir.resolve("custom-persistent-volumes").resolve("wal");
    reconciliator.pgDataPath = pgDataPath;
    reconciliator.patroniStartFilePath = tempDir.resolve(".start-patroni");
    cluster = buildCluster();
    lenient().when(context.getCluster()).thenReturn(cluster);
  }

  private StackGresCluster buildCluster() {
    StackGresCluster cluster = new StackGresCluster();
    cluster.setSpec(new StackGresClusterSpec());
    cluster.getSpec().setPods(new StackGresClusterPods());
    var wal = new StackGresClusterPodsCustomPersistentVolume();
    wal.setName("wal");
    wal.setSize("1Gi");
    wal.setCoherentData(true);
    cluster.getSpec().getPods().setCustomPersistentVolumes(List.of(wal));
    var walMount = new CustomVolumeMount();
    walMount.setName("custom-wal");
    walMount.setMountPath("/custom-wal");
    cluster.getSpec().getPods().setCustomVolumeMounts(Map.of("patroni", List.of(walMount)));
    return cluster;
  }

  private void setSpecWalPath(String walPath) {
    if (cluster.getSpec().getConfigurations() == null) {
      cluster.getSpec().setConfigurations(new StackGresClusterConfigurations());
    }
    if (cluster.getSpec().getConfigurations().getPostgres() == null) {
      cluster.getSpec().getConfigurations().setPostgres(
          new StackGresClusterConfigurationsPostgres());
    }
    cluster.getSpec().getConfigurations().getPostgres().setWalPath(walPath);
  }

  private void createBootstrappedPgData() throws IOException {
    Files.createDirectories(pgDataPath.resolve("pg_wal").resolve("archive_status"));
    Files.writeString(pgDataPath.resolve("pg_wal").resolve("000000010000000000000001"),
        "wal segment content");
    Files.writeString(pgDataPath.resolve("PG_VERSION"), "17");
  }

  private ClusterWalRelocationReconciliator.WalRelocationResult reconcile() {
    var result = reconciliator.safeReconcile(client, context);
    assertTrue(result.success(), () -> result.getExceptions().toString());
    return result.result().orElseThrow();
  }

  private Optional<String> podStatusWalPath() {
    return Optional.ofNullable(cluster.getStatus())
        .map(StackGresClusterStatus::getPodStatuses)
        .stream()
        .flatMap(List::stream)
        .filter(podStatus -> POD_NAME.equals(podStatus.getName()))
        .findFirst()
        .map(StackGresClusterPodStatus::getWalPath);
  }

  @Test
  void notBootstrappedInstance_shouldPassWithoutStatusUpdate() {
    var result = reconcile();
    assertFalse(result.clusterUpdated());
    assertTrue(result.patroniStartAllowed());
  }

  @Test
  void bootstrappedInstanceWithDefaultLayout_shouldReportNoWalPath() throws IOException {
    createBootstrappedPgData();
    var result = reconcile();
    assertTrue(result.patroniStartAllowed());
    assertTrue(result.clusterUpdated());
    assertNull(podStatusWalPath().orElse(null));
    // second cycle is a no-op
    var secondResult = reconcile();
    assertFalse(secondResult.clusterUpdated());
  }

  @Test
  void bootstrappedInstanceWithSymlink_shouldReportWalPath() throws IOException {
    createBootstrappedPgData();
    setSpecWalPath(tempDir.resolve("wal-dir").resolve("pg-wal").toString());
    relocateForTest(tempDir.resolve("wal-dir").resolve("pg-wal"));
    var result = reconcile();
    assertTrue(result.patroniStartAllowed());
    assertTrue(result.clusterUpdated());
    assertEquals(tempDir.resolve("wal-dir").resolve("pg-wal").toString(),
        podStatusWalPath().orElseThrow());
  }

  private void relocateForTest(Path target) throws IOException {
    // relocate manually: move pg_wal content to target and replace it with a symlink
    Files.createDirectories(target.getParent());
    Files.move(pgDataPath.resolve("pg_wal"), target);
    Files.createSymbolicLink(pgDataPath.resolve("pg_wal"), target);
  }

  @Test
  void walPathSetOnDataVolume_shouldRelocateDirectoryToSymlink() throws IOException {
    createBootstrappedPgData();
    // a path under the data volume passes through the patroni to controller translation:
    // use a path under the data volume base path
    Path walPath = pgDataPath.getParent().resolve("wal-dir").resolve("pg-wal");
    setSpecWalPath(walPath.toString());
    doReturn(Optional.of(walPath)).when(reconciliator).translateToController(
        any(), eq(walPath.toString()));
    var result = reconcile();
    assertTrue(result.patroniStartAllowed());
    assertTrue(result.clusterUpdated());
    assertTrue(Files.isSymbolicLink(pgDataPath.resolve("pg_wal")));
    assertEquals(walPath, Files.readSymbolicLink(pgDataPath.resolve("pg_wal")));
    assertTrue(Files.exists(walPath.resolve("000000010000000000000001")));
    assertTrue(Files.isDirectory(walPath.resolve("archive_status")));
    assertEquals(walPath.toString(), podStatusWalPath().orElseThrow());
    assertFalse(Files.exists(pgDataPath.resolve("pg_wal.old")));
    assertFalse(Files.exists(pgDataPath.resolve("pg_wal.new")));
  }

  @Test
  void walPathUnset_shouldRelocateSymlinkToDirectory() throws IOException {
    createBootstrappedPgData();
    Path walPath = pgDataPath.getParent().resolve("wal-dir").resolve("pg-wal");
    relocateForTest(walPath);
    doReturn(Optional.of(walPath)).when(reconciliator).translateToController(
        any(), eq(walPath.toString()));
    var result = reconcile();
    assertTrue(result.patroniStartAllowed());
    assertFalse(Files.isSymbolicLink(pgDataPath.resolve("pg_wal")));
    assertTrue(Files.isDirectory(pgDataPath.resolve("pg_wal")));
    assertTrue(Files.exists(pgDataPath.resolve("pg_wal")
        .resolve("000000010000000000000001")));
    assertNull(podStatusWalPath().orElse(null));
    assertTrue(directoryIsEmpty(walPath));
  }

  private boolean directoryIsEmpty(Path path) throws IOException {
    try (var content = Files.list(path)) {
      return content.findAny().isEmpty();
    }
  }

  @Test
  void walPathChanged_shouldRelocateSymlinkToSymlink() throws IOException {
    createBootstrappedPgData();
    Path oldWalPath = pgDataPath.getParent().resolve("wal-dir").resolve("pg-wal");
    Path newWalPath = pgDataPath.getParent().resolve("wal-dir-2").resolve("pg-wal");
    relocateForTest(oldWalPath);
    setSpecWalPath(newWalPath.toString());
    doReturn(Optional.of(oldWalPath)).when(reconciliator).translateToController(
        any(), eq(oldWalPath.toString()));
    doReturn(Optional.of(newWalPath)).when(reconciliator).translateToController(
        any(), eq(newWalPath.toString()));
    var result = reconcile();
    assertTrue(result.patroniStartAllowed());
    assertTrue(Files.isSymbolicLink(pgDataPath.resolve("pg_wal")));
    assertEquals(newWalPath, Files.readSymbolicLink(pgDataPath.resolve("pg_wal")));
    assertTrue(Files.exists(newWalPath.resolve("000000010000000000000001")));
    assertTrue(directoryIsEmpty(oldWalPath));
    assertEquals(newWalPath.toString(), podStatusWalPath().orElseThrow());
  }

  @Test
  void patroniAlreadySignaled_shouldOnlyUpdateStatus() throws IOException {
    createBootstrappedPgData();
    Files.createFile(reconciliator.patroniStartFilePath);
    Path walPath = pgDataPath.getParent().resolve("wal-dir").resolve("pg-wal");
    setSpecWalPath(walPath.toString());
    var result = reconcile();
    assertTrue(result.patroniStartAllowed());
    assertTrue(result.clusterUpdated());
    assertFalse(Files.isSymbolicLink(pgDataPath.resolve("pg_wal")));
    assertNull(podStatusWalPath().orElse(null));
  }

  @Test
  void notEnoughFreeSpace_shouldBlock() throws IOException {
    createBootstrappedPgData();
    Path walPath = pgDataPath.getParent().resolve("wal-dir").resolve("pg-wal");
    setSpecWalPath(walPath.toString());
    doReturn(Optional.of(walPath)).when(reconciliator).translateToController(
        any(), eq(walPath.toString()));
    doReturn(0L).when(reconciliator).usableSpace(any());
    var result = reconcile();
    assertFalse(result.patroniStartAllowed());
    assertFalse(Files.isSymbolicLink(pgDataPath.resolve("pg_wal")));
    verify(eventController).sendEvent(
        eq(ClusterControllerEventReason.CLUSTER_WAL_RELOCATION_FAILED),
        anyString(), any());
  }

  @Test
  void untranslatableWalPath_shouldBlock() throws IOException {
    createBootstrappedPgData();
    setSpecWalPath("/somewhere/else/pg-wal");
    var result = reconcile();
    assertFalse(result.patroniStartAllowed());
    verify(eventController).sendEvent(
        eq(ClusterControllerEventReason.CLUSTER_WAL_RELOCATION_FAILED),
        anyString(), any());
  }

  @Test
  void crashAfterCopy_shouldResume() throws IOException {
    createBootstrappedPgData();
    Path walPath = pgDataPath.getParent().resolve("wal-dir").resolve("pg-wal");
    setSpecWalPath(walPath.toString());
    doReturn(Optional.of(walPath)).when(reconciliator).translateToController(
        any(), eq(walPath.toString()));
    // simulate a crash after the copy was atomically renamed to the destination
    Files.createDirectories(walPath);
    Files.writeString(walPath.resolve("000000010000000000000001"), "wal segment content");
    var result = reconcile();
    assertTrue(result.patroniStartAllowed());
    assertTrue(Files.isSymbolicLink(pgDataPath.resolve("pg_wal")));
    assertEquals(walPath.toString(), podStatusWalPath().orElseThrow());
  }

  @Test
  void crashAfterFirstRename_shouldResume() throws IOException {
    createBootstrappedPgData();
    Path walPath = pgDataPath.getParent().resolve("wal-dir").resolve("pg-wal");
    setSpecWalPath(walPath.toString());
    lenient().doReturn(Optional.of(walPath)).when(reconciliator).translateToController(
        any(), eq(walPath.toString()));
    // simulate a crash after pg_wal was renamed to pg_wal.old and pg_wal.new was created
    Files.createDirectories(walPath);
    Files.writeString(walPath.resolve("000000010000000000000001"), "wal segment content");
    Files.move(pgDataPath.resolve("pg_wal"), pgDataPath.resolve("pg_wal.old"));
    Files.createSymbolicLink(pgDataPath.resolve("pg_wal.new"), walPath);
    var result = reconcile();
    assertTrue(result.patroniStartAllowed());
    assertTrue(Files.isSymbolicLink(pgDataPath.resolve("pg_wal")));
    assertFalse(Files.exists(pgDataPath.resolve("pg_wal.old")));
    assertFalse(Files.exists(pgDataPath.resolve("pg_wal.new"),
        java.nio.file.LinkOption.NOFOLLOW_LINKS));
  }

  @Test
  void crashWithPartialRelocatingCopy_shouldRestartCopy() throws IOException {
    createBootstrappedPgData();
    Path walPath = pgDataPath.getParent().resolve("wal-dir").resolve("pg-wal");
    setSpecWalPath(walPath.toString());
    doReturn(Optional.of(walPath)).when(reconciliator).translateToController(
        any(), eq(walPath.toString()));
    // simulate a crash in the middle of the copy
    Files.createDirectories(walPath.getParent().resolve("pg-wal.relocating"));
    Files.writeString(walPath.getParent().resolve("pg-wal.relocating").resolve("garbage"),
        "partial content");
    var result = reconcile();
    assertTrue(result.patroniStartAllowed());
    assertTrue(Files.isSymbolicLink(pgDataPath.resolve("pg_wal")));
    assertTrue(Files.exists(walPath.resolve("000000010000000000000001")));
    assertFalse(Files.exists(walPath.getParent().resolve("pg-wal.relocating")));
  }

}
