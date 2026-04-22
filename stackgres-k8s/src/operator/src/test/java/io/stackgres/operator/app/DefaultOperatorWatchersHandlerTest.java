/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.fabric8.kubernetes.api.model.DefaultKubernetesResourceList;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.AnyNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterList;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operator.common.ResourceWatcherFactory;
import io.stackgres.operator.conciliation.DeployedResourcesCache;
import io.stackgres.operator.conciliation.backup.BackupReconciliator;
import io.stackgres.operator.conciliation.cluster.ClusterReconciliator;
import io.stackgres.operator.conciliation.config.ConfigReconciliator;
import io.stackgres.operator.conciliation.dbops.DbOpsReconciliator;
import io.stackgres.operator.conciliation.distributedlogs.DistributedLogsReconciliator;
import io.stackgres.operator.conciliation.shardedbackup.ShardedBackupReconciliator;
import io.stackgres.operator.conciliation.shardedcluster.ShardedClusterReconciliator;
import io.stackgres.operator.conciliation.shardeddbops.ShardedDbOpsReconciliator;
import io.stackgres.operator.conciliation.stream.StreamReconciliator;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Fix-verification tests for {@link DefaultOperatorWatchersHandler}.
 *
 * <p>The handler used to offer no public API to prune stale entries when a DELETE event is
 * missed (e.g. the operator is disconnected while a CR is deleted externally; the watch
 * re-list does not synthesize a DELETED event). These tests verify that {@code resync()}
 * closes that gap by querying the API server, pruning map entries that are no longer live,
 * and invalidating their deployed-resource cache entries.</p>
 */
class DefaultOperatorWatchersHandlerTest {

  /**
   * T4 - missed DELETE is repaired by {@code resync()}.
   *
   * <p>Seeds the {@code clusters} map (via reflection) with two entries and stubs the
   * KubernetesClient so that a {@code list()} call returns only {@code clusterA}. After
   * calling {@code resync()}, {@code clusterB} must have been pruned from the cache and its
   * deployed-resource cache entries invalidated.</p>
   *
   * <p>Judgement call on stubbing: the Fabric8 fluent chain
   * {@code client.resources(...).inAnyNamespace().list()} is stubbed by returning a
   * single catch-all {@link MixedOperation} for every CR kind (via {@code any(Class.class)}).
   * For the {@link StackGresCluster} kind we then override the terminal
   * {@code list()} call to return the specific list containing only {@code clusterA}. This
   * keeps the harness compact while remaining tied to the exact call shape used by
   * production code. Other kinds' maps start empty, so the default empty-list stub is a
   * no-op for them.</p>
   */
  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void missedDeleteIsRepairedByResync() throws Exception {
    KubernetesClient client = Mockito.mock(KubernetesClient.class);
    DeployedResourcesCache deployedResourcesCache = Mockito.mock(DeployedResourcesCache.class);

    // Generic catch-all stub for every CR kind: list() returns an empty
    // DefaultKubernetesResourceList (so getItems() yields an empty list and resync() no-ops
    // for the 8 kinds we're not exercising in this test).
    MixedOperation anyOp = Mockito.mock(MixedOperation.class);
    AnyNamespaceOperation anyNs = Mockito.mock(AnyNamespaceOperation.class);
    DefaultKubernetesResourceList emptyList = new DefaultKubernetesResourceList();
    when(client.resources(any(Class.class), any(Class.class))).thenReturn(anyOp);
    when(anyOp.inAnyNamespace()).thenReturn(anyNs);
    when(anyNs.list()).thenReturn(emptyList);

    // Dedicated stub for StackGresCluster: list() returns only clusterA - simulating that
    // clusterB was deleted externally while the operator was disconnected.
    StackGresCluster clusterA = newCluster("ns", "A");
    StackGresClusterList clusterList = new StackGresClusterList();
    clusterList.setItems(List.of(clusterA));
    MixedOperation clusterOp = Mockito.mock(MixedOperation.class);
    NonNamespaceOperation clusterAnyNs = Mockito.mock(NonNamespaceOperation.class);
    when(client.resources(StackGresCluster.class, StackGresClusterList.class))
        .thenReturn(clusterOp);
    when(clusterOp.inAnyNamespace()).thenReturn(clusterAnyNs);
    when(clusterAnyNs.list()).thenReturn(clusterList);

    DefaultOperatorWatchersHandler handler = new DefaultOperatorWatchersHandler(
        client,
        Mockito.mock(ConfigReconciliator.class),
        Mockito.mock(ClusterReconciliator.class),
        Mockito.mock(DistributedLogsReconciliator.class),
        Mockito.mock(DbOpsReconciliator.class),
        Mockito.mock(BackupReconciliator.class),
        Mockito.mock(ShardedClusterReconciliator.class),
        Mockito.mock(ShardedBackupReconciliator.class),
        Mockito.mock(ShardedDbOpsReconciliator.class),
        Mockito.mock(StreamReconciliator.class),
        Mockito.mock(ResourceWatcherFactory.class),
        deployedResourcesCache);

    // Reflect `clusters` map and seed it with A and B (simulating prior ADDED events).
    Field clustersField = DefaultOperatorWatchersHandler.class.getDeclaredField("clusters");
    clustersField.setAccessible(true);
    Map<String, StackGresCluster> clusters =
        (Map<String, StackGresCluster>) clustersField.get(handler);
    assertNotNull(clusters);
    StackGresCluster clusterB = newCluster("ns", "B");
    clusters.put("ns.A", clusterA);
    clusters.put("ns.B", clusterB);
    assertEquals(2, clusters.size(), "precondition: both clusters seeded");

    // Invoke resync() via reflection so this test compiles on the parent branch before
    // OperatorWatchersHandler.resync() is added to the interface. When the fix is absent,
    // the lookup below fails cleanly with a descriptive message; when the fix lands, the
    // method is found and invoked normally.
    Method resyncMethod;
    try {
      resyncMethod = DefaultOperatorWatchersHandler.class.getMethod("resync");
    } catch (NoSuchMethodException e) {
      fail("S2 fix not applied: DefaultOperatorWatchersHandler.resync() is missing. "
          + "This assertion will pass once the production fix adds the public resync() method.");
      return; // unreachable, but clarifies control flow for the compiler
    }
    try {
      resyncMethod.invoke(handler);
    } catch (InvocationTargetException e) {
      throw new AssertionError("resync() threw", e.getCause());
    }

    // The stale cluster B must have been pruned, its deployed resources invalidated,
    // and cluster A must remain present.
    assertEquals(1, clusters.size(),
        "resync() should prune entries absent from the live list");
    assertTrue(clusters.containsKey("ns.A"),
        "cluster A must remain cached (still live in the API server)");
    assertFalse(clusters.containsKey("ns.B"),
        "cluster B must be pruned (absent from the live list = deleted externally)");
    verify(deployedResourcesCache).removeAll(same(clusterB));
    verifyNoMoreInteractions(deployedResourcesCache);

    System.out.println("[T4-FIXED] clusters map size after resync(): " + clusters.size()
        + " (expected 1 - missed DELETE repaired)");
  }

