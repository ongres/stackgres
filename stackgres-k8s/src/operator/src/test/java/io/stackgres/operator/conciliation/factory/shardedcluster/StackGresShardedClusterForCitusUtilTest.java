/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardedcluster;

import static io.stackgres.common.StackGresShardedClusterUtil.coordinatorConfigName;
import static io.stackgres.operator.conciliation.factory.shardedcluster.StackGresShardedClusterForCitusUtil.getCoordinatorCluster;
import static io.stackgres.operator.conciliation.factory.shardedcluster.StackGresShardedClusterForCitusUtil.getQueryRouterCluster;
import static io.stackgres.operator.conciliation.factory.shardedcluster.StackGresShardedClusterForCitusUtil.getWorkerCluster;
import static io.stackgres.testutil.ModelTestUtil.createWithRandomData;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresShardedClusterUtil;
import io.stackgres.common.crd.CustomServicePortBuilder;
import io.stackgres.common.crd.SecretKeySelector;
import io.stackgres.common.crd.postgres.service.StackGresPostgresServiceBuilder;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtensionBuilder;
import io.stackgres.common.crd.sgcluster.StackGresClusterPods;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgresBuilder;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFromCustomRestoreMethod;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplication;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterCitusConfigurations;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterConfigurations;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterCoordinator;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterCoordinatorConfigurations;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterPostgresServicesBuilder;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterReplicateFrom;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterReplicateFromInstance;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterWorkers;
import io.stackgres.common.crd.sgshardedcluster.StackGresWorkerType;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.testutil.JsonUtil;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class StackGresShardedClusterForCitusUtilTest {

  @Test
  void givedMinimalShardedCluster_shouldGenerateCoordinatorCluster() {
    var shardedCluster = getMinimalShardedCluster();
    var cluster = getCoordinatorCluster(JsonUtil.copy(shardedCluster), Optional.empty());
    checkCoordinatorWithGlobalSettings(
        shardedCluster,
        shardedCluster.getSpec().getCoordinator(),
        shardedCluster.getSpec().getCoordinator().getConfigurationsForCoordinator(),
        cluster,
        0);
    Assertions.assertEquals(
        new StackGresPostgresServiceBuilder()
        .withEnabled(true)
        .build(),
        cluster.getSpec().getPostgresServices().getPrimary());
    Assertions.assertEquals(
        new StackGresPostgresServiceBuilder()
        .withEnabled(true)
        .build(),
        cluster.getSpec().getPostgresServices().getReplicas());
  }

  @Test
  void givedMinimalShardedClusterPrimaryEnabled_shouldGenerateCoordinatorCluster() {
    var shardedCluster = getMinimalShardedCluster();
    shardedCluster.getSpec().setPostgresServices(
        new StackGresShardedClusterPostgresServicesBuilder()
        .withNewCoordinator()
        .withNewPrimary()
        .withEnabled(true)
        .endPrimary()
        .endCoordinator()
        .build());
    var cluster = getCoordinatorCluster(JsonUtil.copy(shardedCluster), Optional.empty());
    checkCoordinatorWithGlobalSettings(
        shardedCluster,
        shardedCluster.getSpec().getCoordinator(),
        shardedCluster.getSpec().getCoordinator().getConfigurationsForCoordinator(),
        cluster,
        0);
    Assertions.assertEquals(
        new StackGresPostgresServiceBuilder()
        .withEnabled(true)
        .build(),
        cluster.getSpec().getPostgresServices().getPrimary());
    Assertions.assertEquals(
        new StackGresPostgresServiceBuilder()
        .withEnabled(true)
        .build(),
        cluster.getSpec().getPostgresServices().getReplicas());
  }

  @Test
  void givedMinimalShardedClusterPrimaryDisabled_shouldGenerateCoordinatorCluster() {
    var shardedCluster = getMinimalShardedCluster();
    shardedCluster.getSpec().setPostgresServices(
        new StackGresShardedClusterPostgresServicesBuilder()
        .withNewCoordinator()
        .withNewPrimary()
        .withEnabled(false)
        .endPrimary()
        .endCoordinator()
        .build());
    var coordinatorPrimary =
        shardedCluster.getSpec().getPostgresServices().getCoordinator().getPrimary();
    coordinatorPrimary.setEnabled(false);
    var cluster = getCoordinatorCluster(JsonUtil.copy(shardedCluster), Optional.empty());
    checkCoordinatorWithGlobalSettings(
        shardedCluster,
        shardedCluster.getSpec().getCoordinator(),
        shardedCluster.getSpec().getCoordinator().getConfigurationsForCoordinator(),
        cluster,
        0);
    Assertions.assertEquals(
        new StackGresPostgresServiceBuilder()
        .withEnabled(true)
        .build(),
        cluster.getSpec().getPostgresServices().getPrimary());
    Assertions.assertEquals(
        new StackGresPostgresServiceBuilder()
        .withEnabled(true)
        .build(),
        cluster.getSpec().getPostgresServices().getReplicas());
  }

  @Test
  void givedMinimalShardedClusterPrimaryDisabledAndAnyDisabled_shouldGenerateCoordinatorCluster() {
    var shardedCluster = getMinimalShardedCluster();
    shardedCluster.getSpec().setPostgresServices(
        new StackGresShardedClusterPostgresServicesBuilder()
        .withNewCoordinator()
        .withNewPrimary()
        .withEnabled(false)
        .endPrimary()
        .withNewAny()
        .withEnabled(false)
        .endAny()
        .endCoordinator()
        .build());
    var coordinatorPrimary =
        shardedCluster.getSpec().getPostgresServices().getCoordinator().getPrimary();
    coordinatorPrimary.setEnabled(false);
    var cluster = getCoordinatorCluster(JsonUtil.copy(shardedCluster), Optional.empty());
    checkCoordinatorWithGlobalSettings(
        shardedCluster,
        shardedCluster.getSpec().getCoordinator(),
        shardedCluster.getSpec().getCoordinator().getConfigurationsForCoordinator(),
        cluster,
        0);
    Assertions.assertEquals(
        new StackGresPostgresServiceBuilder()
        .withEnabled(false)
        .build(),
        cluster.getSpec().getPostgresServices().getPrimary());
    Assertions.assertEquals(
        new StackGresPostgresServiceBuilder()
        .withEnabled(false)
        .build(),
        cluster.getSpec().getPostgresServices().getReplicas());
  }

  @Test
  void givedMinimalShardedCluster_shouldGenerateShardCluster() {
    var shardedCluster = getMinimalShardedCluster();
    var cluster = getWorkerCluster(JsonUtil.copy(shardedCluster), 0, Optional.empty());
    checkClusterWithGlobalSettings(
        shardedCluster,
        shardedCluster.getSpec().getWorkers(),
        shardedCluster.getSpec().getWorkers().getConfigurations(),
        cluster,
        1);
    Assertions.assertEquals(
        new StackGresPostgresServiceBuilder()
        .withEnabled(true)
        .build(),
        cluster.getSpec().getPostgresServices().getPrimary());
    Assertions.assertEquals(
        new StackGresPostgresServiceBuilder()
        .withEnabled(false)
        .build(),
        cluster.getSpec().getPostgresServices().getReplicas());
  }

  @Test
  void givedMinimalShardedClusterPrimariesEnabled_shouldGenerateShardCluster() {
    var shardedCluster = getMinimalShardedCluster();
    shardedCluster.getSpec().setPostgresServices(
        new StackGresShardedClusterPostgresServicesBuilder()
        .withNewWorkers()
        .withNewPrimaries()
        .withEnabled(true)
        .endPrimaries()
        .endWorkers()
        .build());
    var cluster = getWorkerCluster(JsonUtil.copy(shardedCluster), 0, Optional.empty());
    checkClusterWithGlobalSettings(
        shardedCluster,
        shardedCluster.getSpec().getWorkers(),
        shardedCluster.getSpec().getWorkers().getConfigurations(),
        cluster,
        1);
    Assertions.assertEquals(
        new StackGresPostgresServiceBuilder()
        .withEnabled(true)
        .build(),
        cluster.getSpec().getPostgresServices().getPrimary());
    Assertions.assertEquals(
        new StackGresPostgresServiceBuilder()
        .withEnabled(false)
        .build(),
        cluster.getSpec().getPostgresServices().getReplicas());
  }

  @Test
  void givedMinimalShardedClusterWithCoordinatorClusterName_shouldUseCustomCoordinatorName() {
    var shardedCluster = getMinimalShardedCluster();
    shardedCluster.getMetadata().setName("stackgres");
    shardedCluster.getSpec().getCoordinator().setClusterName("custom-coord");
    var cluster = getCoordinatorCluster(JsonUtil.copy(shardedCluster), Optional.empty());

    Assertions.assertEquals("custom-coord", cluster.getMetadata().getName());
  }

  @Test
  void givedMinimalShardedClusterWithWorkersClusterNameTemplate_shouldUseCustomWorkerName() {
    var shardedCluster = getMinimalShardedCluster();
    shardedCluster.getMetadata().setName("stackgres");
    shardedCluster.getSpec().getWorkers().setClusterNameTemplate("legacy-shard");
    var clusterIndex0 = getWorkerCluster(JsonUtil.copy(shardedCluster), 0, Optional.empty());
    var clusterIndex2 = getWorkerCluster(JsonUtil.copy(shardedCluster), 2, Optional.empty());

    Assertions.assertEquals("legacy-shard0", clusterIndex0.getMetadata().getName());
    Assertions.assertEquals("legacy-shard2", clusterIndex2.getMetadata().getName());
  }

  @Test
  void givedShardedClusterReplicatingFromUnknownSgShardedCluster_shouldThrow() {
    var shardedCluster = getMinimalShardedCluster();
    shardedCluster.getMetadata().setName("stackgres");
    shardedCluster.getSpec().setReplicateFrom(new StackGresShardedClusterReplicateFrom());
    shardedCluster.getSpec().getReplicateFrom().setInstance(
        new StackGresShardedClusterReplicateFromInstance());
    shardedCluster.getSpec().getReplicateFrom().getInstance().setSgShardedCluster("missing");

    var copy = JsonUtil.copy(shardedCluster);
    var ex = Assertions.assertThrows(RuntimeException.class,
        () -> getCoordinatorCluster(copy, Optional.empty()));
    Assertions.assertEquals("SGShardedCluster missing was not found", ex.getMessage());
  }

  @Test
  void givedShardedClusterReplicatingFromKnownSgShardedCluster_shouldResolveWorkerNameFromReferenced() {
    var shardedCluster = getMinimalShardedCluster();
    shardedCluster.getMetadata().setName("stackgres");
    shardedCluster.getSpec().setReplicateFrom(new StackGresShardedClusterReplicateFrom());
    shardedCluster.getSpec().getReplicateFrom().setInstance(
        new StackGresShardedClusterReplicateFromInstance());
    shardedCluster.getSpec().getReplicateFrom().getInstance().setSgShardedCluster("source");

    var replicateCluster = getMinimalShardedCluster();
    replicateCluster.getMetadata().setName("source");
    replicateCluster.getSpec().getWorkers().setClusterNameTemplate("source-shard");

    var cluster = getWorkerCluster(
        JsonUtil.copy(shardedCluster), 0, Optional.of(replicateCluster));

    Assertions.assertEquals("source-shard0",
        cluster.getSpec().getReplicateFrom().getInstance().getSgCluster());
  }

  @Test
  void givedMinimalShardedClusterPrimariesDisabled_shouldGenerateShardCluster() {
    var shardedCluster = getMinimalShardedCluster();
    shardedCluster.getSpec().setPostgresServices(
        new StackGresShardedClusterPostgresServicesBuilder()
        .withNewWorkers()
        .withNewPrimaries()
        .withEnabled(false)
        .endPrimaries()
        .endWorkers()
        .build());
    var cluster = getWorkerCluster(JsonUtil.copy(shardedCluster), 0, Optional.empty());
    checkClusterWithGlobalSettings(
        shardedCluster,
        shardedCluster.getSpec().getWorkers(),
        shardedCluster.getSpec().getWorkers().getConfigurations(),
        cluster,
        1);
    Assertions.assertEquals(
        new StackGresPostgresServiceBuilder()
        .withEnabled(false)
        .build(),
        cluster.getSpec().getPostgresServices().getPrimary());
    Assertions.assertEquals(
        new StackGresPostgresServiceBuilder()
        .withEnabled(false)
        .build(),
        cluster.getSpec().getPostgresServices().getReplicas());
  }

  @Test
  void givenMinimalShardedCluster_shouldGenerateQueryRouterClusterWithDefaultName() {
    var shardedCluster = getMinimalShardedCluster();
    shardedCluster.getSpec().getCoordinator().setQueryRouterClusters(2);
    var cluster = getQueryRouterCluster(JsonUtil.copy(shardedCluster), 1024, Optional.empty());

    Assertions.assertEquals(
        StackGresShardedClusterUtil.getQueryRouterClusterName(shardedCluster, 1024),
        cluster.getMetadata().getName());
    Assertions.assertEquals(1, cluster.getSpec().getInstances());
    Assertions.assertNull(cluster.getSpec().getReplication());
    Assertions.assertEquals(
        new StackGresPostgresServiceBuilder()
        .withEnabled(true)
        .build(),
        cluster.getSpec().getPostgresServices().getPrimary());
    Assertions.assertEquals(
        new StackGresPostgresServiceBuilder()
        .withEnabled(false)
        .build(),
        cluster.getSpec().getPostgresServices().getReplicas());
  }

  @Test
  void givenQueryRouterCluster_shouldStartPatroniOnlyOnceRegisteredByTheCoordinator() {
    var shardedCluster = getMinimalShardedCluster();
    shardedCluster.getSpec().getCoordinator().setQueryRouterClusters(2);
    var cluster = getQueryRouterCluster(JsonUtil.copy(shardedCluster), 1025, Optional.empty());
    var coordinator = getCoordinatorCluster(JsonUtil.copy(shardedCluster), Optional.empty());

    Assertions.assertEquals(
        Map.of(StackGresContext.CITUS_GROUP_REGISTERED_ANNOTATION, "1026"),
        cluster.getSpec().getConfigurations().getPatroni().getStartGateAnnotations());
    Assertions.assertNull(
        coordinator.getSpec().getConfigurations().getPatroni().getStartGateAnnotations());
  }

  @Test
  void givenQueryRouterClusterNameTemplate_shouldUseIt() {
    var shardedCluster = getMinimalShardedCluster();
    shardedCluster.getMetadata().setName("stackgres");
    shardedCluster.getSpec().getCoordinator().setQueryRouterClusters(3);
    shardedCluster.getSpec().getCoordinator()
        .setQueryRouterClusterNameTemplate("custom-router");
    var clusterIndex0 = getQueryRouterCluster(JsonUtil.copy(shardedCluster), 1024, Optional.empty());
    var clusterIndex2 = getQueryRouterCluster(JsonUtil.copy(shardedCluster), 1026, Optional.empty());

    Assertions.assertEquals("custom-router0", clusterIndex0.getMetadata().getName());
    Assertions.assertEquals("custom-router2", clusterIndex2.getMetadata().getName());
  }

  @Test
  void givenQueryRouterIndexOffset_shouldNameAccordingly() {
    var shardedCluster = getMinimalShardedCluster();
    shardedCluster.getMetadata().setName("stackgres");
    shardedCluster.getSpec().getCoordinator().setQueryRouterIndexOffset(2048);
    shardedCluster.getSpec().getCoordinator().setQueryRouterClusters(2);
    var clusterIndex0 = getQueryRouterCluster(JsonUtil.copy(shardedCluster), 2048, Optional.empty());
    var clusterIndex1 = getQueryRouterCluster(JsonUtil.copy(shardedCluster), 2049, Optional.empty());

    Assertions.assertEquals("stackgres-router0", clusterIndex0.getMetadata().getName());
    Assertions.assertEquals("stackgres-router1", clusterIndex1.getMetadata().getName());
  }

  @Test
  void givenMinimalShardedClusterQueryRoutersDisabled_shouldGenerateQueryRouterCluster() {
    var shardedCluster = getMinimalShardedCluster();
    shardedCluster.getSpec().getCoordinator().setQueryRouterClusters(1);
    shardedCluster.getSpec().setPostgresServices(
        new StackGresShardedClusterPostgresServicesBuilder()
        .withNewCoordinator()
        .withNewQueryRouters()
        .withEnabled(false)
        .endQueryRouters()
        .endCoordinator()
        .build());
    var cluster = getQueryRouterCluster(JsonUtil.copy(shardedCluster), 1024, Optional.empty());

    Assertions.assertEquals(
        new StackGresPostgresServiceBuilder()
        .withEnabled(false)
        .build(),
        cluster.getSpec().getPostgresServices().getPrimary());
    Assertions.assertEquals(
        new StackGresPostgresServiceBuilder()
        .withEnabled(false)
        .build(),
        cluster.getSpec().getPostgresServices().getReplicas());
  }

  @Test
  void givenConnectToPoolerNotSet_shouldAlwaysCreateTheConnectionPooler() {
    var shardedCluster = getMinimalShardedCluster();
    shardedCluster.getSpec().getCoordinator().setQueryRouterClusters(1);
    shardedCluster.getSpec().getCoordinator().setPods(new StackGresClusterPods());
    shardedCluster.getSpec().getCoordinator().getPods().setDisableConnectionPooling(true);
    shardedCluster.getSpec().getWorkers().setPods(new StackGresClusterPods());
    shardedCluster.getSpec().getWorkers().getPods().setDisableConnectionPooling(true);

    Assertions.assertFalse(getCoordinatorCluster(JsonUtil.copy(shardedCluster), Optional.empty())
        .getSpec().getPods().getDisableConnectionPooling());
    Assertions.assertFalse(getWorkerCluster(JsonUtil.copy(shardedCluster), 0, Optional.empty())
        .getSpec().getPods().getDisableConnectionPooling());
    Assertions.assertFalse(getQueryRouterCluster(JsonUtil.copy(shardedCluster), 1024, Optional.empty())
        .getSpec().getPods().getDisableConnectionPooling());
  }

  @Test
  void givenConnectToPoolerFalse_shouldKeepDisableConnectionPooling() {
    var shardedCluster = getMinimalShardedCluster();
    shardedCluster.getSpec().setConfigurations(new StackGresShardedClusterConfigurations());
    shardedCluster.getSpec().getConfigurations().setCitus(
        new StackGresShardedClusterCitusConfigurations());
    shardedCluster.getSpec().getConfigurations().getCitus().setConnectToPooler(false);
    shardedCluster.getSpec().getCoordinator().setQueryRouterClusters(1);
    shardedCluster.getSpec().getCoordinator().setPods(new StackGresClusterPods());
    shardedCluster.getSpec().getCoordinator().getPods().setDisableConnectionPooling(true);
    shardedCluster.getSpec().getWorkers().setPods(new StackGresClusterPods());
    shardedCluster.getSpec().getWorkers().getPods().setDisableConnectionPooling(true);

    Assertions.assertTrue(getCoordinatorCluster(JsonUtil.copy(shardedCluster), Optional.empty())
        .getSpec().getPods().getDisableConnectionPooling());
    Assertions.assertTrue(getWorkerCluster(JsonUtil.copy(shardedCluster), 0, Optional.empty())
        .getSpec().getPods().getDisableConnectionPooling());
    Assertions.assertTrue(getQueryRouterCluster(JsonUtil.copy(shardedCluster), 1024, Optional.empty())
        .getSpec().getPods().getDisableConnectionPooling());
  }

  private StackGresShardedCluster getMinimalShardedCluster() {
    return Fixtures.shardedCluster().getBuilder()
        .withNewSpec()
        .withNewPostgres()
        .withVersion("16.4")
        .endPostgres()
        .withNewCoordinator()
        .withNewConfigurationsForCoordinator()
        .endConfigurationsForCoordinator()
        .endCoordinator()
        .withNewWorkers()
        .withNewConfigurations()
        .endConfigurations()
        .endWorkers()
        .endSpec()
        .build();
  }

  @Test
  void givedShardedClusterWithMinimalCoordinator_shouldCopyGlobalSettings() {
    var shardedCluster = StackGresShardedClusterTestUtil.createShardedCluster();
    // PgBouncer is always created when connecting to the pooler (see withConnectToPooler tests)
    shardedCluster.getSpec().getConfigurations().getCitus().setConnectToPooler(false);
    shardedCluster.getMetadata().setName(
        "sg" + shardedCluster.getMetadata().getName().toLowerCase());
    shardedCluster.getSpec().getReplication().setRole(null);
    shardedCluster.getSpec().getReplication().setGroups(null);
    shardedCluster.getSpec().getConfigurations().getBackups().get(0)
        .setPaths(List.of(
            createWithRandomData(String.class),
            createWithRandomData(String.class)));
    shardedCluster.getSpec().getReplicateFrom().getInstance().getExternal()
        .setCustomRestoreMethods(List.of(
            createWithRandomData(StackGresClusterReplicateFromCustomRestoreMethod.class),
            createWithRandomData(StackGresClusterReplicateFromCustomRestoreMethod.class)));
    setMinimalCoordinatorAndWorkers(shardedCluster);
    var cluster = getCoordinatorCluster(JsonUtil.copy(shardedCluster), Optional.empty());
    checkCoordinatorWithGlobalSettings(
        shardedCluster,
        shardedCluster.getSpec().getCoordinator(),
        shardedCluster.getSpec().getCoordinator().getConfigurationsForCoordinator(),
        cluster,
        0);
    Assertions.assertEquals(
        shardedCluster.getSpec().getPostgresServices().getCoordinator().getPrimary().getEnabled()
        || shardedCluster.getSpec().getPostgresServices().getCoordinator().getAny().getEnabled(),
        cluster.getSpec().getPostgresServices().getPrimary().getEnabled());
    Assertions.assertEquals(
        shardedCluster.getSpec().getPostgresServices().getCoordinator().getCustomPorts()
        .stream()
        .map(customPort -> new CustomServicePortBuilder(customPort)
            .withNodePort(null)
            .build())
        .toList(),
        cluster.getSpec().getPostgresServices().getPrimary().getCustomPorts());
    Assertions.assertEquals(
        shardedCluster.getSpec().getPostgresServices().getCoordinator().getAny().getEnabled(),
        cluster.getSpec().getPostgresServices().getReplicas().getEnabled());
    Assertions.assertEquals(
        shardedCluster.getSpec().getPostgresServices().getCoordinator().getCustomPorts()
        .stream()
        .map(customPort -> new CustomServicePortBuilder(customPort)
            .withNodePort(null)
            .build())
        .toList(),
        cluster.getSpec().getPostgresServices().getReplicas().getCustomPorts());
  }

  @Test
  void givedShardedClusterWithMinimalWorkers_shouldCopyGlobalSettings() {
    var shardedCluster = StackGresShardedClusterTestUtil.createShardedCluster();
    // PgBouncer is always created when connecting to the pooler (see withConnectToPooler tests)
    shardedCluster.getSpec().getConfigurations().getCitus().setConnectToPooler(false);
    shardedCluster.getMetadata().setName(
        "sg" + shardedCluster.getMetadata().getName().toLowerCase());
    shardedCluster.getSpec().getReplication().setRole(null);
    shardedCluster.getSpec().getReplication().setGroups(null);
    shardedCluster.getSpec().getConfigurations().getBackups().get(0)
        .setPaths(List.of(
            createWithRandomData(String.class),
            createWithRandomData(String.class)));
    shardedCluster.getSpec().getReplicateFrom().getInstance().getExternal()
        .setCustomRestoreMethods(List.of(
            createWithRandomData(StackGresClusterReplicateFromCustomRestoreMethod.class),
            createWithRandomData(StackGresClusterReplicateFromCustomRestoreMethod.class)));
    setMinimalCoordinatorAndWorkers(shardedCluster);
    var cluster = getWorkerCluster(JsonUtil.copy(shardedCluster), 0, Optional.empty());
    checkClusterWithGlobalSettings(
        shardedCluster,
        shardedCluster.getSpec().getWorkers(),
        shardedCluster.getSpec().getWorkers().getConfigurations(),
        cluster,
        1);
    Assertions.assertEquals(
        shardedCluster.getSpec().getPostgresServices().getWorkers().getPrimaries().getEnabled(),
        cluster.getSpec().getPostgresServices().getPrimary().getEnabled());
    Assertions.assertEquals(
        shardedCluster.getSpec().getPostgresServices().getWorkers().getCustomPorts()
        .stream()
        .map(customPort -> new CustomServicePortBuilder(customPort)
            .withNodePort(null)
            .build())
        .toList(),
        cluster.getSpec().getPostgresServices().getPrimary().getCustomPorts());
    Assertions.assertEquals(
        new StackGresPostgresServiceBuilder()
        .withEnabled(false)
        .build(),
        cluster.getSpec().getPostgresServices().getReplicas());
  }

  @Test
  void givedShardedClusterWithCoordinator_shouldCopySettings() {
    var shardedCluster = StackGresShardedClusterTestUtil.createShardedCluster();
    // PgBouncer is always created when connecting to the pooler (see withConnectToPooler tests)
    shardedCluster.getSpec().getConfigurations().getCitus().setConnectToPooler(false);
    shardedCluster.getMetadata().setName(
        "sg" + shardedCluster.getMetadata().getName().toLowerCase());
    shardedCluster.getSpec().getReplication().setRole(null);
    shardedCluster.getSpec().getReplication().setGroups(null);
    shardedCluster.getSpec().getConfigurations().getBackups().get(0)
        .setPaths(List.of(
            createWithRandomData(String.class),
            createWithRandomData(String.class)));
    shardedCluster.getSpec().getReplicateFrom().getInstance().getExternal()
        .setCustomRestoreMethods(List.of(
            createWithRandomData(StackGresClusterReplicateFromCustomRestoreMethod.class),
            createWithRandomData(StackGresClusterReplicateFromCustomRestoreMethod.class)));
    shardedCluster.getSpec().getCoordinator().setReplication(null);
    shardedCluster.getSpec().getCoordinator().getReplicationForCoordinator().setRole(null);
    shardedCluster.getSpec().getCoordinator().getReplicationForCoordinator().setGroups(null);
    var cluster = getCoordinatorCluster(JsonUtil.copy(shardedCluster), Optional.empty());
    checkCoordinatorWithSettings(
        shardedCluster,
        shardedCluster.getSpec().getCoordinator(),
        shardedCluster.getSpec().getCoordinator().getReplicationForCoordinator(),
        shardedCluster.getSpec().getCoordinator().getConfigurationsForCoordinator(),
        shardedCluster.getSpec().getCoordinator().getPods(),
        cluster,
        0);
  }

  @Test
  void givedShardedClusterWithWorkers_shouldCopySettings() {
    var shardedCluster = StackGresShardedClusterTestUtil.createShardedCluster();
    // PgBouncer is always created when connecting to the pooler (see withConnectToPooler tests)
    shardedCluster.getSpec().getConfigurations().getCitus().setConnectToPooler(false);
    shardedCluster.getMetadata().setName(
        "sg" + shardedCluster.getMetadata().getName().toLowerCase());
    shardedCluster.getSpec().getReplication().setRole(null);
    shardedCluster.getSpec().getReplication().setGroups(null);
    shardedCluster.getSpec().getConfigurations().getBackups().get(0)
        .setPaths(List.of(
            createWithRandomData(String.class),
            createWithRandomData(String.class)));
    shardedCluster.getSpec().getReplicateFrom().getInstance().getExternal()
        .setCustomRestoreMethods(List.of(
            createWithRandomData(StackGresClusterReplicateFromCustomRestoreMethod.class),
            createWithRandomData(StackGresClusterReplicateFromCustomRestoreMethod.class)));
    shardedCluster.getSpec().getWorkers().setReplication(null);
    shardedCluster.getSpec().getWorkers().getReplicationForWorkers().setRole(null);
    shardedCluster.getSpec().getWorkers().getReplicationForWorkers().setGroups(null);
    shardedCluster.getSpec().getWorkers().setOverrides(null);
    var cluster = getWorkerCluster(JsonUtil.copy(shardedCluster), 0, Optional.empty());
    checkClusterWithSettings(
        shardedCluster,
        shardedCluster.getSpec().getWorkers(),
        shardedCluster.getSpec().getWorkers().getReplicationForWorkers(),
        shardedCluster.getSpec().getWorkers().getConfigurations(),
        shardedCluster.getSpec().getWorkers().getPods(),
        cluster,
        1);
  }

  @Test
  void givedShardedClusterWithWorkersOverrides_shouldCopyOverrideSettings() {
    var shardedCluster = StackGresShardedClusterTestUtil.createShardedCluster();
    // PgBouncer is always created when connecting to the pooler (see withConnectToPooler tests)
    shardedCluster.getSpec().getConfigurations().getCitus().setConnectToPooler(false);
    shardedCluster.getMetadata().setName(
        "sg" + shardedCluster.getMetadata().getName().toLowerCase());
    shardedCluster.getSpec().getReplication().setRole(null);
    shardedCluster.getSpec().getReplication().setGroups(null);
    shardedCluster.getSpec().getConfigurations().getBackups().get(0)
        .setPaths(List.of(
            createWithRandomData(String.class),
            createWithRandomData(String.class)));
    shardedCluster.getSpec().getReplicateFrom().getInstance().getExternal()
        .setCustomRestoreMethods(List.of(
            createWithRandomData(StackGresClusterReplicateFromCustomRestoreMethod.class),
            createWithRandomData(StackGresClusterReplicateFromCustomRestoreMethod.class)));
    shardedCluster.getSpec().getWorkers().getOverrides().get(0)
        .setIndex(0);
    shardedCluster.getSpec().getWorkers().getOverrides().get(0)
        .setType(StackGresWorkerType.WORKER.toString());
    shardedCluster.getSpec().getWorkers().getOverrides().get(0)
        .setReplication(null);
    shardedCluster.getSpec().getWorkers().getOverrides().get(0)
        .getReplicationForWorkers().setRole(null);
    shardedCluster.getSpec().getWorkers().getOverrides().get(0)
        .getReplicationForWorkers().setGroups(null);
    var cluster = getWorkerCluster(JsonUtil.copy(shardedCluster), 0, Optional.empty());
    checkClusterWithSettings(
        shardedCluster,
        shardedCluster.getSpec().getWorkers().getOverrides().get(0),
        shardedCluster.getSpec().getWorkers().getOverrides().get(0)
            .getReplicationForWorkers(),
        shardedCluster.getSpec().getWorkers().getOverrides().get(0)
            .getConfigurationsForWorkers(),
        shardedCluster.getSpec().getWorkers().getOverrides().get(0)
            .getPodsForWorkers(),
        cluster,
        1);
  }

  @Test
  void givedShardedClusterWithQueryRoutersOverrides_shouldCopyOverrideSettings() {
    var shardedCluster = StackGresShardedClusterTestUtil.createShardedCluster();
    // PgBouncer is always created when connecting to the pooler (see withConnectToPooler tests)
    shardedCluster.getSpec().getConfigurations().getCitus().setConnectToPooler(false);
    shardedCluster.getMetadata().setName(
        "sg" + shardedCluster.getMetadata().getName().toLowerCase());
    shardedCluster.getSpec().getReplication().setRole(null);
    shardedCluster.getSpec().getReplication().setGroups(null);
    shardedCluster.getSpec().getConfigurations().getBackups().get(0)
        .setPaths(List.of(
            createWithRandomData(String.class),
            createWithRandomData(String.class)));
    shardedCluster.getSpec().getConfigurations().getBackups().get(0)
        .setQueryRouterPaths(List.of(
            createWithRandomData(String.class)));
    shardedCluster.getSpec().getReplicateFrom().getInstance().getExternal()
        .setCustomRestoreMethods(List.of(
            createWithRandomData(StackGresClusterReplicateFromCustomRestoreMethod.class),
            createWithRandomData(StackGresClusterReplicateFromCustomRestoreMethod.class),
            createWithRandomData(StackGresClusterReplicateFromCustomRestoreMethod.class)));
    shardedCluster.getSpec().getWorkers().setClusters(0);
    shardedCluster.getSpec().getCoordinator().setQueryRouterIndexOffset(0);
    shardedCluster.getSpec().getWorkers().getOverrides().get(0)
        .setIndex(0);
    shardedCluster.getSpec().getWorkers().getOverrides().get(0)
        .setType(StackGresWorkerType.QUERY_ROUTER.toString());
    shardedCluster.getSpec().getWorkers().getOverrides().get(0)
        .setReplication(null);
    shardedCluster.getSpec().getWorkers().getOverrides().get(0)
        .getReplicationForWorkers().setRole(null);
    shardedCluster.getSpec().getWorkers().getOverrides().get(0)
        .getReplicationForWorkers().setGroups(null);
    var cluster = getQueryRouterCluster(JsonUtil.copy(shardedCluster), 0, Optional.empty());
    checkClusterWithSettings(
        shardedCluster,
        shardedCluster.getSpec().getWorkers().getOverrides().get(0),
        shardedCluster.getSpec().getWorkers().getOverrides().get(0)
            .getReplicationForWorkers(),
        shardedCluster.getSpec().getWorkers().getOverrides().get(0)
            .getConfigurationsForWorkers(),
        shardedCluster.getSpec().getWorkers().getOverrides().get(0)
            .getPodsForWorkers(),
        cluster,
        1,
        true);
  }

  private void setMinimalCoordinatorAndWorkers(StackGresShardedCluster shardedCluster) {
    shardedCluster.getSpec().setCoordinator(new StackGresShardedClusterCoordinator());
    shardedCluster.getSpec().getCoordinator().setInstances(1);
    shardedCluster.getSpec().getCoordinator()
        .setConfigurationsForCoordinator(new StackGresShardedClusterCoordinatorConfigurations());
    shardedCluster.getSpec().getCoordinator().setPods(new StackGresClusterPods());
    shardedCluster.getSpec().setWorkers(new StackGresShardedClusterWorkers());
    shardedCluster.getSpec().getWorkers().setClusters(1);
    shardedCluster.getSpec().getWorkers().setInstancesPerCluster(1);
    shardedCluster.getSpec().getWorkers().setConfigurations(new StackGresClusterConfigurations());
    shardedCluster.getSpec().getWorkers().setPods(new StackGresClusterPods());
  }

  private void checkCoordinatorWithGlobalSettings(
      StackGresShardedCluster shardedCluster,
      StackGresClusterSpec clusterSpec,
      StackGresClusterConfigurations configuration,
      StackGresCluster cluster,
      int index) {
    configuration
        .setSgPostgresConfig(coordinatorConfigName(shardedCluster));
    checkClusterWithGlobalSettings(
        shardedCluster,
        clusterSpec,
        configuration,
        cluster,
        index);
  }

  private void checkClusterWithGlobalSettings(
      StackGresShardedCluster shardedCluster,
      StackGresClusterSpec clusterSpec,
      StackGresClusterConfigurations configuration,
      StackGresCluster cluster,
      int index) {
    checkClusterWithGlobalSettings(shardedCluster, clusterSpec, configuration, cluster, index, false);
  }

  private void checkClusterWithGlobalSettings(
      StackGresShardedCluster shardedCluster,
      StackGresClusterSpec clusterSpec,
      StackGresClusterConfigurations configuration,
      StackGresCluster cluster,
      int index,
      boolean queryRouter) {
    checkClusterGlobalSettingsOnly(shardedCluster, cluster, index, queryRouter);
    if (shardedCluster.getSpec().getMetadata() != null
        && shardedCluster.getSpec().getMetadata().getLabels() != null) {
      Assertions.assertEquals(
          Seq.seq(shardedCluster.getSpec().getMetadata().getLabels().getClusterPods())
          .append(Tuple.tuple("citus-group", String.valueOf(index)))
          .toMap(Tuple2::v1, Tuple2::v2),
          cluster.getSpec().getMetadata().getLabels().getClusterPods());
      Assertions.assertEquals(
          Seq.seq(shardedCluster.getSpec().getMetadata().getLabels().getServices())
          .append(Tuple.tuple("citus-group", String.valueOf(index)))
          .toMap(Tuple2::v1, Tuple2::v2),
          cluster.getSpec().getMetadata().getLabels().getServices());
    }
    if (shardedCluster.getSpec().getMetadata() != null
        && shardedCluster.getSpec().getMetadata().getAnnotations() != null) {
      Assertions.assertEquals(
          shardedCluster.getSpec().getMetadata().getAnnotations(),
          cluster.getSpec().getMetadata().getAnnotations());
    }
    Assertions.assertEquals(
        shardedCluster.getSpec().getReplication(),
        cluster.getSpec().getReplication());
    setExpectedShardPostgresConfig(shardedCluster, configuration, index, queryRouter);
    checkClusterSettings(
        clusterSpec,
        configuration,
        clusterSpec.getPods(),
        cluster);
  }

  private void checkCoordinatorWithSettings(
      StackGresShardedCluster shardedCluster,
      StackGresClusterSpec clusterSpec,
      StackGresClusterReplication replication,
      StackGresClusterConfigurations configuration,
      StackGresClusterPods pod,
      StackGresCluster cluster,
      int index) {
    configuration
        .setSgPostgresConfig(coordinatorConfigName(shardedCluster));
    checkClusterWithSettings(
        shardedCluster,
        clusterSpec,
        replication,
        configuration,
        pod,
        cluster,
        index);
  }

  private void checkClusterWithSettings(
      StackGresShardedCluster shardedCluster,
      StackGresClusterSpec clusterSpec,
      StackGresClusterReplication replication,
      StackGresClusterConfigurations configuration,
      StackGresClusterPods pod,
      StackGresCluster cluster,
      int index) {
    checkClusterWithSettings(shardedCluster, clusterSpec, replication, configuration, pod, cluster, index, false);
  }

  private void checkClusterWithSettings(
      StackGresShardedCluster shardedCluster,
      StackGresClusterSpec clusterSpec,
      StackGresClusterReplication replication,
      StackGresClusterConfigurations configuration,
      StackGresClusterPods pod,
      StackGresCluster cluster,
      int index,
      boolean queryRouter) {
    checkClusterGlobalSettingsOnly(shardedCluster, cluster, index, queryRouter);
    Assertions.assertEquals(
        Seq.seq(clusterSpec.getMetadata().getLabels().getClusterPods())
        .append(Tuple.tuple("citus-group", String.valueOf(index)))
        .toMap(Tuple2::v1, Tuple2::v2),
        cluster.getSpec().getMetadata().getLabels().getClusterPods());
    Assertions.assertEquals(
        Seq.seq(clusterSpec.getMetadata().getLabels().getServices())
        .append(Tuple.tuple("citus-group", String.valueOf(index)))
        .toMap(Tuple2::v1, Tuple2::v2),
        cluster.getSpec().getMetadata().getLabels().getServices());
    Assertions.assertEquals(
        clusterSpec.getMetadata().getAnnotations(),
        cluster.getSpec().getMetadata().getAnnotations());
    Assertions.assertEquals(
        replication,
        cluster.getSpec().getReplication());
    setExpectedShardPostgresConfig(shardedCluster, configuration, index, queryRouter);
    checkClusterSettings(clusterSpec, configuration, pod, cluster);
  }

  private void setExpectedShardPostgresConfig(
      StackGresShardedCluster shardedCluster,
      StackGresClusterConfigurations configuration,
      int index,
      boolean queryRouter) {
    if (index > 0) {
      configuration.setSgPostgresConfig(queryRouter
          ? StackGresShardedClusterUtil.queryRouterConfigName(shardedCluster, index - 1)
          : StackGresShardedClusterUtil.workerConfigName(shardedCluster, index - 1));
    }
  }

  private void checkClusterGlobalSettingsOnly(
      StackGresShardedCluster shardedCluster,
      StackGresCluster cluster,
      int index,
      boolean queryRouter) {
    if (shardedCluster.getSpec().getConfigurations() != null
        && shardedCluster.getSpec().getConfigurations().getBackups() != null) {
      Assertions.assertEquals(
          shardedCluster.getSpec().getConfigurations().getBackups().get(0)
          .getRetention(),
          cluster.getSpec().getConfigurations().getBackups().get(0)
          .getRetention());
      Assertions.assertEquals(
          shardedCluster.getSpec().getConfigurations().getBackups().get(0)
          .getCompression(),
          cluster.getSpec().getConfigurations().getBackups().get(0)
          .getCompression());
      Assertions.assertNull(
          cluster.getSpec().getConfigurations().getBackups().get(0)
          .getCronSchedule());
      Assertions.assertEquals(
          shardedCluster.getSpec().getConfigurations().getBackups().get(0)
          .getSgObjectStorage(),
          cluster.getSpec().getConfigurations().getBackups().get(0)
          .getSgObjectStorage());
      Assertions.assertEquals(
          shardedCluster.getSpec().getConfigurations().getBackups().get(0)
          .getPerformance(),
          cluster.getSpec().getConfigurations().getBackups().get(0)
          .getPerformance());
      if (queryRouter) {
        Assertions.assertEquals(
            shardedCluster.getSpec().getConfigurations().getBackups().get(0)
            .getQueryRouterPaths().get(index - 1),
            cluster.getSpec().getConfigurations().getBackups().get(0)
            .getPath());
      } else {
        Assertions.assertEquals(
            shardedCluster.getSpec().getConfigurations().getBackups().get(0)
            .getPaths().get(index),
            cluster.getSpec().getConfigurations().getBackups().get(0)
            .getPath());
      }
    }
    Assertions.assertEquals(
        shardedCluster.getSpec().getDistributedLogs(),
        cluster.getSpec().getDistributedLogs());
    Assertions.assertEquals(
        shardedCluster.getSpec().getNonProductionOptions(),
        cluster.getSpec().getNonProductionOptions());
    if (shardedCluster.getStatus() != null
        && shardedCluster.getStatus().getExtensions() != null) {
      Assertions.assertEquals(
          new StackGresClusterPostgresBuilder(shardedCluster.getSpec().getPostgres())
          .editSsl()
          .withCertificateSecretKeySelector(
              shardedCluster.getSpec().getPostgres().getSsl().getEnabled()
              ? new SecretKeySelector(
                  StackGresShardedClusterUtil.CERTIFICATE_KEY,
                  StackGresShardedClusterUtil.postgresSslSecretName(shardedCluster))
                  : shardedCluster.getSpec().getPostgres().getSsl()
                  .getCertificateSecretKeySelector())
          .withPrivateKeySecretKeySelector(
              shardedCluster.getSpec().getPostgres().getSsl().getEnabled()
              ? new SecretKeySelector(
                  StackGresShardedClusterUtil.PRIVATE_KEY_KEY,
                  StackGresShardedClusterUtil.postgresSslSecretName(shardedCluster))
                  : shardedCluster.getSpec().getPostgres().getSsl()
                  .getPrivateKeySecretKeySelector())
          .endSsl()
          .withExtensions(shardedCluster.getStatus().getExtensions()
              .stream()
              .map(extension -> new StackGresClusterExtensionBuilder()
                  .withName(extension.getName())
                  .withPublisher(extension.getPublisher())
                  .withRepository(extension.getRepository())
                  .withVersion(extension.getVersion())
                  .build())
              .toList())
          .build(),
          cluster.getSpec().getPostgres());
    }
  }

  private void checkClusterSettings(
      StackGresClusterSpec clusterSpec,
      StackGresClusterConfigurations configuration,
      StackGresClusterPods pod,
      StackGresCluster cluster) {
    Assertions.assertEquals(
        clusterSpec.getSgInstanceProfile(),
        cluster.getSpec().getSgInstanceProfile());
    Assertions.assertEquals(
        configuration.getSgPostgresConfig(),
        cluster.getSpec().getConfigurations().getSgPostgresConfig());
    Assertions.assertEquals(
        configuration.getSgPoolingConfig(),
        cluster.getSpec().getConfigurations().getSgPoolingConfig());
    if (pod != null) {
      Assertions.assertEquals(
          pod.getDisableConnectionPooling(),
          cluster.getSpec().getPods().getDisableConnectionPooling());
      Assertions.assertEquals(
          pod.getDisableMetricsExporter(),
          cluster.getSpec().getPods().getDisableMetricsExporter());
      Assertions.assertEquals(
          pod.getDisablePostgresUtil(),
          cluster.getSpec().getPods().getDisablePostgresUtil());
      Assertions.assertEquals(
          pod.getManagementPolicy(),
          cluster.getSpec().getPods().getManagementPolicy());
      Assertions.assertEquals(
          pod.getCustomVolumes(),
          cluster.getSpec().getPods().getCustomVolumes());
      Assertions.assertEquals(
          pod.getCustomContainers(),
          cluster.getSpec().getPods().getCustomContainers());
      Assertions.assertEquals(
          pod.getCustomInitContainers(),
          cluster.getSpec().getPods().getCustomInitContainers());
      Assertions.assertEquals(
          pod.getCustomEnv(),
          cluster.getSpec().getPods().getCustomEnv());
      Assertions.assertEquals(
          pod.getCustomInitEnv(),
          cluster.getSpec().getPods().getCustomInitEnv());
      Assertions.assertEquals(
          pod.getCustomEnvFrom(),
          cluster.getSpec().getPods().getCustomEnvFrom());
      Assertions.assertEquals(
          pod.getCustomInitEnvFrom(),
          cluster.getSpec().getPods().getCustomInitEnvFrom());
      Assertions.assertEquals(
          pod.getResources(),
          cluster.getSpec().getPods().getResources());
      Assertions.assertEquals(
          pod.getPersistentVolume(),
          cluster.getSpec().getPods().getPersistentVolume());
    }
  }

}
