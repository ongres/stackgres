/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import javax.validation.constraints.AssertTrue;

import io.stackgres.common.crd.SecretKeySelector;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFrom;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFromExternal;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFromExternalSecretKeyRef;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFromExternalSecretKeyRefs;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFromInstance;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.validation.ConstraintValidationTest;
import io.stackgres.operator.validation.ConstraintValidator;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import org.junit.jupiter.api.Test;

class ClusterReplicateFromConstraintValidatorTest
    extends ConstraintValidationTest<StackGresClusterReview> {

  @Override
  protected ConstraintValidator<StackGresClusterReview> buildValidator() {
    return new ClusterConstraintValidator();
  }

  @Override
  protected StackGresClusterReview getValidReview() {
    var review = AdmissionReviewFixtures.cluster().loadCreate().get();
    review.getRequest().getObject().getSpec()
        .setReplicateFrom(new StackGresClusterReplicateFrom());
    review.getRequest().getObject().getSpec().getReplicateFrom()
        .setInstance(new StackGresClusterReplicateFromInstance());
    review.getRequest().getObject().getSpec().getReplicateFrom().getInstance()
        .setExternal(new StackGresClusterReplicateFromExternal());
    review.getRequest().getObject().getSpec().getReplicateFrom().getInstance().getExternal()
        .setHost("test");
    review.getRequest().getObject().getSpec().getReplicateFrom().getInstance().getExternal()
        .setPort(12345);
    review.getRequest().getObject().getSpec().getReplicateFrom().getInstance().getExternal()
        .setSecretKeyRefs(new StackGresClusterReplicateFromExternalSecretKeyRefs());
    review.getRequest().getObject().getSpec().getReplicateFrom().getInstance().getExternal()
        .getSecretKeyRefs().setSuperuser(new StackGresClusterReplicateFromExternalSecretKeyRef());
    review.getRequest().getObject().getSpec().getReplicateFrom().getInstance().getExternal()
        .getSecretKeyRefs().getSuperuser().setUsername(new SecretKeySelector("test", "test"));
    review.getRequest().getObject().getSpec().getReplicateFrom().getInstance().getExternal()
        .getSecretKeyRefs().getSuperuser().setPassword(new SecretKeySelector("test", "test"));
    review.getRequest().getObject().getSpec().getReplicateFrom().getInstance().getExternal()
        .getSecretKeyRefs().setReplication(new StackGresClusterReplicateFromExternalSecretKeyRef());
    review.getRequest().getObject().getSpec().getReplicateFrom().getInstance().getExternal()
        .getSecretKeyRefs().getReplication().setUsername(new SecretKeySelector("test", "test"));
    review.getRequest().getObject().getSpec().getReplicateFrom().getInstance().getExternal()
        .getSecretKeyRefs().getReplication().setPassword(new SecretKeySelector("test", "test"));
    review.getRequest().getObject().getSpec().getReplicateFrom().getInstance().getExternal()
        .getSecretKeyRefs().setAuthenticator(
            new StackGresClusterReplicateFromExternalSecretKeyRef());
    review.getRequest().getObject().getSpec().getReplicateFrom().getInstance().getExternal()
        .getSecretKeyRefs().getAuthenticator().setUsername(new SecretKeySelector("test", "test"));
    review.getRequest().getObject().getSpec().getReplicateFrom().getInstance().getExternal()
        .getSecretKeyRefs().getAuthenticator().setPassword(new SecretKeySelector("test", "test"));
    return review;
  }

  @Override
  protected StackGresClusterReview getInvalidReview() {
    final StackGresClusterReview review = AdmissionReviewFixtures.cluster().loadCreate().get();

    review.getRequest().getObject().setSpec(null);

    return review;
  }

  @Test
  void validReplicateFrom_shouldPass() throws ValidationFailed {
    StackGresClusterReview review = getValidReview();

    validator.validate(review);
  }

  @Test
  void nullInstance_shouldFail() {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getReplicateFrom().setInstance(null);

    checkNotNullErrorCause(StackGresClusterReplicateFrom.class,
        "spec.replicateFrom.instance", review);
  }

  @Test
  void nullExternal_shouldFail() {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getReplicateFrom().getInstance()
        .setExternal(null);

    checkNotNullErrorCause(StackGresClusterReplicateFromInstance.class,
        "spec.replicateFrom.instance.external", review);
  }

  @Test
  void nullHost_shouldFail() {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getReplicateFrom().getInstance()
        .getExternal().setHost(null);

    checkNotNullErrorCause(StackGresClusterReplicateFromExternal.class,
        "spec.replicateFrom.instance.external.host", review);
  }

  @Test
  void nullPort_shouldFail() {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getReplicateFrom().getInstance()
        .getExternal().setPort(null);

    checkNotNullErrorCause(StackGresClusterReplicateFromExternal.class,
        "spec.replicateFrom.instance.external.port", review);
  }

  @Test
  void nullSecretKeyRefs_shouldFail() {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getReplicateFrom().getInstance()
        .getExternal().setSecretKeyRefs(null);

    checkNotNullErrorCause(StackGresClusterReplicateFromExternal.class,
        "spec.replicateFrom.instance.external.secretKeyRefs", review);
  }

  @Test
  void nullSuperuser_shouldFail() {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getReplicateFrom().getInstance()
        .getExternal().getSecretKeyRefs().setSuperuser(null);

    checkNotNullErrorCause(StackGresClusterReplicateFromExternalSecretKeyRefs.class,
        "spec.replicateFrom.instance.external.secretKeyRefs.superuser", review);
  }

  @Test
  void nullSuperuserUsername_shouldFail() {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getReplicateFrom().getInstance()
        .getExternal().getSecretKeyRefs().getSuperuser().setUsername(null);

    checkNotNullErrorCause(StackGresClusterReplicateFromExternalSecretKeyRef.class,
        "spec.replicateFrom.instance.external.secretKeyRefs.superuser.username", review);
  }

  @Test
  void nullSuperuserPassword_shouldFail() {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getReplicateFrom().getInstance()
        .getExternal().getSecretKeyRefs().getSuperuser().setPassword(null);

    checkNotNullErrorCause(StackGresClusterReplicateFromExternalSecretKeyRef.class,
        "spec.replicateFrom.instance.external.secretKeyRefs.superuser.password", review);
  }

  @Test
  void nullSuperuserUsernameSecretName_shouldFail() {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getReplicateFrom().getInstance()
        .getExternal().getSecretKeyRefs().getSuperuser().getUsername().setName(null);

    checkErrorCause(SecretKeySelector.class,
        "spec.replicateFrom.instance.external.secretKeyRefs.superuser.username.name",
        "isNameNotEmpty", review, AssertTrue.class);
  }

  @Test
  void nullSuperuserUsernameSecretKey_shouldFail() {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getReplicateFrom().getInstance()
        .getExternal().getSecretKeyRefs().getSuperuser().getUsername().setKey(null);

    checkErrorCause(SecretKeySelector.class,
        "spec.replicateFrom.instance.external.secretKeyRefs.superuser.username.key",
        "isKeyNotEmpty", review, AssertTrue.class);
  }

  @Test
  void nullSuperuserPasswordSecretName_shouldFail() {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getReplicateFrom().getInstance()
        .getExternal().getSecretKeyRefs().getSuperuser().getPassword().setName(null);

    checkErrorCause(SecretKeySelector.class,
        "spec.replicateFrom.instance.external.secretKeyRefs.superuser.password.name",
        "isNameNotEmpty", review, AssertTrue.class);
  }

  @Test
  void nullSuperuserPasswordSecretKey_shouldFail() {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getReplicateFrom().getInstance()
        .getExternal().getSecretKeyRefs().getSuperuser().getPassword().setKey(null);

    checkErrorCause(SecretKeySelector.class,
        "spec.replicateFrom.instance.external.secretKeyRefs.superuser.password.key",
        "isKeyNotEmpty", review, AssertTrue.class);
  }

  @Test
  void nullReplication_shouldFail() {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getReplicateFrom().getInstance()
        .getExternal().getSecretKeyRefs().setReplication(null);

    checkNotNullErrorCause(StackGresClusterReplicateFromExternalSecretKeyRefs.class,
        "spec.replicateFrom.instance.external.secretKeyRefs.replication", review);
  }

  @Test
  void nullReplicationUsername_shouldFail() {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getReplicateFrom().getInstance()
        .getExternal().getSecretKeyRefs().getReplication().setUsername(null);

    checkNotNullErrorCause(StackGresClusterReplicateFromExternalSecretKeyRef.class,
        "spec.replicateFrom.instance.external.secretKeyRefs.replication.username", review);
  }

  @Test
  void nullReplicationPassword_shouldFail() {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getReplicateFrom().getInstance()
        .getExternal().getSecretKeyRefs().getReplication().setPassword(null);

    checkNotNullErrorCause(StackGresClusterReplicateFromExternalSecretKeyRef.class,
        "spec.replicateFrom.instance.external.secretKeyRefs.replication.password", review);
  }

  @Test
  void nullReplicationUsernameSecretName_shouldFail() {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getReplicateFrom().getInstance()
        .getExternal().getSecretKeyRefs().getReplication().getUsername().setName(null);

    checkErrorCause(SecretKeySelector.class,
        "spec.replicateFrom.instance.external.secretKeyRefs.replication.username.name",
        "isNameNotEmpty", review, AssertTrue.class);
  }

  @Test
  void nullReplicationUsernameSecretKey_shouldFail() {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getReplicateFrom().getInstance()
        .getExternal().getSecretKeyRefs().getReplication().getUsername().setKey(null);

    checkErrorCause(SecretKeySelector.class,
        "spec.replicateFrom.instance.external.secretKeyRefs.replication.username.key",
        "isKeyNotEmpty", review, AssertTrue.class);
  }

  @Test
  void nullReplicationPasswordSecretName_shouldFail() {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getReplicateFrom().getInstance()
        .getExternal().getSecretKeyRefs().getReplication().getPassword().setName(null);

    checkErrorCause(SecretKeySelector.class,
        "spec.replicateFrom.instance.external.secretKeyRefs.replication.password.name",
        "isNameNotEmpty", review, AssertTrue.class);
  }

  @Test
  void nullReplicationPasswordSecretKey_shouldFail() {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getReplicateFrom().getInstance()
        .getExternal().getSecretKeyRefs().getReplication().getPassword().setKey(null);

    checkErrorCause(SecretKeySelector.class,
        "spec.replicateFrom.instance.external.secretKeyRefs.replication.password.key",
        "isKeyNotEmpty", review, AssertTrue.class);
  }

  @Test
  void nullAuthenticator_shouldFail() {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getReplicateFrom().getInstance()
        .getExternal().getSecretKeyRefs().setAuthenticator(null);

    checkNotNullErrorCause(StackGresClusterReplicateFromExternalSecretKeyRefs.class,
        "spec.replicateFrom.instance.external.secretKeyRefs.authenticator", review);
  }

  @Test
  void nullAuthenticatorUsername_shouldFail() {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getReplicateFrom().getInstance()
        .getExternal().getSecretKeyRefs().getAuthenticator().setUsername(null);

    checkNotNullErrorCause(StackGresClusterReplicateFromExternalSecretKeyRef.class,
        "spec.replicateFrom.instance.external.secretKeyRefs.authenticator.username", review);
  }

  @Test
  void nullAuthenticatorPassword_shouldFail() {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getReplicateFrom().getInstance()
        .getExternal().getSecretKeyRefs().getAuthenticator().setPassword(null);

    checkNotNullErrorCause(StackGresClusterReplicateFromExternalSecretKeyRef.class,
        "spec.replicateFrom.instance.external.secretKeyRefs.authenticator.password", review);
  }

  @Test
  void nullAuthenticatorUsernameSecretName_shouldFail() {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getReplicateFrom().getInstance()
        .getExternal().getSecretKeyRefs().getAuthenticator().getUsername().setName(null);

    checkErrorCause(SecretKeySelector.class,
        "spec.replicateFrom.instance.external.secretKeyRefs.authenticator.username.name",
        "isNameNotEmpty", review, AssertTrue.class);
  }

  @Test
  void nullAuthenticatorUsernameSecretKey_shouldFail() {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getReplicateFrom().getInstance()
        .getExternal().getSecretKeyRefs().getAuthenticator().getUsername().setKey(null);

    checkErrorCause(SecretKeySelector.class,
        "spec.replicateFrom.instance.external.secretKeyRefs.authenticator.username.key",
        "isKeyNotEmpty", review, AssertTrue.class);
  }

  @Test
  void nullAuthenticatorPasswordSecretName_shouldFail() {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getReplicateFrom().getInstance()
        .getExternal().getSecretKeyRefs().getAuthenticator().getPassword().setName(null);

    checkErrorCause(SecretKeySelector.class,
        "spec.replicateFrom.instance.external.secretKeyRefs.authenticator.password.name",
        "isNameNotEmpty", review, AssertTrue.class);
  }

  @Test
  void nullAuthenticatorPasswordSecretKey_shouldFail() {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getReplicateFrom().getInstance()
        .getExternal().getSecretKeyRefs().getAuthenticator().getPassword().setKey(null);

    checkErrorCause(SecretKeySelector.class,
        "spec.replicateFrom.instance.external.secretKeyRefs.authenticator.password.key",
        "isKeyNotEmpty", review, AssertTrue.class);
  }

}
