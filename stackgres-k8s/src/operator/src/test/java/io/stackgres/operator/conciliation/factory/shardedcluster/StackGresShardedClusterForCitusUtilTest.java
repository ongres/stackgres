/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardedcluster;

import static io.stackgres.common.StackGresShardedClusterUtil.coordinatorConfigName;
import static io.stackgres.operator.conciliation.factory.shardedcluster.StackGresShardedClusterForCitusUtil.getCoordinatorCluster;
import static io.stackgres.operator.conciliation.factory.shardedcluster.StackGresShardedClusterForCitusUtil.getShardsCluster;
import static io.stackgres.testutil.ModelTestUtil.createWithRandomData;

import java.util.ArrayList;
import java.util.List;

import io.stackgres.common.StackGresShardedClusterUtil;
import io.stackgres.common.crd.CustomServicePortBuilder;
import io.stackgres.common.crd.SecretKeySelector;
import io.stackgres.common.crd.postgres.service.StackGresPostgresServiceBuilder;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtensionBuilder;
import io.stackgres.common.crd.sgcluster.StackGresClusterPods;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgresBuilder;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplication;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterCoordinator;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterCoordinatorConfigurations;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterPostgresServicesBuilder;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterShards;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.testutil.JsonUtil;
import io.stackgres.testutil.ModelTestUtil;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class StackGresShardedClusterForCitusUtilTest {

  @Test
  void givedMinimalShardedCluster_shouldGenerateCoordinatorCluster() {
    var shardedCluster = getMinimalShardedCluster();
    var cluster = getCoordinatorCluster(JsonUtil.copy(shardedCluster));
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
    var cluster = getCoordinatorCluster(JsonUtil.copy(shardedCluster));
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
    var cluster = getCoordinatorCluster(JsonUtil.copy(shardedCluster));
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
    var cluster = getCoordinatorCluster(JsonUtil.copy(shardedCluster));
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
    var cluster = getShardsCluster(JsonUtil.copy(shardedCluster), 0);
    checkClusterWithGlobalSettings(
        shardedCluster,
        shardedCluster.getSpec().getShards(),
        shardedCluster.getSpec().getShards().getConfigurations(),
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
        .withNewShards()
        .withNewPrimaries()
        .withEnabled(true)
        .endPrimaries()
        .endShards()
        .build());
    var cluster = getShardsCluster(JsonUtil.copy(shardedCluster), 0);
    checkClusterWithGlobalSettings(
        shardedCluster,
        shardedCluster.getSpec().getShards(),
        shardedCluster.getSpec().getShards().getConfigurations(),
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
  void givedMinimalShardedClusterPrimariesDisabled_shouldGenerateShardCluster() {
    var shardedCluster = getMinimalShardedCluster();
    shardedCluster.getSpec().setPostgresServices(
        new StackGresShardedClusterPostgresServicesBuilder()
        .withNewShards()
        .withNewPrimaries()
        .withEnabled(false)
        .endPrimaries()
        .endShards()
        .build());
    var cluster = getShardsCluster(JsonUtil.copy(shardedCluster), 0);
    checkClusterWithGlobalSettings(
        shardedCluster,
        shardedCluster.getSpec().getShards(),
        shardedCluster.getSpec().getShards().getConfigurations(),
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

  private StackGresShardedCluster getMinimalShardedCluster() {
    return Fixtures.shardedCluster().getBuilder()
        .withNewSpec()
        .withNewPostgres()
        .endPostgres()
        .withNewCoordinator()
        .withNewConfigurationsForCoordinator()
        .endConfigurationsForCoordinator()
        .endCoordinator()
        .withNewShards()
        .withNewConfigurations()
        .endConfigurations()
        .endShards()
        .endSpec()
        .build();
  }

  @Test
  void givedShardedClusterWithMinimalCoordinator_shouldCopyGlobalSettings() {
    var shardedCluster = createShardedCluster();
    shardedCluster.getMetadata().setName(
        "sg" + shardedCluster.getMetadata().getName().toLowerCase());
    shardedCluster.getSpec().getReplication().setRole(null);
    shardedCluster.getSpec().getReplication().setGroups(null);
    shardedCluster.getSpec().getConfigurations().getBackups().get(0)
        .setPaths(List.of(
            createWithRandomData(String.class),
            createWithRandomData(String.class)));
    setMinimalCoordinatorAndShards(shardedCluster);
    var cluster = getCoordinatorCluster(JsonUtil.copy(shardedCluster));
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
  void givedShardedClusterWithMinimalShards_shouldCopyGlobalSettings() {
    var shardedCluster = createShardedCluster();
    shardedCluster.getMetadata().setName(
        "sg" + shardedCluster.getMetadata().getName().toLowerCase());
    shardedCluster.getSpec().getReplication().setRole(null);
    shardedCluster.getSpec().getReplication().setGroups(null);
    shardedCluster.getSpec().getConfigurations().getBackups().get(0)
        .setPaths(List.of(
            createWithRandomData(String.class),
            createWithRandomData(String.class)));
    setMinimalCoordinatorAndShards(shardedCluster);
    var cluster = getShardsCluster(JsonUtil.copy(shardedCluster), 0);
    checkClusterWithGlobalSettings(
        shardedCluster,
        shardedCluster.getSpec().getShards(),
        shardedCluster.getSpec().getShards().getConfigurations(),
        cluster,
        1);
    Assertions.assertEquals(
        shardedCluster.getSpec().getPostgresServices().getShards().getPrimaries().getEnabled(),
        cluster.getSpec().getPostgresServices().getPrimary().getEnabled());
    Assertions.assertEquals(
        shardedCluster.getSpec().getPostgresServices().getShards().getCustomPorts()
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
    var shardedCluster = createShardedCluster();
    shardedCluster.getMetadata().setName(
        "sg" + shardedCluster.getMetadata().getName().toLowerCase());
    shardedCluster.getSpec().getReplication().setRole(null);
    shardedCluster.getSpec().getReplication().setGroups(null);
    shardedCluster.getSpec().getConfigurations().getBackups().get(0)
        .setPaths(List.of(
            createWithRandomData(String.class),
            createWithRandomData(String.class)));
    shardedCluster.getSpec().getCoordinator().setReplication(null);
    shardedCluster.getSpec().getCoordinator().getReplicationForCoordinator().setRole(null);
    shardedCluster.getSpec().getCoordinator().getReplicationForCoordinator().setGroups(null);
    var cluster = getCoordinatorCluster(JsonUtil.copy(shardedCluster));
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
  void givedShardedClusterWithShards_shouldCopySettings() {
    var shardedCluster = createShardedCluster();
    shardedCluster.getMetadata().setName(
        "sg" + shardedCluster.getMetadata().getName().toLowerCase());
    shardedCluster.getSpec().getReplication().setRole(null);
    shardedCluster.getSpec().getReplication().setGroups(null);
    shardedCluster.getSpec().getConfigurations().getBackups().get(0)
        .setPaths(List.of(
            createWithRandomData(String.class),
            createWithRandomData(String.class)));
    shardedCluster.getSpec().getShards().setReplication(null);
    shardedCluster.getSpec().getShards().getReplicationForShards().setRole(null);
    shardedCluster.getSpec().getShards().getReplicationForShards().setGroups(null);
    shardedCluster.getSpec().getShards().setOverrides(null);
    var cluster = getShardsCluster(JsonUtil.copy(shardedCluster), 0);
    checkClusterWithSettings(
        shardedCluster,
        shardedCluster.getSpec().getShards(),
        shardedCluster.getSpec().getShards().getReplicationForShards(),
        shardedCluster.getSpec().getShards().getConfigurations(),
        shardedCluster.getSpec().getShards().getPods(),
        cluster,
        1);
  }

  @Test
  void givedShardedClusterWithShardsOverrides_shouldCopyOverrideSettings() {
    var shardedCluster = createShardedCluster();
    shardedCluster.getMetadata().setName(
        "sg" + shardedCluster.getMetadata().getName().toLowerCase());
    shardedCluster.getSpec().getReplication().setRole(null);
    shardedCluster.getSpec().getReplication().setGroups(null);
    shardedCluster.getSpec().getConfigurations().getBackups().get(0)
        .setPaths(List.of(
            createWithRandomData(String.class),
            createWithRandomData(String.class)));
    shardedCluster.getSpec().getShards().getOverrides().get(0)
        .setIndex(0);
    shardedCluster.getSpec().getShards().getOverrides().get(0)
        .setReplication(null);
    shardedCluster.getSpec().getShards().getOverrides().get(0)
        .getReplicationForShards().setRole(null);
    shardedCluster.getSpec().getShards().getOverrides().get(0)
        .getReplicationForShards().setGroups(null);
    var cluster = getShardsCluster(JsonUtil.copy(shardedCluster), 0);
    checkClusterWithSettings(
        shardedCluster,
        shardedCluster.getSpec().getShards().getOverrides().get(0),
        shardedCluster.getSpec().getShards().getOverrides().get(0)
            .getReplicationForShards(),
        shardedCluster.getSpec().getShards().getOverrides().get(0)
            .getConfigurationsForShards(),
        shardedCluster.getSpec().getShards().getOverrides().get(0)
            .getPodsForShards(),
        cluster,
        1);
  }

  private void setMinimalCoordinatorAndShards(StackGresShardedCluster shardedCluster) {
    shardedCluster.getSpec().setCoordinator(new StackGresShardedClusterCoordinator());
    shardedCluster.getSpec().getCoordinator().setInstances(1);
    shardedCluster.getSpec().getCoordinator()
        .setConfigurationsForCoordinator(new StackGresShardedClusterCoordinatorConfigurations());
    shardedCluster.getSpec().getCoordinator().setPods(new StackGresClusterPods());
    shardedCluster.getSpec().setShards(new StackGresShardedClusterShards());
    shardedCluster.getSpec().getShards().setClusters(1);
    shardedCluster.getSpec().getShards().setInstancesPerCluster(1);
    shardedCluster.getSpec().getShards().setConfigurations(new StackGresClusterConfigurations());
    shardedCluster.getSpec().getShards().setPods(new StackGresClusterPods());
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
    checkClusterGlobalSettingsOnly(shardedCluster, cluster, index);
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
    checkClusterGlobalSettingsOnly(shardedCluster, cluster, index);
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
    checkClusterSettings(clusterSpec, configuration, pod, cluster);
  }

  private void checkClusterGlobalSettingsOnly(
      StackGresShardedCluster shardedCluster,
      StackGresCluster cluster,
      int index) {
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
      Assertions.assertEquals(
          shardedCluster.getSpec().getConfigurations().getBackups().get(0)
          .getPaths().get(index),
          cluster.getSpec().getConfigurations().getBackups().get(0)
          .getPath());
    }
    Assertions.assertEquals(
        shardedCluster.getSpec().getDistributedLogs(),
        cluster.getSpec().getDistributedLogs());
    Assertions.assertEquals(
        shardedCluster.getSpec().getNonProductionOptions(),
        cluster.getSpec().getNonProductionOptions());
    if (shardedCluster.getStatus() != null
        && shardedCluster.getStatus().getToInstallPostgresExtensions() != null) {
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
          .withExtensions(shardedCluster.getStatus().getToInstallPostgresExtensions()
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
    if (cluster.getSpec().getPods() != null) {
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
          pod.getResources(),
          cluster.getSpec().getPods().getResources());
      Assertions.assertEquals(
          pod.getPersistentVolume(),
          cluster.getSpec().getPods().getPersistentVolume());
    }
  }

  private StackGresShardedCluster createShardedCluster() {
    var shardedCluster = createWithRandomData(StackGresShardedCluster.class);
    List<String> sgBackups = new ArrayList<String>(
        shardedCluster.getSpec().getShards().getClusters() + 1);
    Seq.range(0, shardedCluster.getSpec().getShards().getClusters() + 1)
        .forEach(index -> sgBackups.add(ModelTestUtil.generateRandom(String.class)));
    shardedCluster.getStatus().setSgBackups(sgBackups);
    return shardedCluster;
  }

}
