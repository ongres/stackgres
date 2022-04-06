/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Positive;

import io.stackgres.common.crd.ConfigMapKeySelector;
import io.stackgres.common.crd.SecretKeySelector;
import io.stackgres.common.crd.Toleration;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterInitData;
import io.stackgres.common.crd.sgcluster.StackGresClusterNonProduction;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodScheduling;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplication;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicationGroup;
import io.stackgres.common.crd.sgcluster.StackGresClusterRestorePitr;
import io.stackgres.common.crd.sgcluster.StackGresClusterScriptEntry;
import io.stackgres.common.crd.sgcluster.StackGresClusterScriptFrom;
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
import io.stackgres.operator.validation.ConstraintValidationTest;
import io.stackgres.operator.validation.ConstraintValidator;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.testutil.JsonUtil;
import org.gradle.internal.impldep.com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;

class ClusterConstraintValidatorTest extends ConstraintValidationTest<StackGresClusterReview> {

  @Override
  protected ConstraintValidator<StackGresClusterReview> buildValidator() {
    return new ClusterConstraintValidator();
  }

  @Override
  protected StackGresClusterReview getValidReview() {
    return JsonUtil.readFromJson("cluster_allow_requests/valid_creation.json",
        StackGresClusterReview.class);
  }

  @Override
  protected StackGresClusterReview getInvalidReview() {
    final StackGresClusterReview review = JsonUtil
        .readFromJson("cluster_allow_requests/valid_creation.json",
            StackGresClusterReview.class);

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

    checkNotNullErrorCause(StackGresClusterSpec.class, "spec.resourceProfile", review);
  }

  @Test
  void nullVolumeSize_shouldFail() {
    StackGresClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getPod().getPersistentVolume().setSize(null);

    checkNotNullErrorCause(StackGresPodPersistentVolume.class, "spec.pod.persistentVolume.size",
        review);
  }

  @Test
  void invalidVolumeSize_shouldFail() {
    StackGresClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getPod().getPersistentVolume().setSize("512");

    checkErrorCause(StackGresPodPersistentVolume.class, "spec.pod.persistentVolume.size",
        review, Pattern.class);
  }

  @Test
  void validScript_shouldPass() throws ValidationFailed {
    StackGresClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().setInitData(new StackGresClusterInitData());
    review.getRequest().getObject().getSpec().getInitData().setScripts(new ArrayList<>());
    review.getRequest().getObject().getSpec().getInitData().getScripts()
        .add(new StackGresClusterScriptEntry());
    review.getRequest().getObject().getSpec().getInitData().getScripts().get(0)
        .setScript("SELECT 1");

    validator.validate(review);
  }

  @Test
  void missingScript_shouldFail() {
    StackGresClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().setInitData(new StackGresClusterInitData());
    review.getRequest().getObject().getSpec().getInitData().setScripts(new ArrayList<>());
    review.getRequest().getObject().getSpec().getInitData().getScripts()
        .add(new StackGresClusterScriptEntry());

    checkErrorCause(StackGresClusterScriptEntry.class,
        new String[] {"spec.initData.scripts[0].script",
            "spec.initData.scripts[0].scriptFrom"},
        "isScriptMutuallyExclusiveAndRequired", review, AssertTrue.class);
  }

  @Test
  void validScriptAndScriptFrom_shouldFail() {
    StackGresClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().setInitData(new StackGresClusterInitData());
    review.getRequest().getObject().getSpec().getInitData().setScripts(new ArrayList<>());
    review.getRequest().getObject().getSpec().getInitData().getScripts()
        .add(new StackGresClusterScriptEntry());
    review.getRequest().getObject().getSpec().getInitData().getScripts().get(0)
        .setScriptFrom(new StackGresClusterScriptFrom());
    review.getRequest().getObject().getSpec().getInitData().getScripts().get(0)
        .setScript("SELECT 1");
    review.getRequest().getObject().getSpec().getInitData().getScripts().get(0).getScriptFrom()
        .setConfigMapKeyRef(new ConfigMapKeySelector());
    review.getRequest().getObject().getSpec().getInitData().getScripts().get(0).getScriptFrom()
        .getConfigMapKeyRef().setName("test");
    review.getRequest().getObject().getSpec().getInitData().getScripts().get(0).getScriptFrom()
        .getConfigMapKeyRef().setKey("test");

    checkErrorCause(StackGresClusterScriptEntry.class,
        new String[] {"spec.initData.scripts[0].script",
            "spec.initData.scripts[0].scriptFrom"},
        "isScriptMutuallyExclusiveAndRequired", review, AssertTrue.class);
  }

