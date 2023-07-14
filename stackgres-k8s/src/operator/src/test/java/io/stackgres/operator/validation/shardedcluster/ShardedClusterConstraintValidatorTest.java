/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardedcluster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import com.google.common.collect.Lists;
import io.stackgres.common.crd.SecretKeySelector;
import io.stackgres.common.crd.Toleration;
import io.stackgres.common.crd.sgcluster.StackGresClusterNonProduction;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodScheduling;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplication;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterSsl;
import io.stackgres.common.crd.sgcluster.StackGresFeatureGates;
import io.stackgres.common.crd.sgcluster.StackGresPodPersistentVolume;
import io.stackgres.common.crd.sgcluster.StackGresPostgresFlavor;
import io.stackgres.common.crd.sgcluster.StackGresReplicationMode;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterBackupConfiguration;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterConfiguration;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterCoordinator;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterReplication;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterShard;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterShardPod;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterShards;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpec;
import io.stackgres.common.validation.ValidEnum;
import io.stackgres.common.validation.ValidEnumList;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.validation.ConstraintValidationTest;
import io.stackgres.operator.validation.ConstraintValidator;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.junit.jupiter.api.Test;

class ShardedClusterConstraintValidatorTest
    extends ConstraintValidationTest<StackGresShardedClusterReview> {

  @Override
  protected ConstraintValidator<StackGresShardedClusterReview> buildValidator() {
    return new ShardedClusterConstraintValidator();
  }

  @Override
  protected StackGresShardedClusterReview getValidReview() {
    return AdmissionReviewFixtures.shardedCluster().loadCreate().get();
  }

  @Override
  protected StackGresShardedClusterReview getInvalidReview() {
    final StackGresShardedClusterReview review =
        AdmissionReviewFixtures.shardedCluster().loadCreate().get();

    review.getRequest().getObject().setSpec(null);
    return review;
  }

  @Test
  void nullSpec_shouldFail() {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().setSpec(null);

    checkNotNullErrorCause(StackGresShardedCluster.class, "spec", review);
  }

  @Test
  void sslCertificateSecretWithEmptyName_shouldFail() {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getPostgres().setSsl(new StackGresClusterSsl());
    review.getRequest().getObject().getSpec().getPostgres().getSsl().setEnabled(true);
    review.getRequest().getObject().getSpec().getPostgres().getSsl()
        .setCertificateSecretKeySelector(
            new SecretKeySelector("test", null));
    review.getRequest().getObject().getSpec().getPostgres().getSsl().setPrivateKeySecretKeySelector(
        new SecretKeySelector("test", "test"));

    checkErrorCause(SecretKeySelector.class,
        "spec.postgres.ssl.certificateSecretKeySelector.name",
        "isNameNotEmpty", review, AssertTrue.class);
  }

  @Test
  void sslPrivateKeySecretWithEmptyName_shouldFail() {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getPostgres().setSsl(new StackGresClusterSsl());
    review.getRequest().getObject().getSpec().getPostgres().getSsl().setEnabled(true);
    review.getRequest().getObject().getSpec().getPostgres().getSsl()
        .setCertificateSecretKeySelector(
            new SecretKeySelector("test", "test"));
    review.getRequest().getObject().getSpec().getPostgres().getSsl().setPrivateKeySecretKeySelector(
        new SecretKeySelector("test", null));

    checkErrorCause(SecretKeySelector.class,
        "spec.postgres.ssl.privateKeySecretKeySelector.name",
        "isNameNotEmpty", review, AssertTrue.class);
  }

  @Test
  void givenNullSyncInstances_shouldFail() {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getCoordinator().setInstances(2);
    review.getRequest().getObject().getSpec().getReplication().setMode(
        StackGresReplicationMode.SYNC.toString());
    review.getRequest().getObject().getSpec().getReplication().setSyncInstances(null);

    checkErrorCause(StackGresShardedClusterReplication.class,
        "spec.replication.syncInstances",
        "isSyncInstancesSetForSyncMode",
        review, AssertTrue.class);
  }

  @Test
  void givenSyncInstancesLessThanOne_shouldFail() {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getCoordinator().setInstances(2);
    review.getRequest().getObject().getSpec().getReplication().setMode(
        StackGresReplicationMode.SYNC.toString());
    review.getRequest().getObject().getSpec().getReplication().setSyncInstances(0);

    checkErrorCause(StackGresClusterReplication.class,
        "spec.replication.syncInstances",
        review, Min.class, "must be greater than or equal to 1");
  }

  @Test
  void givenNullBackupPathsOnBackups_shouldFail() {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec()
        .setConfiguration(new StackGresShardedClusterConfiguration());
    review.getRequest().getObject().getSpec().getConfiguration().setBackups(new ArrayList<>());
    review.getRequest().getObject().getSpec().getConfiguration().getBackups()
        .add(new StackGresShardedClusterBackupConfiguration());
    review.getRequest().getObject().getSpec().getConfiguration().getBackups().get(0)
        .setObjectStorage("test");

    checkErrorCause(StackGresShardedClusterBackupConfiguration.class,
        "spec.configurations.backups[0].paths",
        review, NotNull.class, "must not be null");
  }

  @Test
  void givenNullObjectStorageOnBackups_shouldFail() {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec()
        .setConfiguration(new StackGresShardedClusterConfiguration());
    review.getRequest().getObject().getSpec().getConfiguration().setBackups(new ArrayList<>());
    review.getRequest().getObject().getSpec().getConfiguration().getBackups()
        .add(new StackGresShardedClusterBackupConfiguration());
    review.getRequest().getObject().getSpec().getConfiguration().getBackups().get(0)
        .setPaths(List.of("test-0", "test-1", "test-2"));

    checkErrorCause(StackGresShardedClusterBackupConfiguration.class,
        "spec.configurations.backups[0].sgObjectStorage",
        review, NotNull.class, "must not be null");
  }

  @Test
  void givenValidFlavor_shouldPass() throws ValidationFailed {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getPostgres().setFlavor(
        StackGresPostgresFlavor.BABELFISH.toString());

    validator.validate(review);
  }

  @Test
  void givenInvalidFlavor_shouldFail() {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getPostgres().setFlavor(
        "glassfish");

    checkErrorCause(StackGresClusterPostgres.class, "spec.postgres.flavor",
        review, ValidEnum.class);
  }

  @Test
  void givenValidFeatureGate_shouldPass() throws ValidationFailed {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().setNonProductionOptions(
        new StackGresClusterNonProduction());
    review.getRequest().getObject().getSpec().getNonProductionOptions().setEnabledFeatureGates(
        Lists.newArrayList(StackGresFeatureGates.BABELFISH_FLAVOR.toString()));

    validator.validate(review);
  }

  @Test
  void givenInvalidFeatureGate_shouldFail() {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().setNonProductionOptions(
        new StackGresClusterNonProduction());
    review.getRequest().getObject().getSpec().getNonProductionOptions().setEnabledFeatureGates(
        Lists.newArrayList("glassfish-flavor"));

    checkErrorCause(StackGresClusterNonProduction.class,
        "spec.nonProductionOptions.enabledFeatureGates",
        review, ValidEnumList.class);
  }

  @Test
  void givenMissingReplicationMode_shouldFail() {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getReplication().setMode(null);

    checkErrorCause(StackGresClusterReplication.class,
        "spec.replication.mode",
        review, ValidEnum.class);
  }

  @Test
  void givenMissingReplicationRole_shouldPass() throws ValidationFailed {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getReplication().setRole(null);

    validator.validate(review);
  }

  @Test
  void givenInstancesGreatherThanSyncInstances_shouldPass() throws ValidationFailed {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getCoordinator().setInstances(2);
    review.getRequest().getObject().getSpec().getShards().setInstancesPerCluster(2);
    review.getRequest().getObject().getSpec().getReplication().setMode(
        StackGresReplicationMode.SYNC.toString());
    review.getRequest().getObject().getSpec().getReplication().setSyncInstances(1);

    validator.validate(review);
  }

  @Test
  void nullCoordinatorResourceProfile_shouldFail() {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getCoordinator().setResourceProfile(null);

    checkErrorCause(StackGresClusterSpec.class,
        "spec.coordinator.sgInstanceProfile",
        "isResourceProfilePresent", review,
        AssertTrue.class);
  }

  @Test
  void nullCoordinatorVolumeSize_shouldFail() {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getCoordinator()
        .getPod().getPersistentVolume().setSize(null);

    checkNotNullErrorCause(StackGresPodPersistentVolume.class,
        "spec.coordinator.pods.persistentVolume.size",
        review);
  }

  @Test
  void invalidCoordinatorVolumeSize_shouldFail() {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getCoordinator()
        .getPod().getPersistentVolume().setSize("512");

    checkErrorCause(StackGresPodPersistentVolume.class,
        "spec.coordinator.pods.persistentVolume.size",
        review, Pattern.class);
  }

  @Test
  void validCoordinatorNodeSelector_shouldPass() throws ValidationFailed {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getCoordinator().getPod()
        .setScheduling(new StackGresClusterPodScheduling());
    review.getRequest().getObject().getSpec().getCoordinator().getPod().getScheduling()
        .setNodeSelector(new HashMap<>());
    review.getRequest().getObject().getSpec().getCoordinator().getPod().getScheduling()
        .getNodeSelector()
        .put("test", "true");

    validator.validate(review);
  }

  @Test
  void validCoordinatorToleration_shouldPass() throws ValidationFailed {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getCoordinator().getPod()
        .setScheduling(new StackGresClusterPodScheduling());
    review.getRequest().getObject().getSpec().getCoordinator().getPod().getScheduling()
        .setTolerations(new ArrayList<>());
    review.getRequest().getObject().getSpec().getCoordinator().getPod().getScheduling()
        .getTolerations()
        .add(new Toleration());
    review.getRequest().getObject().getSpec().getCoordinator().getPod().getScheduling()
        .getTolerations().get(0)
        .setKey("test");

    validator.validate(review);
  }

  @Test
  void validCoordinatorTolerationKeyEmpty_shouldPass() throws ValidationFailed {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getCoordinator().getPod()
        .setScheduling(new StackGresClusterPodScheduling());
    review.getRequest().getObject().getSpec().getCoordinator().getPod().getScheduling()
        .setTolerations(new ArrayList<>());
    review.getRequest().getObject().getSpec().getCoordinator().getPod().getScheduling()
        .getTolerations()
        .add(new Toleration());
    review.getRequest().getObject().getSpec().getCoordinator().getPod().getScheduling()
        .getTolerations().get(0)
        .setKey("");
    review.getRequest().getObject().getSpec().getCoordinator().getPod().getScheduling()
        .getTolerations().get(0)
        .setOperator("Exists");

    validator.validate(review);
  }

  @Test
  void invalidCoordinatorTolerationKeyEmpty_shouldFail() {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getCoordinator().getPod()
        .setScheduling(new StackGresClusterPodScheduling());
    review.getRequest().getObject().getSpec().getCoordinator().getPod().getScheduling()
        .setTolerations(new ArrayList<>());
    review.getRequest().getObject().getSpec().getCoordinator().getPod().getScheduling()
        .getTolerations()
        .add(new Toleration());
    review.getRequest().getObject().getSpec().getCoordinator().getPod().getScheduling()
        .getTolerations().get(0)
        .setKey("");

    checkErrorCause(Toleration.class,
        new String[] {"spec.coordinator.pods.scheduling.tolerations[0].key",
            "spec.coordinator.pods.scheduling.tolerations[0].operator"},
        "isOperatorExistsWhenKeyIsEmpty", review,
        AssertTrue.class);
  }

  @Test
  void invalidCoordinatorTolerationOperator_shouldFail() {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getCoordinator().getPod()
        .setScheduling(new StackGresClusterPodScheduling());
    review.getRequest().getObject().getSpec().getCoordinator().getPod().getScheduling()
        .setTolerations(new ArrayList<>());
    review.getRequest().getObject().getSpec().getCoordinator().getPod().getScheduling()
        .getTolerations()
        .add(new Toleration());
    review.getRequest().getObject().getSpec().getCoordinator().getPod().getScheduling()
        .getTolerations().get(0)
        .setKey("test");
    review.getRequest().getObject().getSpec().getCoordinator().getPod().getScheduling()
        .getTolerations().get(0)
        .setOperator("NotExists");

    checkErrorCause(Toleration.class, "spec.coordinator.pods.scheduling.tolerations[0].operator",
        "isOperatorValid", review, AssertTrue.class);
  }

  @Test
  void invalidCoordinatorTolerationEffect_shouldFail() {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getCoordinator().getPod()
        .setScheduling(new StackGresClusterPodScheduling());
    review.getRequest().getObject().getSpec().getCoordinator().getPod().getScheduling()
        .setTolerations(new ArrayList<>());
    review.getRequest().getObject().getSpec().getCoordinator().getPod().getScheduling()
        .getTolerations()
        .add(new Toleration());
    review.getRequest().getObject().getSpec().getCoordinator().getPod().getScheduling()
        .getTolerations().get(0)
        .setKey("test");
    review.getRequest().getObject().getSpec().getCoordinator().getPod().getScheduling()
        .getTolerations().get(0)
        .setEffect("NeverSchedule");

    checkErrorCause(Toleration.class, "spec.coordinator.pods.scheduling.tolerations[0].effect",
        "isEffectValid", review, AssertTrue.class);
  }

  @Test
  void givenCoordinatorTolerationsSetAndEffectNoExecute_shouldPass() throws ValidationFailed {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getCoordinator().getPod()
        .setScheduling(new StackGresClusterPodScheduling());
    review.getRequest().getObject().getSpec().getCoordinator().getPod().getScheduling()
        .setTolerations(new ArrayList<>());
    review.getRequest().getObject().getSpec().getCoordinator().getPod().getScheduling()
        .getTolerations()
        .add(new Toleration());
    review.getRequest().getObject().getSpec().getCoordinator().getPod().getScheduling()
        .getTolerations().get(0)
        .setKey("test");
    review.getRequest().getObject().getSpec().getCoordinator().getPod().getScheduling()
        .getTolerations().get(0)
        .setTolerationSeconds(100L);
    review.getRequest().getObject().getSpec().getCoordinator().getPod().getScheduling()
        .getTolerations().get(0)
        .setEffect("NoExecute");

    validator.validate(review);
  }

  @Test
  void givenCoordinatorTolerationsSetAndEffectOtherThanNoExecute_shouldFail() {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getCoordinator().getPod()
        .setScheduling(new StackGresClusterPodScheduling());
    review.getRequest().getObject().getSpec().getCoordinator().getPod().getScheduling()
        .setTolerations(new ArrayList<>());
    review.getRequest().getObject().getSpec().getCoordinator().getPod().getScheduling()
        .getTolerations()
        .add(new Toleration());
    review.getRequest().getObject().getSpec().getCoordinator().getPod().getScheduling()
        .getTolerations().get(0)
        .setKey("test");
    review.getRequest().getObject().getSpec().getCoordinator().getPod().getScheduling()
        .getTolerations().get(0)
        .setTolerationSeconds(100L);
    review.getRequest().getObject().getSpec().getCoordinator().getPod().getScheduling()
        .getTolerations().get(0)
        .setEffect(new Random().nextBoolean() ? "NoSchedule" : "PreferNoSchedule");

    checkErrorCause(Toleration.class, "spec.coordinator.pods.scheduling.tolerations[0].effect",
        "isEffectNoExecuteIfTolerationIsSet", review, AssertTrue.class);
  }

  @Test
  void givenCoordinatorInstancesEqualsToSyncInstances_shouldFail() {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getCoordinator().setInstances(1);
    review.getRequest().getObject().getSpec().getReplication().setMode(
        StackGresReplicationMode.SYNC.toString());
    review.getRequest().getObject().getSpec().getReplication().setSyncInstances(1);

    checkErrorCause(StackGresShardedClusterSpec.class,
        "spec.replication.syncInstances",
        "isSupportingRequiredSynchronousReplicas",
        review, AssertTrue.class);
  }

  @Test
  void givenCoordinatorInstancesLessThanSyncInstances_shouldFail() {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getCoordinator().setInstances(1);
    review.getRequest().getObject().getSpec().getReplication().setMode(
        StackGresReplicationMode.SYNC.toString());
    review.getRequest().getObject().getSpec().getReplication().setSyncInstances(2);

    checkErrorCause(StackGresShardedClusterSpec.class,
        "spec.replication.syncInstances",
        "isSupportingRequiredSynchronousReplicas",
        review, AssertTrue.class);
  }

  @Test
  void givenCoordinatorInstancesEqualsToCoordinatorSyncInstances_shouldFail() {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getCoordinator().setInstances(1);
    review.getRequest().getObject().getSpec().getCoordinator()
        .setReplicationForCoordinator(new StackGresShardedClusterReplication());
    review.getRequest().getObject().getSpec().getCoordinator().getReplicationForCoordinator()
        .setMode(StackGresReplicationMode.SYNC.toString());
    review.getRequest().getObject().getSpec().getCoordinator().getReplicationForCoordinator()
        .setSyncInstances(1);

    checkErrorCause(StackGresShardedClusterCoordinator.class,
        "spec.coordinator.replication.syncInstances",
        "isCoordinatorSupportingRequiredSynchronousReplicas",
        review, AssertTrue.class);
  }

  @Test
  void givenCoordinatorInstancesLessThanCoordinatorSyncInstances_shouldFail() {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getCoordinator().setInstances(1);
    review.getRequest().getObject().getSpec().getCoordinator()
        .setReplicationForCoordinator(new StackGresShardedClusterReplication());
    review.getRequest().getObject().getSpec().getCoordinator().getReplicationForCoordinator()
        .setMode(StackGresReplicationMode.SYNC.toString());
    review.getRequest().getObject().getSpec().getCoordinator().getReplicationForCoordinator()
        .setSyncInstances(2);

    checkErrorCause(StackGresShardedClusterCoordinator.class,
        "spec.coordinator.replication.syncInstances",
        "isCoordinatorSupportingRequiredSynchronousReplicas",
        review, AssertTrue.class);
  }

  @Test
  void givenCoordinatorNullSyncInstances_shouldFail() {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getCoordinator().setInstances(2);
    review.getRequest().getObject().getSpec().getReplication().setMode(
        StackGresReplicationMode.SYNC.toString());
    review.getRequest().getObject().getSpec().getReplication().setSyncInstances(null);

    checkErrorCause(StackGresShardedClusterReplication.class,
        "spec.replication.syncInstances",
        "isSyncInstancesSetForSyncMode",
        review, AssertTrue.class);
  }

  @Test
  void givenCoordinatorSyncInstancesLessThanOne_shouldFail() {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getCoordinator().setInstances(2);
    review.getRequest().getObject().getSpec().getReplication().setMode(
        StackGresReplicationMode.SYNC.toString());
    review.getRequest().getObject().getSpec().getReplication().setSyncInstances(0);

    checkErrorCause(StackGresClusterReplication.class,
        "spec.replication.syncInstances",
        review, Min.class, "must be greater than or equal to 1");
  }

  @Test
  void nullShardsResourceProfile_shouldFail() {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getShards().setResourceProfile(null);

    checkErrorCause(StackGresClusterSpec.class,
        "spec.shards.sgInstanceProfile",
        "isResourceProfilePresent", review,
        AssertTrue.class);
  }

  @Test
  void nullShardsVolumeSize_shouldFail() {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getShards()
        .getPod().getPersistentVolume().setSize(null);

    checkNotNullErrorCause(StackGresPodPersistentVolume.class,
        "spec.shards.pods.persistentVolume.size",
        review);
  }

  @Test
  void invalidShardsVolumeSize_shouldFail() {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getShards()
        .getPod().getPersistentVolume().setSize("512");

    checkErrorCause(StackGresPodPersistentVolume.class,
        "spec.shards.pods.persistentVolume.size",
        review, Pattern.class);
  }

  @Test
  void validShardsNodeSelector_shouldPass() throws ValidationFailed {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getShards().getPod()
        .setScheduling(new StackGresClusterPodScheduling());
    review.getRequest().getObject().getSpec().getShards().getPod().getScheduling()
        .setNodeSelector(new HashMap<>());
    review.getRequest().getObject().getSpec().getShards().getPod().getScheduling()
        .getNodeSelector()
        .put("test", "true");

    validator.validate(review);
  }

  @Test
  void validShardsToleration_shouldPass() throws ValidationFailed {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getShards().getPod()
        .setScheduling(new StackGresClusterPodScheduling());
    review.getRequest().getObject().getSpec().getShards().getPod().getScheduling()
        .setTolerations(new ArrayList<>());
    review.getRequest().getObject().getSpec().getShards().getPod().getScheduling()
        .getTolerations()
        .add(new Toleration());
    review.getRequest().getObject().getSpec().getShards().getPod().getScheduling()
        .getTolerations().get(0)
        .setKey("test");

    validator.validate(review);
  }

  @Test
  void validShardsTolerationKeyEmpty_shouldPass() throws ValidationFailed {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getShards().getPod()
        .setScheduling(new StackGresClusterPodScheduling());
    review.getRequest().getObject().getSpec().getShards().getPod().getScheduling()
        .setTolerations(new ArrayList<>());
    review.getRequest().getObject().getSpec().getShards().getPod().getScheduling()
        .getTolerations()
        .add(new Toleration());
    review.getRequest().getObject().getSpec().getShards().getPod().getScheduling()
        .getTolerations().get(0)
        .setKey("");
    review.getRequest().getObject().getSpec().getShards().getPod().getScheduling()
        .getTolerations().get(0)
        .setOperator("Exists");

    validator.validate(review);
  }

  @Test
  void invalidShardsTolerationKeyEmpty_shouldFail() {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getShards().getPod()
        .setScheduling(new StackGresClusterPodScheduling());
    review.getRequest().getObject().getSpec().getShards().getPod().getScheduling()
        .setTolerations(new ArrayList<>());
    review.getRequest().getObject().getSpec().getShards().getPod().getScheduling()
        .getTolerations()
        .add(new Toleration());
    review.getRequest().getObject().getSpec().getShards().getPod().getScheduling()
        .getTolerations().get(0)
        .setKey("");

    checkErrorCause(Toleration.class,
        new String[] {"spec.shards.pods.scheduling.tolerations[0].key",
            "spec.shards.pods.scheduling.tolerations[0].operator"},
        "isOperatorExistsWhenKeyIsEmpty", review,
        AssertTrue.class);
  }

  @Test
  void invalidShardsTolerationOperator_shouldFail() {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getShards().getPod()
        .setScheduling(new StackGresClusterPodScheduling());
    review.getRequest().getObject().getSpec().getShards().getPod().getScheduling()
        .setTolerations(new ArrayList<>());
    review.getRequest().getObject().getSpec().getShards().getPod().getScheduling()
        .getTolerations()
        .add(new Toleration());
    review.getRequest().getObject().getSpec().getShards().getPod().getScheduling()
        .getTolerations().get(0)
        .setKey("test");
    review.getRequest().getObject().getSpec().getShards().getPod().getScheduling()
        .getTolerations().get(0)
        .setOperator("NotExists");

    checkErrorCause(Toleration.class, "spec.shards.pods.scheduling.tolerations[0].operator",
        "isOperatorValid", review, AssertTrue.class);
  }

  @Test
  void invalidShardsTolerationEffect_shouldFail() {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getShards().getPod()
        .setScheduling(new StackGresClusterPodScheduling());
    review.getRequest().getObject().getSpec().getShards().getPod().getScheduling()
        .setTolerations(new ArrayList<>());
    review.getRequest().getObject().getSpec().getShards().getPod().getScheduling()
        .getTolerations()
        .add(new Toleration());
    review.getRequest().getObject().getSpec().getShards().getPod().getScheduling()
        .getTolerations().get(0)
        .setKey("test");
    review.getRequest().getObject().getSpec().getShards().getPod().getScheduling()
        .getTolerations().get(0)
        .setEffect("NeverSchedule");

    checkErrorCause(Toleration.class, "spec.shards.pods.scheduling.tolerations[0].effect",
        "isEffectValid", review, AssertTrue.class);
  }

  @Test
  void givenShardsTolerationsSetAndEffectNoExecute_shouldPass() throws ValidationFailed {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getShards().getPod()
        .setScheduling(new StackGresClusterPodScheduling());
    review.getRequest().getObject().getSpec().getShards().getPod().getScheduling()
        .setTolerations(new ArrayList<>());
    review.getRequest().getObject().getSpec().getShards().getPod().getScheduling()
        .getTolerations()
        .add(new Toleration());
    review.getRequest().getObject().getSpec().getShards().getPod().getScheduling()
        .getTolerations().get(0)
        .setKey("test");
    review.getRequest().getObject().getSpec().getShards().getPod().getScheduling()
        .getTolerations().get(0)
        .setTolerationSeconds(100L);
    review.getRequest().getObject().getSpec().getShards().getPod().getScheduling()
        .getTolerations().get(0)
        .setEffect("NoExecute");

    validator.validate(review);
  }

  @Test
  void givenShardsTolerationsSetAndEffectOtherThanNoExecute_shouldFail() {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getShards().getPod()
        .setScheduling(new StackGresClusterPodScheduling());
    review.getRequest().getObject().getSpec().getShards().getPod().getScheduling()
        .setTolerations(new ArrayList<>());
    review.getRequest().getObject().getSpec().getShards().getPod().getScheduling()
        .getTolerations()
        .add(new Toleration());
    review.getRequest().getObject().getSpec().getShards().getPod().getScheduling()
        .getTolerations().get(0)
        .setKey("test");
    review.getRequest().getObject().getSpec().getShards().getPod().getScheduling()
        .getTolerations().get(0)
        .setTolerationSeconds(100L);
    review.getRequest().getObject().getSpec().getShards().getPod().getScheduling()
        .getTolerations().get(0)
        .setEffect(new Random().nextBoolean() ? "NoSchedule" : "PreferNoSchedule");

    checkErrorCause(Toleration.class, "spec.shards.pods.scheduling.tolerations[0].effect",
        "isEffectNoExecuteIfTolerationIsSet", review, AssertTrue.class);
  }

  @Test
  void givenShardsInstancesPerClusterEqualsToSyncInstances_shouldFail() {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getShards().setInstancesPerCluster(1);
    review.getRequest().getObject().getSpec().getReplication().setMode(
        StackGresReplicationMode.SYNC.toString());
    review.getRequest().getObject().getSpec().getReplication().setSyncInstances(1);

    checkErrorCause(StackGresShardedClusterSpec.class,
        "spec.replication.syncInstances",
        "isSupportingRequiredSynchronousReplicas",
        review, AssertTrue.class);
  }

  @Test
  void givenShardsInstancesPerClusterLessThanSyncInstances_shouldFail() {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getShards().setInstancesPerCluster(1);
    review.getRequest().getObject().getSpec().getReplication().setMode(
        StackGresReplicationMode.SYNC.toString());
    review.getRequest().getObject().getSpec().getReplication().setSyncInstances(2);

    checkErrorCause(StackGresShardedClusterSpec.class,
        "spec.replication.syncInstances",
        "isSupportingRequiredSynchronousReplicas",
        review, AssertTrue.class);
  }

  @Test
  void givenShardsInstancesPerClusterEqualsToShardsSyncInstances_shouldFail() {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getShards().setInstancesPerCluster(1);
    review.getRequest().getObject().getSpec().getShards()
        .setReplicationForShards(new StackGresShardedClusterReplication());
    review.getRequest().getObject().getSpec().getShards().getReplicationForShards()
        .setMode(StackGresReplicationMode.SYNC.toString());
    review.getRequest().getObject().getSpec().getShards().getReplicationForShards()
        .setSyncInstances(1);

    checkErrorCause(StackGresShardedClusterShards.class,
        "spec.shards.replication.syncInstances",
        "isShardsSupportingRequiredSynchronousReplicas",
        review, AssertTrue.class);
  }

  @Test
  void givenShardsInstancesPerClusterLessThanShardsSyncInstances_shouldFail() {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getShards().setInstancesPerCluster(1);
    review.getRequest().getObject().getSpec().getShards()
        .setReplicationForShards(new StackGresShardedClusterReplication());
    review.getRequest().getObject().getSpec().getShards().getReplicationForShards()
        .setMode(StackGresReplicationMode.SYNC.toString());
    review.getRequest().getObject().getSpec().getShards().getReplicationForShards()
        .setSyncInstances(2);

    checkErrorCause(StackGresShardedClusterShards.class,
        "spec.shards.replication.syncInstances",
        "isShardsSupportingRequiredSynchronousReplicas",
        review, AssertTrue.class);
  }

  @Test
  void givenShardsNullSyncInstances_shouldFail() {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getShards().setInstancesPerCluster(2);
    review.getRequest().getObject().getSpec().getReplication().setMode(
        StackGresReplicationMode.SYNC.toString());
    review.getRequest().getObject().getSpec().getReplication().setSyncInstances(null);

    checkErrorCause(StackGresShardedClusterReplication.class,
        "spec.replication.syncInstances",
        "isSyncInstancesSetForSyncMode",
        review, AssertTrue.class);
  }

  @Test
  void givenShardsSyncInstancesLessThanOne_shouldFail() {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getShards().setInstancesPerCluster(2);
    review.getRequest().getObject().getSpec().getReplication().setMode(
        StackGresReplicationMode.SYNC.toString());
    review.getRequest().getObject().getSpec().getReplication().setSyncInstances(0);

    checkErrorCause(StackGresClusterReplication.class,
        "spec.replication.syncInstances",
        review, Min.class, "must be greater than or equal to 1");
  }

  @Test
  void nullOverridesShardsResourceProfile_shouldPass() throws ValidationFailed {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getShards()
        .setOverrides(List.of(new StackGresShardedClusterShard()));
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0).setResourceProfile(null);

    validator.validate(review);
  }

  @Test
  void nullOverridesShardsVolumeSize_shouldPass() throws ValidationFailed {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getShards()
        .setOverrides(List.of(new StackGresShardedClusterShard()));
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0).setPodForShards(new StackGresShardedClusterShardPod());
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0)
        .getPodForShards().setPersistentVolume(null);

    validator.validate(review);
  }

  @Test
  void nullOverridesShardsVolumeSize_shouldFail() {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getShards()
        .setOverrides(List.of(new StackGresShardedClusterShard()));
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0).setPodForShards(new StackGresShardedClusterShardPod());
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0).getPodForShards()
        .setPersistentVolume(new StackGresPodPersistentVolume());
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0)
        .getPodForShards().getPersistentVolume().setSize(null);

    checkNotNullErrorCause(StackGresPodPersistentVolume.class,
        "spec.shards.overrides[0].pods.persistentVolume.size",
        review);
  }

  @Test
  void invalidOverridesShardsVolumeSize_shouldFail() {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getShards()
        .setOverrides(List.of(new StackGresShardedClusterShard()));
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0).setPodForShards(new StackGresShardedClusterShardPod());
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0).getPodForShards()
        .setPersistentVolume(new StackGresPodPersistentVolume());
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0)
        .getPodForShards().getPersistentVolume().setSize("512");

    checkErrorCause(StackGresPodPersistentVolume.class,
        "spec.shards.overrides[0].pods.persistentVolume.size",
        review, Pattern.class);
  }

  @Test
  void validOverridesShardsNodeSelector_shouldPass() throws ValidationFailed {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getShards()
        .setOverrides(List.of(new StackGresShardedClusterShard()));
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0).setPodForShards(new StackGresShardedClusterShardPod());
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0).getPodForShards()
        .setScheduling(new StackGresClusterPodScheduling());
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0).getPodForShards()
        .getScheduling().setNodeSelector(new HashMap<>());
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0).getPodForShards()
        .getScheduling().getNodeSelector()
        .put("test", "true");

    validator.validate(review);
  }

  @Test
  void validOverridesShardsToleration_shouldPass() throws ValidationFailed {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getShards()
        .setOverrides(List.of(new StackGresShardedClusterShard()));
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0).setPodForShards(new StackGresShardedClusterShardPod());
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0).getPodForShards()
        .setScheduling(new StackGresClusterPodScheduling());
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0).getPodForShards()
        .getScheduling().setTolerations(new ArrayList<>());
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0).getPodForShards()
        .getScheduling().getTolerations()
        .add(new Toleration());
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0).getPodForShards()
        .getScheduling()
        .getTolerations().get(0)
        .setKey("test");

    validator.validate(review);
  }

  @Test
  void validOverridesShardsTolerationKeyEmpty_shouldPass() throws ValidationFailed {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getShards()
        .setOverrides(List.of(new StackGresShardedClusterShard()));
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0).setPodForShards(new StackGresShardedClusterShardPod());
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0).getPodForShards()
        .setScheduling(new StackGresClusterPodScheduling());
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0).getPodForShards()
        .getScheduling().setTolerations(new ArrayList<>());
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0).getPodForShards()
        .getScheduling().getTolerations()
        .add(new Toleration());
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0).getPodForShards()
        .getScheduling().getTolerations().get(0)
        .setKey("");
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0).getPodForShards()
        .getScheduling().getTolerations().get(0)
        .setOperator("Exists");

    validator.validate(review);
  }

  @Test
  void invalidOverridesShardsTolerationKeyEmpty_shouldFail() {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getShards()
        .setOverrides(List.of(new StackGresShardedClusterShard()));
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0).setPodForShards(new StackGresShardedClusterShardPod());
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0).getPodForShards()
        .setScheduling(new StackGresClusterPodScheduling());
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0).getPodForShards()
        .getScheduling().setTolerations(new ArrayList<>());
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0).getPodForShards()
        .getScheduling().getTolerations()
        .add(new Toleration());
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0).getPodForShards()
        .getScheduling().getTolerations().get(0)
        .setKey("");

    checkErrorCause(Toleration.class,
        new String[] {
            "spec.shards.overrides[0].pods.scheduling.tolerations[0].key",
            "spec.shards.overrides[0].pods.scheduling.tolerations[0].operator"},
        "isOperatorExistsWhenKeyIsEmpty", review,
        AssertTrue.class);
  }

  @Test
  void invalidOverridesShardsTolerationOperator_shouldFail() {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getShards()
        .setOverrides(List.of(new StackGresShardedClusterShard()));
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0).setPodForShards(new StackGresShardedClusterShardPod());
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0).getPodForShards()
        .setScheduling(new StackGresClusterPodScheduling());
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0).getPodForShards()
        .getScheduling().setTolerations(new ArrayList<>());
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0).getPodForShards()
        .getScheduling().getTolerations()
        .add(new Toleration());
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0).getPodForShards()
        .getScheduling().getTolerations().get(0)
        .setKey("test");
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0).getPodForShards()
        .getScheduling().getTolerations().get(0)
        .setOperator("NotExists");

    checkErrorCause(Toleration.class,
        "spec.shards.overrides[0].pods.scheduling.tolerations[0].operator",
        "isOperatorValid", review, AssertTrue.class);
  }

  @Test
  void invalidOverridesShardsTolerationEffect_shouldFail() {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getShards()
        .setOverrides(List.of(new StackGresShardedClusterShard()));
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0).setPodForShards(new StackGresShardedClusterShardPod());
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0).getPodForShards()
        .setScheduling(new StackGresClusterPodScheduling());
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0).getPodForShards()
        .getScheduling().setTolerations(new ArrayList<>());
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0).getPodForShards()
        .getScheduling().getTolerations()
        .add(new Toleration());
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0).getPodForShards()
        .getScheduling().getTolerations().get(0)
        .setKey("test");
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0).getPodForShards()
        .getScheduling().getTolerations().get(0)
        .setEffect("NeverSchedule");

    checkErrorCause(Toleration.class,
        "spec.shards.overrides[0].pods.scheduling.tolerations[0].effect",
        "isEffectValid", review, AssertTrue.class);
  }

  @Test
  void givenOverridesShardsTolerationsSetAndEffectNoExecute_shouldPass() throws ValidationFailed {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getShards()
        .setOverrides(List.of(new StackGresShardedClusterShard()));
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0).setPodForShards(new StackGresShardedClusterShardPod());
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0).getPodForShards()
        .setScheduling(new StackGresClusterPodScheduling());
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0).getPodForShards()
        .getScheduling().setTolerations(new ArrayList<>());
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0).getPodForShards()
        .getScheduling().getTolerations()
        .add(new Toleration());
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0).getPodForShards()
        .getScheduling().getTolerations().get(0)
        .setKey("test");
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0).getPodForShards()
        .getScheduling().getTolerations().get(0)
        .setTolerationSeconds(100L);
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0).getPodForShards()
        .getScheduling().getTolerations().get(0)
        .setEffect("NoExecute");

    validator.validate(review);
  }

  @Test
  void givenOverridesShardsTolerationsSetAndEffectOtherThanNoExecute_shouldFail() {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getShards()
        .setOverrides(List.of(new StackGresShardedClusterShard()));
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0).setPodForShards(new StackGresShardedClusterShardPod());
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0).getPodForShards()
        .setScheduling(new StackGresClusterPodScheduling());
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0).getPodForShards()
        .getScheduling().setTolerations(new ArrayList<>());
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0).getPodForShards()
        .getScheduling().getTolerations()
        .add(new Toleration());
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0).getPodForShards()
        .getScheduling().getTolerations().get(0)
        .setKey("test");
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0).getPodForShards()
        .getScheduling().getTolerations().get(0)
        .setTolerationSeconds(100L);
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0).getPodForShards()
        .getScheduling().getTolerations().get(0)
        .setEffect(new Random().nextBoolean() ? "NoSchedule" : "PreferNoSchedule");

    checkErrorCause(Toleration.class,
        "spec.shards.overrides[0].pods.scheduling.tolerations[0].effect",
        "isEffectNoExecuteIfTolerationIsSet", review, AssertTrue.class);
  }

  @Test
  void givenOverridesShardsInstancesPerClusterEqualsToSyncInstances_shouldFail() {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getShards()
        .setOverrides(List.of(new StackGresShardedClusterShard()));
    review.getRequest().getObject().getSpec().getShards()
        .setInstancesPerCluster(3);
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0).setInstancesPerCluster(1);
    review.getRequest().getObject().getSpec().getReplication().setMode(
        StackGresReplicationMode.SYNC.toString());
    review.getRequest().getObject().getSpec().getReplication().setSyncInstances(1);

    checkErrorCause(StackGresShardedClusterSpec.class,
        "spec.replication.syncInstances",
        "isSupportingRequiredSynchronousReplicas",
        review, AssertTrue.class);
  }

  @Test
  void givenOverridesShardsInstancesPerClusterLessThanSyncInstances_shouldFail() {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getShards()
        .setOverrides(List.of(new StackGresShardedClusterShard()));
    review.getRequest().getObject().getSpec().getShards()
        .setInstancesPerCluster(3);
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0).setInstancesPerCluster(1);
    review.getRequest().getObject().getSpec().getReplication().setMode(
        StackGresReplicationMode.SYNC.toString());
    review.getRequest().getObject().getSpec().getReplication().setSyncInstances(2);

    checkErrorCause(StackGresShardedClusterSpec.class,
        "spec.replication.syncInstances",
        "isSupportingRequiredSynchronousReplicas",
        review, AssertTrue.class);
  }

  @Test
  void givenOverridesShardsInstancesPerClusterEqualsToShardsSyncInstances_shouldFail() {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getShards()
        .setOverrides(List.of(new StackGresShardedClusterShard()));
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0).setInstancesPerCluster(1);
    review.getRequest().getObject().getSpec().getShards()
        .setReplicationForShards(new StackGresShardedClusterReplication());
    review.getRequest().getObject().getSpec().getShards()
        .getReplicationForShards()
        .setMode(StackGresReplicationMode.SYNC.toString());
    review.getRequest().getObject().getSpec().getShards()
        .getReplicationForShards()
        .setSyncInstances(1);

    checkErrorCause(StackGresShardedClusterShard.class,
        "spec.shards.replication.syncInstances",
        "isShardsOverrideSupportingRequiredSynchronousReplicas",
        review, AssertTrue.class);
  }

  @Test
  void givenOverridesShardsInstancesPerClusterEqualsToOverridesShardsSyncInstances_shouldFail() {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getShards()
        .setOverrides(List.of(new StackGresShardedClusterShard()));
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0).setInstancesPerCluster(1);
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0)
        .setReplicationForShards(new StackGresShardedClusterReplication());
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0).getReplicationForShards()
        .setMode(StackGresReplicationMode.SYNC.toString());
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0).getReplicationForShards()
        .setSyncInstances(1);

    checkErrorCause(StackGresShardedClusterShard.class,
        "spec.shards.overrides[0].replication.syncInstances",
        "isShardsOverrideSupportingRequiredSynchronousReplicas",
        review, AssertTrue.class);
  }

  @Test
  void givenOverridesShardsInstancesPerClusterLessThanShardsSyncInstances_shouldFail() {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getShards()
        .setOverrides(List.of(new StackGresShardedClusterShard()));
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0).setInstancesPerCluster(1);
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0)
        .setReplicationForShards(new StackGresShardedClusterReplication());
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0).getReplicationForShards()
        .setMode(StackGresReplicationMode.SYNC.toString());
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0).getReplicationForShards()
        .setSyncInstances(2);

    checkErrorCause(StackGresShardedClusterShard.class,
        "spec.shards.overrides[0].replication.syncInstances",
        "isShardsOverrideSupportingRequiredSynchronousReplicas",
        review, AssertTrue.class);
  }

  @Test
  void givenOverridesShardsNullSyncInstances_shouldFail() {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getShards()
        .setOverrides(List.of(new StackGresShardedClusterShard()));
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0).setInstancesPerCluster(2);
    review.getRequest().getObject().getSpec().getReplication().setMode(
        StackGresReplicationMode.SYNC.toString());
    review.getRequest().getObject().getSpec().getReplication().setSyncInstances(null);

    checkErrorCause(StackGresShardedClusterReplication.class,
        "spec.replication.syncInstances",
        "isSyncInstancesSetForSyncMode",
        review, AssertTrue.class);
  }

  @Test
  void givenOverridesShardsSyncInstancesLessThanOne_shouldFail() {
    StackGresShardedClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getShards()
        .setOverrides(List.of(new StackGresShardedClusterShard()));
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0).setInstancesPerCluster(2);
    review.getRequest().getObject().getSpec().getReplication().setMode(
        StackGresReplicationMode.SYNC.toString());
    review.getRequest().getObject().getSpec().getReplication().setSyncInstances(0);

    checkErrorCause(StackGresClusterReplication.class,
        "spec.replication.syncInstances",
        review, Min.class, "must be greater than or equal to 1");
  }

}
