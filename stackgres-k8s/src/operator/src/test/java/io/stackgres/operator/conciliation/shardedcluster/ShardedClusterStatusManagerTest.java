/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresProperty;
import io.stackgres.common.crd.Condition;
import io.stackgres.common.crd.sgcluster.ClusterStatusCondition;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.crd.sgshardedcluster.ShardedClusterStatusCondition;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterStatus;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.LabelFactoryForShardedCluster;
import io.stackgres.common.labels.ShardedClusterLabelFactory;
import io.stackgres.common.labels.ShardedClusterLabelMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShardedClusterStatusManagerTest {

  private static final String OPERATOR_VERSION =
      StackGresProperty.OPERATOR_VERSION.getString();

  private final LabelFactoryForShardedCluster labelFactory =
      new ShardedClusterLabelFactory(new ShardedClusterLabelMapper());

  @Mock
  private KubernetesClient client;

  @Mock
  private MixedOperation<StackGresCluster, KubernetesResourceList<StackGresCluster>,
      Resource<StackGresCluster>> clusterOperation;

  @Mock
  private NonNamespaceOperation<StackGresCluster, KubernetesResourceList<StackGresCluster>,
      Resource<StackGresCluster>> clusterNamespaceOperation;

  @Mock
  private FilterWatchListDeletable<StackGresCluster, KubernetesResourceList<StackGresCluster>,
      Resource<StackGresCluster>> clusterLabelledOperation;

  @Mock
  private KubernetesResourceList<StackGresCluster> clusterList;

  private ShardedClusterStatusManager statusManager;

  private StackGresShardedCluster shardedCluster;

  private StackGresCluster coordinator;

  @BeforeEach
  void setUp() {
    statusManager = new ShardedClusterStatusManager(labelFactory, client);

    shardedCluster = Fixtures.shardedCluster().loadDefault().get();
    shardedCluster.setStatus(new StackGresShardedClusterStatus());
    shardedCluster.getMetadata().setAnnotations(new HashMap<>());
    setOperatorVersion(shardedCluster, OPERATOR_VERSION);

    coordinator = Fixtures.cluster().loadDefault().get();
    coordinator.getMetadata().setName(shardedCluster.getMetadata().getName() + "-coord");
    coordinator.getMetadata().setAnnotations(new HashMap<>());
    coordinator.getMetadata().setOwnerReferences(List.of(
        new OwnerReferenceBuilder()
        .withKind(StackGresShardedCluster.KIND)
        .withName(shardedCluster.getMetadata().getName())
        .build()));

    lenient().when(client.resources(StackGresCluster.class)).thenReturn(clusterOperation);
    lenient().when(clusterOperation.inNamespace(anyString()))
        .thenReturn(clusterNamespaceOperation);
    lenient().when(clusterNamespaceOperation.withLabels(anyMap()))
        .thenReturn(clusterLabelledOperation);
    lenient().when(clusterLabelledOperation.list()).thenReturn(clusterList);
    lenient().when(clusterList.getItems()).thenReturn(List.of(coordinator));
  }

  private void setOperatorVersion(HasMetadata resource, String version) {
    resource.getMetadata().getAnnotations().put(StackGresContext.VERSION_KEY, version);
  }

  private Optional<Condition> getCondition(ShardedClusterStatusCondition.Type type) {
    return Optional.ofNullable(shardedCluster.getStatus().getConditions())
        .orElse(List.of())
        .stream()
        .filter(condition -> type.getType().equals(condition.getType()))
        .findFirst();
  }

  private Optional<Condition> getPendingUpgradeCondition() {
    return getCondition(ShardedClusterStatusCondition.Type.PENDING_UPGRADE);
  }

  private Optional<Condition> getPendingRestartCondition() {
    return getCondition(ShardedClusterStatusCondition.Type.PENDING_RESTART);
  }

  private void setClusterCondition(StackGresCluster cluster, Condition condition) {
    if (cluster.getStatus() == null) {
      cluster.setStatus(new StackGresClusterStatus());
    }
    cluster.getStatus().setConditions(List.of(condition));
  }

  @Test
  void givenAnUpToDateShardedClusterAndChildren_shouldNotBePendingUpgrade() {
    statusManager.refreshCondition(shardedCluster);

    var condition = getPendingUpgradeCondition().orElseThrow();
    assertEquals("False", condition.getStatus());
    assertEquals("FalsePendingUpgrade", condition.getReason());
    assertNull(condition.getMessage());
  }

  @Test
  void givenAShardedClusterWithAnOlderOperatorVersion_shouldBePendingUpgrade() {
    setOperatorVersion(shardedCluster, "1.18.0");

    statusManager.refreshCondition(shardedCluster);

    var condition = getPendingUpgradeCondition().orElseThrow();
    assertEquals("True", condition.getStatus());
    assertEquals("ShardedClusterRequiresUpgrade", condition.getReason());
    assertTrue(condition.getMessage().contains("1.18.0"),
        "the message should name the operator version the resource was created with");
    assertTrue(condition.getMessage().contains("securityUpgrade"),
        "the message should tell how to clear the condition");
  }

  @Test
  void givenAShardedClusterWithAPatchLevelOperatorVersionDifference_shouldNotBePendingUpgrade() {
    setOperatorVersion(shardedCluster, OPERATOR_VERSION + "-rc5");

    statusManager.refreshCondition(shardedCluster);

    assertEquals("False", getPendingUpgradeCondition().orElseThrow().getStatus(),
        "a patch level difference of the operator version is not a pending upgrade");
  }

  @Test
  void givenAChildClusterPendingUpgrade_shouldBePendingUpgrade() {
    setClusterCondition(coordinator,
        ClusterStatusCondition.CLUSTER_REQUIRES_UPGRADE.getCondition());

    statusManager.refreshCondition(shardedCluster);

    var condition = getPendingUpgradeCondition().orElseThrow();
    assertEquals("True", condition.getStatus(),
        "the condition must be aggregated from the PendingUpgrade condition of the children");
    assertTrue(condition.getMessage().contains("1 SGCluster requires an upgrade"),
        "the message should count the SGClusters that require an upgrade: "
            + condition.getMessage());
  }

  @Test
  void givenAChildClusterNotPendingUpgrade_shouldNotBePendingUpgrade() {
    setClusterCondition(coordinator,
        ClusterStatusCondition.FALSE_PENDING_UPGRADE.getCondition());

    statusManager.refreshCondition(shardedCluster);

    assertEquals("False", getPendingUpgradeCondition().orElseThrow().getStatus());
  }

  @Test
  void givenAChildClusterPendingRestart_shouldBePendingRestart() {
    setClusterCondition(coordinator,
        ClusterStatusCondition.POD_REQUIRES_RESTART.getCondition());

    statusManager.refreshCondition(shardedCluster);

    var condition = getPendingRestartCondition().orElseThrow();
    assertEquals("True", condition.getStatus());
    assertTrue(condition.getMessage().contains("1 SGCluster requires a restart"),
        "the message should count the SGClusters that require a restart: "
            + condition.getMessage());
  }

  @Test
  void givenAChildClusterPendingRestartForAnotherReason_shouldBePendingRestart() {
    var pendingRestart = ClusterStatusCondition.POD_REQUIRES_RESTART.getCondition();
    pendingRestart.setReason("SomeOtherReason");
    setClusterCondition(coordinator, pendingRestart);

    statusManager.refreshCondition(shardedCluster);

    assertEquals("True", getPendingRestartCondition().orElseThrow().getStatus(),
        "only the type and the status of the children condition must be looked at");
  }

  @Test
  void givenNoChildClusterPendingRestart_shouldNotBePendingRestart() {
    setClusterCondition(coordinator,
        ClusterStatusCondition.FALSE_PENDING_RESTART.getCondition());

    statusManager.refreshCondition(shardedCluster);

    var condition = getPendingRestartCondition().orElseThrow();
    assertEquals("False", condition.getStatus());
    assertNull(condition.getMessage());
  }

  @Test
  void givenAShardedClusterWithoutOperatorVersion_shouldNotBePendingUpgrade() {
    shardedCluster.getMetadata().getAnnotations().remove(StackGresContext.VERSION_KEY);

    statusManager.refreshCondition(shardedCluster);

    assertEquals("False", getPendingUpgradeCondition().orElseThrow().getStatus());
  }
}