  @Test
  void scriptWithEmptyDatabaseName_shouldFail() throws ValidationFailed {
    StackGresClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().setInitData(new StackGresClusterInitData());
    review.getRequest().getObject().getSpec().getInitData().setScripts(new ArrayList<>());
    review.getRequest().getObject().getSpec().getInitData().getScripts()
        .add(new StackGresClusterScriptEntry());
    review.getRequest().getObject().getSpec().getInitData().getScripts().get(0).setDatabase("");
    review.getRequest().getObject().getSpec().getInitData().getScripts().get(0)
        .setScript("SELECT 1");

    checkErrorCause(StackGresClusterScriptEntry.class,
        new String[] {"spec.initData.scripts[0].database"},
        "isDatabaseNameNonEmpty", review, AssertTrue.class);
  }

  @Test
  void validScriptFromConfigMap_shouldPass() throws ValidationFailed {
    StackGresClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().setInitData(new StackGresClusterInitData());
    review.getRequest().getObject().getSpec().getInitData().setScripts(new ArrayList<>());
    review.getRequest().getObject().getSpec().getInitData().getScripts()
        .add(new StackGresClusterScriptEntry());
    review.getRequest().getObject().getSpec().getInitData().getScripts().get(0)
        .setScriptFrom(new StackGresClusterScriptFrom());
    review.getRequest().getObject().getSpec().getInitData().getScripts().get(0).getScriptFrom()
        .setConfigMapKeyRef(new ConfigMapKeySelector());
    review.getRequest().getObject().getSpec().getInitData().getScripts().get(0).getScriptFrom()
        .getConfigMapKeyRef().setName("test");
    review.getRequest().getObject().getSpec().getInitData().getScripts().get(0).getScriptFrom()
        .getConfigMapKeyRef().setKey("test");

    validator.validate(review);
  }

  @Test
  void validScriptFromSecret_shouldPass() throws ValidationFailed {
    StackGresClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().setInitData(new StackGresClusterInitData());
    review.getRequest().getObject().getSpec().getInitData().setScripts(new ArrayList<>());
    review.getRequest().getObject().getSpec().getInitData().getScripts()
        .add(new StackGresClusterScriptEntry());
    review.getRequest().getObject().getSpec().getInitData().getScripts().get(0)
        .setScriptFrom(new StackGresClusterScriptFrom());
    review.getRequest().getObject().getSpec().getInitData().getScripts().get(0).getScriptFrom()
        .setSecretKeyRef(new SecretKeySelector());
    review.getRequest().getObject().getSpec().getInitData().getScripts().get(0).getScriptFrom()
        .getSecretKeyRef().setName("test");
    review.getRequest().getObject().getSpec().getInitData().getScripts().get(0).getScriptFrom()
        .getSecretKeyRef().setKey("test");

    validator.validate(review);
  }

  @Test
  void missingScriptFrom_shouldFail() {
    StackGresClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().setInitData(new StackGresClusterInitData());
    review.getRequest().getObject().getSpec().getInitData().setScripts(new ArrayList<>());
    review.getRequest().getObject().getSpec().getInitData().getScripts()
        .add(new StackGresClusterScriptEntry());
    review.getRequest().getObject().getSpec().getInitData().getScripts().get(0)
        .setScriptFrom(new StackGresClusterScriptFrom());

    checkErrorCause(StackGresClusterScriptFrom.class,
        new String[] {"spec.initData.scripts[0].scriptFrom.secretKeyRef",
            "spec.initData.scripts[0].scriptFrom.configMapKeyRef"},
        "isSecretKeySelectorAndConfigMapKeySelectorMutuallyExclusiveAndRequired",
        review, AssertTrue.class);
  }

  @Test
  void validScriptFromConfigMapAndSecret_shouldFail() {
    StackGresClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().setInitData(new StackGresClusterInitData());
    review.getRequest().getObject().getSpec().getInitData().setScripts(new ArrayList<>());
    review.getRequest().getObject().getSpec().getInitData().getScripts()
        .add(new StackGresClusterScriptEntry());
    review.getRequest().getObject().getSpec().getInitData().getScripts().get(0)
        .setScriptFrom(new StackGresClusterScriptFrom());
    review.getRequest().getObject().getSpec().getInitData().getScripts().get(0).getScriptFrom()
        .setConfigMapKeyRef(new ConfigMapKeySelector());
    review.getRequest().getObject().getSpec().getInitData().getScripts().get(0).getScriptFrom()
        .getConfigMapKeyRef().setName("test");
    review.getRequest().getObject().getSpec().getInitData().getScripts().get(0).getScriptFrom()
        .getConfigMapKeyRef().setKey("test");
    review.getRequest().getObject().getSpec().getInitData().getScripts().get(0).getScriptFrom()
        .setSecretKeyRef(new SecretKeySelector());
    review.getRequest().getObject().getSpec().getInitData().getScripts().get(0).getScriptFrom()
        .getSecretKeyRef().setName("test");
    review.getRequest().getObject().getSpec().getInitData().getScripts().get(0).getScriptFrom()
        .getSecretKeyRef().setKey("test");

    checkErrorCause(StackGresClusterScriptFrom.class,
        new String[] {"spec.initData.scripts[0].scriptFrom.secretKeyRef",
            "spec.initData.scripts[0].scriptFrom.configMapKeyRef"},
        "isSecretKeySelectorAndConfigMapKeySelectorMutuallyExclusiveAndRequired",
        review, AssertTrue.class);
  }

