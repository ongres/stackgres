/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Predicate;

import io.stackgres.common.crd.SecretKeySelector;
import io.stackgres.common.crd.Toleration;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterBackupConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterInitData;
import io.stackgres.common.crd.sgcluster.StackGresClusterNonProduction;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodScheduling;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplication;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicationGroup;
import io.stackgres.common.crd.sgcluster.StackGresClusterRestoreFromBackup;
import io.stackgres.common.crd.sgcluster.StackGresClusterRestorePitr;
import io.stackgres.common.crd.sgcluster.StackGresClusterScriptEntry;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterSsl;
import io.stackgres.common.crd.sgcluster.StackGresFeatureGates;
import io.stackgres.common.crd.sgcluster.StackGresPodPersistentVolume;
import io.stackgres.common.crd.sgcluster.StackGresPostgresFlavor;
import io.stackgres.common.crd.sgcluster.StackGresReplicationMode;
import io.stackgres.common.crd.sgcluster.StackGresReplicationRole;
import io.stackgres.common.validation.ValidEnum;
import io.stackgres.common.validation.ValidEnumList;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.validation.ConstraintValidationTest;
import io.stackgres.operator.validation.ConstraintValidator;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.gradle.internal.impldep.com.google.common.collect.Lists;
import org.jooq.lambda.tuple.Tuple;
import org.junit.jupiter.api.Test;

class ClusterConstraintValidatorTest extends ConstraintValidationTest<StackGresClusterReview> {

  @Override
  protected ConstraintValidator<StackGresClusterReview> buildValidator() {
    return new ClusterConstraintValidator();
  }

  @Override
  protected StackGresClusterReview getValidReview() {
    return AdmissionReviewFixtures.cluster().loadCreate().get();
  }

  @Override
  protected StackGresClusterReview getInvalidReview() {
    final StackGresClusterReview review = AdmissionReviewFixtures.cluster().loadCreate().get();

    review.getRequest().getObject().setSpec(null);
    return review;
  }

  @Test
  void nullSpec_shouldFail() {
    StackGresClusterReview review = getValidReview();
    review.getRequest().getObject().setSpec(null);

    checkNotNullErrorCause(StackGresCluster.class, "spec", review);
  }

  @Test
  void nullResourceProfile_shouldFail() {
    StackGresClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().setResourceProfile(null);

    checkErrorCause(StackGresClusterSpec.class,
        "spec.sgInstanceProfile",
        "isResourceProfilePresent", review,
        AssertTrue.class);
  }

  @Test
  void nullVolumeSize_shouldFail() {
    StackGresClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getPod().getPersistentVolume().setSize(null);

    checkNotNullErrorCause(StackGresPodPersistentVolume.class, "spec.pods.persistentVolume.size",
        review);
  }

  @Test
  void invalidVolumeSize_shouldFail() {
    StackGresClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getPod().getPersistentVolume().setSize("512");

    checkErrorCause(StackGresPodPersistentVolume.class, "spec.pods.persistentVolume.size",
        review, Pattern.class);
  }

  @Test
  void validNodeSelector_shouldPass() throws ValidationFailed {
    StackGresClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getPod()
        .setScheduling(new StackGresClusterPodScheduling());
    review.getRequest().getObject().getSpec().getPod().getScheduling()
        .setNodeSelector(new HashMap<>());
    review.getRequest().getObject().getSpec().getPod().getScheduling().getNodeSelector()
        .put("test", "true");

    validator.validate(review);
  }

  @Test
  void validToleration_shouldPass() throws ValidationFailed {
    StackGresClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getPod()
        .setScheduling(new StackGresClusterPodScheduling());
    review.getRequest().getObject().getSpec().getPod().getScheduling()
        .setTolerations(new ArrayList<>());
    review.getRequest().getObject().getSpec().getPod().getScheduling().getTolerations()
        .add(new Toleration());
    review.getRequest().getObject().getSpec().getPod().getScheduling().getTolerations().get(0)
        .setKey("test");

    validator.validate(review);
  }

