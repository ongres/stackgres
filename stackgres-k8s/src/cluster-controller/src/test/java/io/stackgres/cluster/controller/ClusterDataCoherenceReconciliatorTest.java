/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.cluster.common.ClusterControllerEventReason;
import io.stackgres.cluster.common.StackGresClusterContext;
import io.stackgres.cluster.configuration.ClusterControllerPropertyContext;
import io.stackgres.common.ClusterControllerProperty;
import io.stackgres.common.crd.CustomVolumeMount;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPods;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodsCustomPersistentVolume;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClusterDataCoherenceReconciliatorTest {

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

  private ClusterDataCoherenceReconciliator reconciliator;

  private StackGresCluster cluster;

  private Path dataPath;

  private Path walPath;

  @BeforeEach
  void setUp() throws Exception {
    when(propertyContext.getString(ClusterControllerProperty.CLUSTER_CONTROLLER_POD_NAME))
        .thenReturn(POD_NAME);
    var parameters = new ClusterDataCoherenceReconciliator.Parameters();
    parameters.eventController = eventController;
    parameters.propertyContext = propertyContext;
    reconciliator = spy(new ClusterDataCoherenceReconciliator(parameters));
    dataPath = tempDir.resolve("data-volume");
    walPath = tempDir.resolve("custom-persistent-volumes").resolve("wal");
    Files.createDirectories(dataPath);
    Files.createDirectories(walPath);
    reconciliator.pgBasePath = dataPath;
    reconciliator.customVolumesBasePath = tempDir.resolve("custom-persistent-volumes");
    reconciliator.patroniStartFilePath = tempDir.resolve(".start-patroni");
    cluster = buildCluster();
    lenient().when(context.getCluster()).thenReturn(cluster);
    lenient().doReturn("data-pvc-uid").when(reconciliator)
        .resolvePvcUid(any(), any(), eq("data"));
    lenient().doReturn("wal-pvc-uid").when(reconciliator)
        .resolvePvcUid(any(), any(), eq("wal"));
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
    walMount.setSubPath("wal");
    cluster.getSpec().getPods().setCustomVolumeMounts(Map.of("patroni", List.of(walMount)));
    return cluster;
  }

  private boolean reconcile() {
    var result = reconciliator.safeReconcile(client, context);
    assertTrue(result.success(), () -> result.getExceptions().toString());
    return result.result().orElseThrow();
  }

  private boolean reconcileAllowingFailure() {
    return reconciliator.safeReconcile(client, context).result().orElse(false);
  }

  @Test
  void freshInstance_shouldPassAndInitializeMarkers() throws IOException {
    assertTrue(reconcile());
    assertTrue(Files.exists(dataPath.resolve(".data-coherent-id")));
    assertTrue(Files.exists(walPath.resolve(".data-coherent-id")));
    assertTrue(Files.exists(dataPath.resolve(".data-coherent-mount-paths")));
    assertTrue(Files.exists(walPath.resolve(".data-coherent-mount-paths")));
    assertEquals(
        Files.readAllLines(dataPath.resolve(".data-coherent-id")).get(0),
        Files.readAllLines(walPath.resolve(".data-coherent-id")).get(0));
    assertEquals("data-pvc-uid",
        Files.readAllLines(dataPath.resolve(".data-coherent-id")).get(1));
    assertEquals("wal-pvc-uid",
        Files.readAllLines(walPath.resolve(".data-coherent-id")).get(1));
    assertEquals(
        Files.readString(dataPath.resolve(".data-coherent-mount-paths")),
        Files.readString(walPath.resolve(".data-coherent-mount-paths")));
    assertTrue(Files.readString(dataPath.resolve(".data-coherent-mount-paths"))
        .contains("wal\t/custom-wal\twal"));
    assertTrue(Files.isDirectory(walPath.resolve("wal")));
  }

  @Test
  void noCoherentVolumes_shouldPassWithoutMarkers() throws IOException {
    cluster.getSpec().getPods().setCustomPersistentVolumes(List.of());
    assertTrue(reconcile());
    assertFalse(Files.exists(dataPath.resolve(".data-coherent-id")));
  }

  @Test
  void patroniAlreadySignaled_shouldPassWithoutTouchingTheFilesystem() throws IOException {
    Files.createFile(reconciliator.patroniStartFilePath);
    assertTrue(reconcile());
    assertFalse(Files.exists(dataPath.resolve(".data-coherent-id")));
  }

  @Test
  void deletedCustomPvc_shouldBlock() throws IOException {
    assertTrue(reconcile());
    // simulate the wal PVC being deleted and re-created empty
    Files.delete(walPath.resolve(".data-coherent-id"));
    Files.delete(walPath.resolve(".data-coherent-mount-paths"));
    assertFalse(reconcile());
    verify(eventController).sendEvent(
        eq(ClusterControllerEventReason.CLUSTER_DATA_COHERENCE_BLOCKED), anyString(), any());
  }

  @Test
  void deletedDataPvc_shouldBlock() throws IOException {
    assertTrue(reconcile());
    // simulate the data PVC being deleted and re-created empty
    Files.delete(dataPath.resolve(".data-coherent-id"));
    Files.delete(dataPath.resolve(".data-coherent-mount-paths"));
    assertFalse(reconcile());
    verify(eventController).sendEvent(
        eq(ClusterControllerEventReason.CLUSTER_DATA_COHERENCE_BLOCKED), anyString(), any());
  }

  @Test
  void volumeOfAnotherCoherentSet_shouldBlock() throws IOException {
    assertTrue(reconcile());
    Files.writeString(walPath.resolve(".data-coherent-id"),
        "another-coherence-id\nwal-pvc-uid\n");
    assertFalse(reconcile());
    verify(eventController).sendEvent(
        eq(ClusterControllerEventReason.CLUSTER_DATA_COHERENCE_BLOCKED), anyString(), any());
  }

  @Test
  void subPathChange_shouldBlock() throws IOException {
    assertTrue(reconcile());
    cluster.getSpec().getPods().getCustomVolumeMounts()
        .get("patroni").get(0).setSubPath("wal-2");
    assertFalse(reconcile());
    verify(eventController).sendEvent(
        eq(ClusterControllerEventReason.CLUSTER_DATA_COHERENCE_BLOCKED), anyString(), any());
  }

  @Test
  void newMemberAdded_shouldPassAndUpdateMarkers() throws IOException {
    var noCustomVolumesCluster = buildCluster();
    noCustomVolumesCluster.getSpec().getPods().setCustomPersistentVolumes(List.of());
    when(context.getCluster()).thenReturn(noCustomVolumesCluster);
    assertTrue(reconcile());
    // The set only had the data volume (feature inactive), now the wal volume is added
    when(context.getCluster()).thenReturn(cluster);
    assertTrue(reconcile());
    assertTrue(Files.exists(walPath.resolve(".data-coherent-id")));
    assertTrue(Files.readString(dataPath.resolve(".data-coherent-mount-paths"))
        .contains("wal\t/custom-wal\twal"));
  }

  @Test
  void memberRemovedFromSpec_shouldPassAndReconcileLists() throws IOException {
    assertTrue(reconcile());
    cluster.getSpec().getPods().getCustomPersistentVolumes().get(0).setCoherentData(false);
    var otherVolume = new StackGresClusterPodsCustomPersistentVolume();
    otherVolume.setName("other");
    otherVolume.setSize("1Gi");
    otherVolume.setCoherentData(true);
    Files.createDirectories(tempDir.resolve("custom-persistent-volumes").resolve("other"));
    lenient().doReturn("other-pvc-uid").when(reconciliator)
        .resolvePvcUid(any(), any(), eq("other"));
    var otherMount = new CustomVolumeMount();
    otherMount.setName("custom-other");
    otherMount.setMountPath("/custom-other");
    cluster.getSpec().getPods().setCustomPersistentVolumes(List.of(
        cluster.getSpec().getPods().getCustomPersistentVolumes().get(0),
        otherVolume));
    cluster.getSpec().getPods().setCustomVolumeMounts(Map.of("patroni", List.of(
        cluster.getSpec().getPods().getCustomVolumeMounts().get("patroni").get(0),
        otherMount)));
    assertTrue(reconcile());
    assertFalse(Files.readString(dataPath.resolve(".data-coherent-mount-paths"))
        .contains("wal\t/custom-wal\twal"));
    assertTrue(Files.readString(dataPath.resolve(".data-coherent-mount-paths"))
        .contains("other\t/custom-other\t"));
  }

  @Test
  void allowCoherentDataRemoval_shouldReinitializeInsteadOfBlocking() throws IOException {
    assertTrue(reconcile());
    Files.delete(walPath.resolve(".data-coherent-id"));
    Files.delete(walPath.resolve(".data-coherent-mount-paths"));
    cluster.getSpec().getPods().getCustomPersistentVolumes().get(0)
        .setAllowCoherentDataRemoval(true);
    assertTrue(reconcile());
    verify(eventController).sendEvent(
        eq(ClusterControllerEventReason.CLUSTER_DATA_COHERENCE_REINITIALIZED),
        anyString(), any());
    assertTrue(Files.exists(walPath.resolve(".data-coherent-id")));
    assertEquals(
        Files.readAllLines(dataPath.resolve(".data-coherent-id")).get(0),
        Files.readAllLines(walPath.resolve(".data-coherent-id")).get(0));
  }

  @Test
  void restoredPvc_shouldReinitializeInsteadOfBlocking() throws IOException {
    assertTrue(reconcile());
    // simulate a restore from a VolumeSnapshot: the data volume content (markers included)
    // is restored on a new PersistentVolumeClaim while the wal volume is fresh
    Files.delete(walPath.resolve(".data-coherent-id"));
    Files.delete(walPath.resolve(".data-coherent-mount-paths"));
    doReturn("restored-data-pvc-uid").when(reconciliator)
        .resolvePvcUid(any(), any(), eq("data"));
    assertTrue(reconcile());
    verify(eventController).sendEvent(
        eq(ClusterControllerEventReason.CLUSTER_DATA_COHERENCE_REINITIALIZED),
        anyString(), any());
    assertEquals("restored-data-pvc-uid",
        Files.readAllLines(dataPath.resolve(".data-coherent-id")).get(1));
    assertTrue(Files.exists(walPath.resolve(".data-coherent-id")));
  }

  @Test
  void unmountedCoherentVolume_shouldBlock() throws IOException {
    var otherVolume = new StackGresClusterPodsCustomPersistentVolume();
    otherVolume.setName("unmounted");
    otherVolume.setSize("1Gi");
    otherVolume.setCoherentData(true);
    var otherMount = new CustomVolumeMount();
    otherMount.setName("custom-unmounted");
    otherMount.setMountPath("/custom-unmounted");
    cluster.getSpec().getPods().setCustomPersistentVolumes(List.of(otherVolume));
    cluster.getSpec().getPods().setCustomVolumeMounts(Map.of("patroni", List.of(otherMount)));
    assertFalse(reconcile());
    verify(eventController).sendEvent(
        eq(ClusterControllerEventReason.CLUSTER_DATA_COHERENCE_BLOCKED), anyString(), any());
  }

  @Test
  void steadyState_shouldNotChangeMarkers() throws IOException {
    assertTrue(reconcile());
    var idContent = Files.readString(dataPath.resolve(".data-coherent-id"));
    var mountPathsContent = Files.readString(dataPath.resolve(".data-coherent-mount-paths"));
    assertTrue(reconcile());
    assertEquals(idContent, Files.readString(dataPath.resolve(".data-coherent-id")));
    assertEquals(mountPathsContent,
        Files.readString(dataPath.resolve(".data-coherent-mount-paths")));
    verify(eventController, never()).sendEvent(any(), anyString(), any());
  }

  @Test
  void blockedCheck_shouldNotChangeMarkers() throws IOException {
    assertTrue(reconcile());
    Files.delete(walPath.resolve(".data-coherent-id"));
    var mountPathsContent = Files.readString(dataPath.resolve(".data-coherent-mount-paths"));
    assertFalse(reconcileAllowingFailure());
    assertFalse(Files.exists(walPath.resolve(".data-coherent-id")));
    assertEquals(mountPathsContent,
        Files.readString(dataPath.resolve(".data-coherent-mount-paths")));
  }

}