  @Test
  void scriptFromConfigMapWithEmptyKey_shouldFail() {
    StackGresClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().setInitData(new StackGresClusterInitData());
    review.getRequest().getObject().getSpec().getInitData().setScripts(new ArrayList<>());
    review.getRequest().getObject().getSpec().getInitData().getScripts()
        .add(new StackGresClusterScriptEntry());
    review.getRequest().getObject().getSpec().getInitData().getScripts().get(0)
        .setScriptFrom(new StackGresClusterScriptFrom());
    review.getRequest().getObject().getSpec().getInitData().getScripts().get(0).getScriptFrom()
        .setConfigMapKeyRef(new ConfigMapKeySelector());
    review.getRequest().getObject().getSpec().getInitData().getScripts().get(0).getScriptFrom()
        .getConfigMapKeyRef().setName("test");
    review.getRequest().getObject().getSpec().getInitData().getScripts().get(0).getScriptFrom()
        .getConfigMapKeyRef().setKey("");

    checkErrorCause(SecretKeySelector.class,
        "spec.initData.scripts[0].scriptFrom.configMapKeyRef.key",
        "isKeyNotEmpty", review, AssertTrue.class);
  }

  @Test
  void scriptFromConfigMapWithEmptyName_shouldFail() {
    StackGresClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().setInitData(new StackGresClusterInitData());
    review.getRequest().getObject().getSpec().getInitData().setScripts(new ArrayList<>());
    review.getRequest().getObject().getSpec().getInitData().getScripts()
        .add(new StackGresClusterScriptEntry());
    review.getRequest().getObject().getSpec().getInitData().getScripts().get(0)
        .setScriptFrom(new StackGresClusterScriptFrom());
    review.getRequest().getObject().getSpec().getInitData().getScripts().get(0).getScriptFrom()
        .setConfigMapKeyRef(new ConfigMapKeySelector());
    review.getRequest().getObject().getSpec().getInitData().getScripts().get(0).getScriptFrom()
        .getConfigMapKeyRef().setName("");
    review.getRequest().getObject().getSpec().getInitData().getScripts().get(0).getScriptFrom()
        .getConfigMapKeyRef().setKey("test");

    checkErrorCause(SecretKeySelector.class,
        "spec.initData.scripts[0].scriptFrom.configMapKeyRef.name",
        "isNameNotEmpty", review, AssertTrue.class);
  }

  @Test
  void scriptFromSecretWithEmptyKey_shouldFail() {
    StackGresClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().setInitData(new StackGresClusterInitData());
    review.getRequest().getObject().getSpec().getInitData().setScripts(new ArrayList<>());
    review.getRequest().getObject().getSpec().getInitData().getScripts()
        .add(new StackGresClusterScriptEntry());
    review.getRequest().getObject().getSpec().getInitData().getScripts().get(0)
        .setScriptFrom(new StackGresClusterScriptFrom());
    review.getRequest().getObject().getSpec().getInitData().getScripts().get(0).getScriptFrom()
        .setSecretKeyRef(new SecretKeySelector());
    review.getRequest().getObject().getSpec().getInitData().getScripts().get(0).getScriptFrom()
        .getSecretKeyRef().setName("test");
    review.getRequest().getObject().getSpec().getInitData().getScripts().get(0).getScriptFrom()
        .getSecretKeyRef().setKey("");

    checkErrorCause(SecretKeySelector.class, "spec.initData.scripts[0].scriptFrom.secretKeyRef.key",
        "isKeyNotEmpty", review, AssertTrue.class);
  }