  @Test
  void validTolerationKeyEmpty_shouldPass() throws ValidationFailed {
    StackGresClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getPod()
        .setScheduling(new StackGresClusterPodScheduling());
    review.getRequest().getObject().getSpec().getPod().getScheduling()
        .setTolerations(new ArrayList<>());
    review.getRequest().getObject().getSpec().getPod().getScheduling().getTolerations()
        .add(new Toleration());
    review.getRequest().getObject().getSpec().getPod().getScheduling().getTolerations().get(0)
        .setKey("");
    review.getRequest().getObject().getSpec().getPod().getScheduling().getTolerations().get(0)
        .setOperator("Exists");

    validator.validate(review);
  }

  @Test
  void invalidTolerationKeyEmpty_shouldFail() {
    StackGresClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getPod()
        .setScheduling(new StackGresClusterPodScheduling());
    review.getRequest().getObject().getSpec().getPod().getScheduling()
        .setTolerations(new ArrayList<>());
    review.getRequest().getObject().getSpec().getPod().getScheduling().getTolerations()
        .add(new Toleration());
    review.getRequest().getObject().getSpec().getPod().getScheduling().getTolerations().get(0)
        .setKey("");

    checkErrorCause(Toleration.class,
        new String[] {"spec.pods.scheduling.tolerations[0].key",
            "spec.pods.scheduling.tolerations[0].operator"},
        "isOperatorExistsWhenKeyIsEmpty", review,
        AssertTrue.class);
  }

  @Test
  void invalidTolerationOperator_shouldFail() {
    StackGresClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getPod()
        .setScheduling(new StackGresClusterPodScheduling());
    review.getRequest().getObject().getSpec().getPod().getScheduling()
        .setTolerations(new ArrayList<>());
    review.getRequest().getObject().getSpec().getPod().getScheduling().getTolerations()
        .add(new Toleration());
    review.getRequest().getObject().getSpec().getPod().getScheduling().getTolerations().get(0)
        .setKey("test");
    review.getRequest().getObject().getSpec().getPod().getScheduling().getTolerations().get(0)
        .setOperator("NotExists");

    checkErrorCause(Toleration.class, "spec.pods.scheduling.tolerations[0].operator",
        "isOperatorValid", review, AssertTrue.class);
  }

  @Test
  void invalidTolerationEffect_shouldFail() {
    StackGresClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getPod()
        .setScheduling(new StackGresClusterPodScheduling());
    review.getRequest().getObject().getSpec().getPod().getScheduling()
        .setTolerations(new ArrayList<>());
    review.getRequest().getObject().getSpec().getPod().getScheduling().getTolerations()
        .add(new Toleration());
    review.getRequest().getObject().getSpec().getPod().getScheduling().getTolerations().get(0)
        .setKey("test");
    review.getRequest().getObject().getSpec().getPod().getScheduling().getTolerations().get(0)
        .setEffect("NeverSchedule");

    checkErrorCause(Toleration.class, "spec.pods.scheduling.tolerations[0].effect",
        "isEffectValid", review, AssertTrue.class);
  }