  /**
   * T5 - quantify per-entry size.
   *
   * <p>Loads a realistic {@link StackGresShardedCluster} fixture and measures the JSON-serialized
   * byte size, extrapolating to the total MB that would leak from 1000 missed deletes if
   * resync() were not called. Purely diagnostic.</p>
   */
  @Test
  void quantifyPerEntrySizeForShardedCluster() throws Exception {
    StackGresShardedCluster shardedCluster = Fixtures.shardedCluster().loadDefault().get();
    assertNotNull(shardedCluster);

    byte[] serialized = JsonUtil.jsonMapper().writeValueAsBytes(shardedCluster);
    int perEntryBytes = serialized.length;
    assertTrue(perEntryBytes > 0, "fixture should serialize to a non-empty byte array");

    long missedDeletes = 1_000L;
    long leakedBytes = (long) perEntryBytes * missedDeletes;
    double leakedMegabytes = leakedBytes / (1024.0 * 1024.0);

    System.out.println("[T5] StackGresShardedCluster JSON size (bytes): " + perEntryBytes);
    System.out.println("[T5] StackGresShardedCluster JSON size (KB):    "
        + String.format(Locale.ROOT, "%.2f", perEntryBytes / 1024.0));
    System.out.println("[T5] Extrapolated leak from " + missedDeletes
        + " missed deletes (bytes): " + leakedBytes);
    System.out.println("[T5] Extrapolated leak from " + missedDeletes
        + " missed deletes (MB):    "
        + String.format(Locale.ROOT, "%.2f", leakedMegabytes));
    System.out.println("[T5] Note: in-memory JVM retention typically exceeds the JSON size due to "
        + "object headers, pointer padding and list/map wrappers; JSON size is a conservative "
        + "lower bound.");
  }

  private static StackGresCluster newCluster(String namespace, String name) {
    StackGresCluster cluster = new StackGresCluster();
    ObjectMeta metadata = new ObjectMeta();
    metadata.setNamespace(namespace);
    metadata.setName(name);
    cluster.setMetadata(metadata);
    return cluster;
  }
}