  @Test
  void scriptFromSecretWithEmptyName_shouldFail() {
    StackGresClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().setInitData(new StackGresClusterInitData());
    review.getRequest().getObject().getSpec().getInitData().setScripts(new ArrayList<>());
    review.getRequest().getObject().getSpec().getInitData().getScripts()
        .add(new StackGresClusterScriptEntry());
    review.getRequest().getObject().getSpec().getInitData().getScripts().get(0)
        .setScriptFrom(new StackGresClusterScriptFrom());
    review.getRequest().getObject().getSpec().getInitData().getScripts().get(0).getScriptFrom()
        .setSecretKeyRef(new SecretKeySelector());
    review.getRequest().getObject().getSpec().getInitData().getScripts().get(0).getScriptFrom()
        .getSecretKeyRef().setName("");
    review.getRequest().getObject().getSpec().getInitData().getScripts().get(0).getScriptFrom()
        .getSecretKeyRef().setKey("test");

    checkErrorCause(SecretKeySelector.class,
        "spec.initData.scripts[0].scriptFrom.secretKeyRef.name",
        "isNameNotEmpty", review, AssertTrue.class);
  }

  @Test
  void validNodeSelector_shouldPass() throws ValidationFailed {
    StackGresClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getPod()
        .setScheduling(new StackGresClusterPodScheduling());
    review.getRequest().getObject().getSpec().getPod().getScheduling()
        .setNodeSelector(new HashMap<>());
    review.getRequest().getObject().getSpec().getPod().getScheduling().getNodeSelector().put("test",
        "true");

    validator.validate(review);
  }

  @Test
  void invalidNodeSelector_shouldFail() {
    StackGresClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getPod()
        .setScheduling(new StackGresClusterPodScheduling());
    review.getRequest().getObject().getSpec().getPod().getScheduling()
        .setNodeSelector(new HashMap<>());

    checkErrorCause(StackGresClusterPodScheduling.class, "spec.pod.scheduling.nodeSelector",
        "isNodeSelectorNotEmpty", review, AssertTrue.class);
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
        new String[] {"spec.pod.scheduling.tolerations[0].key",
            "spec.pod.scheduling.tolerations[0].operator"},
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

    checkErrorCause(Toleration.class, "spec.pod.scheduling.tolerations[0].operator",
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

    checkErrorCause(Toleration.class, "spec.pod.scheduling.tolerations[0].effect",
        "isEffectValid", review, AssertTrue.class);
  }

  @Test
  void sslCertificateSecretNull_shouldFail() {
    StackGresClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getPostgres().setSsl(new StackGresClusterSsl());
    review.getRequest().getObject().getSpec().getPostgres().getSsl().setEnabled(true);
    review.getRequest().getObject().getSpec().getPostgres().getSsl().setPrivateKeySecretKeySelector(
        new SecretKeySelector("test", "test"));

    checkErrorCause(StackGresClusterSsl.class,
        "spec.postgres.ssl.certificateSecretKeySelector",
        "isNotEnabledCertificateSecretKeySelectorRequired", review, AssertTrue.class);
  }

  @Test
  void sslPrivateKeySecretNull_shouldFail() {
    StackGresClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getPostgres().setSsl(new StackGresClusterSsl());
    review.getRequest().getObject().getSpec().getPostgres().getSsl().setEnabled(true);
    review.getRequest().getObject().getSpec().getPostgres().getSsl()
        .setCertificateSecretKeySelector(
            new SecretKeySelector("test", "test"));

    checkErrorCause(StackGresClusterSsl.class,
        "spec.postgres.ssl.privateKeySecretKeySelector",
        "isNotEnabledSecretKeySecretKeySelectorRequired", review, AssertTrue.class);
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

    checkErrorCause(Toleration.class, "spec.pod.scheduling.tolerations[0].effect",
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
        review, ValidEnum.class);
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
  void givenInvalidPitrRestoreTimestamp_shouldFail() {
    StackGresClusterReview review = getValidReview();
    review.getRequest().getObject().getSpec().getInitData().getRestore().getFromBackup()
        .setPointInTimeRecovery(new StackGresClusterRestorePitr());
    review.getRequest().getObject().getSpec().getInitData().getRestore().getFromBackup()
        .getPointInTimeRecovery().setRestoreToTimestamp("mié 06 abr 2022 17:27:22 CEST");

    checkErrorCause(StackGresClusterRestorePitr.class,
        "spec.initData.restore.fromBackup.pointInTimeRecovery.restoreToTimestamp",
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

    checkErrorCause(StackGresClusterRestorePitr.class,
        "spec.initData.restore.fromBackup.name",
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

    checkErrorCause(StackGresClusterRestorePitr.class,
        "spec.initData.restore.fromBackup.name",
        "isNameNotNullOrUidNotNull",
        review, AssertTrue.class,
        "name cannot be null");
  }
}