  @Test
  void sslCertificateSecretWithEmptyName_shouldFail() {
    StackGresClusterReview review = getValidReview();
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
    StackGresClusterReview review = getValidReview();
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
  void givenTolerationsSetAndEffectNoExecute_shouldPass() throws ValidationFailed {
    StackGresClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getPod()
        .setScheduling(new StackGresClusterPodScheduling());
    review.getRequest().getObject().getSpec().getPod().getScheduling()
        .setTolerations(new ArrayList<>());
    review.getRequest().getObject().getSpec().getPod().getScheduling().getTolerations()
        .add(new Toleration());
    review.getRequest().getObject().getSpec().getPod().getScheduling().getTolerations().get(0)
        .setKey("test");
    review.getRequest().getObject().getSpec().getPod().getScheduling().getTolerations().get(0)
        .setTolerationSeconds(100L);
    review.getRequest().getObject().getSpec().getPod().getScheduling().getTolerations().get(0)
        .setEffect("NoExecute");

    validator.validate(review);
  }

  @Test
  void givenTolerationsSetAndEffectOtherThanNoExecute_shouldFail() {
    StackGresClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getPod()
        .setScheduling(new StackGresClusterPodScheduling());
    review.getRequest().getObject().getSpec().getPod().getScheduling()
        .setTolerations(new ArrayList<>());
    review.getRequest().getObject().getSpec().getPod().getScheduling().getTolerations()
        .add(new Toleration());
    review.getRequest().getObject().getSpec().getPod().getScheduling().getTolerations().get(0)
        .setKey("test");
    review.getRequest().getObject().getSpec().getPod().getScheduling().getTolerations().get(0)
        .setTolerationSeconds(100L);
    review.getRequest().getObject().getSpec().getPod().getScheduling().getTolerations().get(0)
        .setEffect(new Random().nextBoolean() ? "NoSchedule" : "PreferNoSchedule");

    checkErrorCause(Toleration.class, "spec.pods.scheduling.tolerations[0].effect",
        "isEffectNoExecuteIfTolerationIsSet", review, AssertTrue.class);
  }

  @Test
  void givenValidFlavor_shouldPass() throws ValidationFailed {
    StackGresClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getPostgres().setFlavor(
        StackGresPostgresFlavor.BABELFISH.toString());

    validator.validate(review);
  }

  @Test
  void givenInvalidFlavor_shouldFail() {
    StackGresClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getPostgres().setFlavor(
        "glassfish");

    checkErrorCause(StackGresClusterPostgres.class, "spec.postgres.flavor",
        review, ValidEnum.class);
  }

  @Test
  void givenValidFeatureGate_shouldPass() throws ValidationFailed {
    StackGresClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().setNonProductionOptions(
        new StackGresClusterNonProduction());
    review.getRequest().getObject().getSpec().getNonProductionOptions().setEnabledFeatureGates(
        Lists.newArrayList(StackGresFeatureGates.BABELFISH_FLAVOR.toString()));

    validator.validate(review);
  }

  @Test
  void givenInvalidFeatureGate_shouldFail() {
    StackGresClusterReview review = getValidReview();
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
    StackGresClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getReplication().setMode(null);

    checkErrorCause(StackGresClusterReplication.class,
        "spec.replication.mode",
        review, ValidEnum.class);
  }

  @Test
  void givenMissingReplicationRole_shouldFail() {
    StackGresClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getReplication().setRole(null);

    checkErrorCause(StackGresClusterReplication.class,
        "spec.replication.role",
        "isRoleValid",
        review, AssertTrue.class);
  }

  @Test
  void givenValidGroup_shouldPass() throws ValidationFailed {
    StackGresClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().setInstances(2);
    review.getRequest().getObject().getSpec().getReplication().setGroups(new ArrayList<>());
    review.getRequest().getObject().getSpec().getReplication().getGroups()
    .add(new StackGresClusterReplicationGroup());
    review.getRequest().getObject().getSpec().getReplication().getGroups().get(0)
    .setName("group-1");
    review.getRequest().getObject().getSpec().getReplication().getGroups().get(0)
    .setRole(StackGresReplicationRole.READONLY.toString());
    review.getRequest().getObject().getSpec().getReplication().getGroups().get(0).setInstances(1);

    validator.validate(review);
  }

  @Test
  void givenGroupWithouName_shouldFail() {
    StackGresClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().setInstances(2);
    review.getRequest().getObject().getSpec().getReplication().setGroups(new ArrayList<>());
    review.getRequest().getObject().getSpec().getReplication().getGroups()
    .add(new StackGresClusterReplicationGroup());
    review.getRequest().getObject().getSpec().getReplication().getGroups().get(0)
    .setRole(StackGresReplicationRole.READONLY.toString());
    review.getRequest().getObject().getSpec().getReplication().getGroups().get(0).setInstances(1);

    checkErrorCause(StackGresClusterReplicationGroup.class,
        "spec.replication.groups[0].name",
        review, NotNull.class, "must not be null");
  }

  @Test
  void givenGroupWithouRole_shouldFail() {
    StackGresClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().setInstances(2);
    review.getRequest().getObject().getSpec().getReplication().setGroups(new ArrayList<>());
    review.getRequest().getObject().getSpec().getReplication().getGroups()
    .add(new StackGresClusterReplicationGroup());
    review.getRequest().getObject().getSpec().getReplication().getGroups().get(0)
    .setName("group-1");
    review.getRequest().getObject().getSpec().getReplication().getGroups().get(0).setInstances(1);

    checkErrorCause(StackGresClusterReplicationGroup.class,
        "spec.replication.groups[0].role",
        review, ValidEnum.class);
  }

  @Test
  void givenGroupWithouInstancesLessThanOne_shouldFail() {
    StackGresClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().setInstances(2);
    review.getRequest().getObject().getSpec().getReplication().setGroups(new ArrayList<>());
    review.getRequest().getObject().getSpec().getReplication().getGroups()
    .add(new StackGresClusterReplicationGroup());
    review.getRequest().getObject().getSpec().getReplication().getGroups().get(0)
    .setName("group-1");
    review.getRequest().getObject().getSpec().getReplication().getGroups().get(0)
    .setRole(StackGresReplicationRole.READONLY.toString());
    review.getRequest().getObject().getSpec().getReplication().getGroups().get(0).setInstances(0);

    checkErrorCause(StackGresClusterReplicationGroup.class,
        "spec.replication.groups[0].instances",
        review, Positive.class);
  }

  @Test
  void givenInstancesEqualsToGroupInstances_shouldFail() {
    StackGresClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().setInstances(1);
    review.getRequest().getObject().getSpec().getReplication().setGroups(new ArrayList<>());
    review.getRequest().getObject().getSpec().getReplication().getGroups()
    .add(new StackGresClusterReplicationGroup());
    review.getRequest().getObject().getSpec().getReplication().getGroups().get(0)
    .setName("group-1");
    review.getRequest().getObject().getSpec().getReplication().getGroups().get(0)
    .setRole(StackGresReplicationRole.READONLY.toString());
    review.getRequest().getObject().getSpec().getReplication().getGroups().get(0).setInstances(1);

    checkErrorCause(StackGresClusterSpec.class,
        "spec.instances",
        "isSupportingInstancesForInstancesInReplicationGroups",
        review, AssertTrue.class);
  }

  @Test
  void givenInstancesLessThanGroupInstances_shouldFail() {
    StackGresClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().setInstances(1);
    review.getRequest().getObject().getSpec().getReplication().setGroups(new ArrayList<>());
    review.getRequest().getObject().getSpec().getReplication().getGroups()
    .add(new StackGresClusterReplicationGroup());
    review.getRequest().getObject().getSpec().getReplication().getGroups().get(0)
    .setName("group-1");
    review.getRequest().getObject().getSpec().getReplication().getGroups().get(0)
    .setRole(StackGresReplicationRole.READONLY.toString());
    review.getRequest().getObject().getSpec().getReplication().getGroups().get(0).setInstances(2);

    checkErrorCause(StackGresClusterSpec.class,
        "spec.instances",
        "isSupportingInstancesForInstancesInReplicationGroups",
        review, AssertTrue.class);
  }

  @Test
  void givenInstancesGreatherThanSyncInstances_shouldPass() throws ValidationFailed {
    StackGresClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().setInstances(2);
    review.getRequest().getObject().getSpec().getReplication().setMode(
        StackGresReplicationMode.SYNC.toString());
    review.getRequest().getObject().getSpec().getReplication().setSyncInstances(1);

    validator.validate(review);
  }

  @Test
  void givenInstancesEqualsToSyncInstances_shouldFail() {
    StackGresClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().setInstances(1);
    review.getRequest().getObject().getSpec().getReplication().setMode(
        StackGresReplicationMode.SYNC.toString());
    review.getRequest().getObject().getSpec().getReplication().setSyncInstances(1);

    checkErrorCause(StackGresClusterSpec.class,
        "spec.instances",
        "isSupportingRequiredSynchronousReplicas",
        review, AssertTrue.class);
  }

  @Test
  void givenInstancesLessThanSyncInstances_shouldFail() {
    StackGresClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().setInstances(1);
    review.getRequest().getObject().getSpec().getReplication().setMode(
        StackGresReplicationMode.SYNC.toString());
    review.getRequest().getObject().getSpec().getReplication().setSyncInstances(2);

    checkErrorCause(StackGresClusterSpec.class,
        "spec.instances",
        "isSupportingRequiredSynchronousReplicas",
        review, AssertTrue.class);
  }

  @Test
  void givenNullSyncInstances_shouldFail() {
    StackGresClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().setInstances(2);
    review.getRequest().getObject().getSpec().getReplication().setMode(
        StackGresReplicationMode.SYNC.toString());
    review.getRequest().getObject().getSpec().getReplication().setSyncInstances(null);

    checkErrorCause(StackGresClusterReplication.class,
        "spec.replication.syncInstances",
        "isSyncInstancesSetForSyncMode",
        review, AssertTrue.class);
  }

  @Test
  void givenSyncInstancesLessThanOne_shouldFail() {
    StackGresClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().setInstances(2);
    review.getRequest().getObject().getSpec().getReplication().setMode(
        StackGresReplicationMode.SYNC.toString());
    review.getRequest().getObject().getSpec().getReplication().setSyncInstances(0);

    checkErrorCause(StackGresClusterReplication.class,
        "spec.replication.syncInstances",
        review, Min.class, "must be greater than or equal to 1");
  }

  @Test
  void fromBackupWithTargetName_shouldPass() throws ValidationFailed {
    StackGresClusterReview review = getValidReview();
    setTargetName(review);

    validator.validate(review);
  }

  @Test
  void fromBackupWithTargetXid_shouldPass() throws ValidationFailed {
    StackGresClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getInitData().getRestore().getFromBackup()
        .setTargetXid("test");

    validator.validate(review);
  }

  @Test
  void fromBackupWithTargetLsn_shouldPass() throws ValidationFailed {
    StackGresClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getInitData().getRestore().getFromBackup()
        .setTargetLsn("test");

    validator.validate(review);
  }

  @Test
  void fromBackupWithPointInTimeRecovery_shouldPass() throws ValidationFailed {
    StackGresClusterReview review = getValidReview();
    setPointInTimeRecovery(review);

    validator.validate(review);
  }

  @Test
  void nullRestoreToTimestamp_shouldFail() {
    StackGresClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getInitData().getRestore().getFromBackup()
        .setPointInTimeRecovery(new StackGresClusterRestorePitr());

    checkErrorCause(StackGresClusterRestorePitr.class,
        "spec.initialData.restore.fromBackup.pointInTimeRecovery.restoreToTimestamp",
        review, NotNull.class, "must not be null");
  }

  @Test
  void givenTwoRestoreTargets_shouldFail() {
    StackGresClusterReview review = getValidReview();
    var map = Map.<String, Consumer<StackGresClusterReview>>of(
        "targetName", this::setTargetName,
        "targetXid", this::setTargetXid,
        "targetLsn", this::setTargetLsn,
        "pointInTimeRecovery", this::setPointInTimeRecovery);
    map
        .entrySet()
        .stream()
        .flatMap(entry -> map.entrySet()
            .stream()
            .filter(Predicate.not(entry::equals))
            .map(otherEntry -> Tuple.tuple(entry, otherEntry)))
        .forEach(t -> {
          resetRestoreFromBackup(review);
          t.v1.getValue().accept(review);
          t.v2.getValue().accept(review);

          checkErrorCause(StackGresClusterRestoreFromBackup.class,
              new String[] {
                  "spec.initialData.restore.fromBackup.targetName",
                  "spec.initialData.restore.fromBackup.targetXid",
                  "spec.initialData.restore.fromBackup.targetLsn",
                  "spec.initialData.restore.fromBackup.pointInTimeRecovery",
              },
              "isJustOneTarget",
              review, AssertTrue.class,
              "targetName, targetLsn, targetXid pointInTimeRecovery"
                  + " are mutually exclusive");
        });
  }

  private void resetRestoreFromBackup(StackGresClusterReview review) {
    review.getRequest().getObject().getSpec().getInitData().getRestore()
        .setFromBackup(new StackGresClusterRestoreFromBackup());
    review.getRequest().getObject().getSpec().getInitData().getRestore().getFromBackup()
        .setName("test");
  }

  private void setTargetName(StackGresClusterReview review) {
    review.getRequest().getObject().getSpec().getInitData().getRestore().getFromBackup()
        .setTargetName("test");
  }

  private void setTargetXid(StackGresClusterReview review) {
    review.getRequest().getObject().getSpec().getInitData().getRestore().getFromBackup()
        .setTargetXid("test");
  }

  private void setTargetLsn(StackGresClusterReview review) {
    review.getRequest().getObject().getSpec().getInitData().getRestore().getFromBackup()
        .setTargetLsn("test");
  }

  private void setPointInTimeRecovery(StackGresClusterReview review) {
    review.getRequest().getObject().getSpec().getInitData().getRestore().getFromBackup()
        .setPointInTimeRecovery(new StackGresClusterRestorePitr());
    review.getRequest().getObject().getSpec().getInitData().getRestore().getFromBackup()
        .getPointInTimeRecovery().setRestoreToTimestamp("2022-04-16T17:27:22Z");
  }

  @Test
  void givenInvalidPitrRestoreTimestamp_shouldFail() {
    StackGresClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getInitData().getRestore().getFromBackup()
        .setPointInTimeRecovery(new StackGresClusterRestorePitr());
    review.getRequest().getObject().getSpec().getInitData().getRestore().getFromBackup()
        .getPointInTimeRecovery().setRestoreToTimestamp("mié 06 abr 2022 17:27:22 CEST");

    checkErrorCause(StackGresClusterRestorePitr.class,
        "spec.initialData.restore.fromBackup.pointInTimeRecovery.restoreToTimestamp",
        "isRestoreToTimestampValid",
        review, AssertTrue.class,
        "restoreToTimestamp must be in ISO 8601 date format: `YYYY-MM-DDThh:mm:ss.ddZ`.");
  }

  @Test
  void givenBothRestoreBakcupNameAndUid_shouldFail() {
    StackGresClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getInitData().getRestore().getFromBackup()
        .setName("test");
    review.getRequest().getObject().getSpec().getInitData().getRestore().getFromBackup()
        .setUid("test");

    checkErrorCause(StackGresClusterRestoreFromBackup.class,
        "spec.initialData.restore.fromBackup.name",
        "isNameNotNullOrUidNotNull",
        review, AssertTrue.class,
        "name cannot be null");
  }

  @Test
  void givenMissingRestoreBakcupNameAndUid_shouldFail() {
    StackGresClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getInitData().getRestore().getFromBackup()
        .setName(null);
    review.getRequest().getObject().getSpec().getInitData().getRestore().getFromBackup()
        .setUid(null);

    checkErrorCause(StackGresClusterRestoreFromBackup.class,
        "spec.initialData.restore.fromBackup.name",
        "isNameNotNullOrUidNotNull",
        review, AssertTrue.class,
        "name cannot be null");
  }

  @Test
  void givenNullBackupPathWhenSgBackupConfigNotNull_shouldFail() {
    StackGresClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getConfiguration().setBackupPath(null);

    checkErrorCause(StackGresClusterConfiguration.class,
        "spec.configurations.backupPath",
        "isBackupPathSetWhenSgBackupConfigIsSet",
        review, AssertTrue.class);
  }

  @Test
  void givenNullBackupPathOnBackups_shouldFail() {
    StackGresClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getConfiguration().setBackupConfig(null);
    review.getRequest().getObject().getSpec().getConfiguration().setBackupPath(null);
    review.getRequest().getObject().getSpec().getConfiguration().setBackups(new ArrayList<>());
    review.getRequest().getObject().getSpec().getConfiguration().getBackups()
        .add(new StackGresClusterBackupConfiguration());
    review.getRequest().getObject().getSpec().getConfiguration().getBackups().get(0)
        .setObjectStorage("test");

    checkErrorCause(StackGresClusterBackupConfiguration.class,
        "spec.configurations.backups[0].path",
        review, NotNull.class, "must not be null");
  }

  @Test
  void givenNullObjectStorageOnBackups_shouldFail() {
    StackGresClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getConfiguration().setBackupConfig(null);
    review.getRequest().getObject().getSpec().getConfiguration().setBackupPath(null);
    review.getRequest().getObject().getSpec().getConfiguration().setBackups(new ArrayList<>());
    review.getRequest().getObject().getSpec().getConfiguration().getBackups()
        .add(new StackGresClusterBackupConfiguration());
    review.getRequest().getObject().getSpec().getConfiguration().getBackups().get(0)
        .setPath("test");

    checkErrorCause(StackGresClusterBackupConfiguration.class,
        "spec.configurations.backups[0].sgObjectStorage",
        review, NotNull.class, "must not be null");
  }

  @Test
  void notNullInitialDataScripts_shouldFail() {
    StackGresClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().setInitData(new StackGresClusterInitData());
    review.getRequest().getObject().getSpec().getInitData().setScripts(new ArrayList<>());
    review.getRequest().getObject().getSpec().getInitData().getScripts()
        .add(new StackGresClusterScriptEntry());
    review.getRequest().getObject().getSpec().getInitData().getScripts().get(0)
        .setScript("SELECT 1");

    checkErrorCause(StackGresClusterInitData.class, "spec.initialData.scripts",
        review, Null.class);
  }

}
