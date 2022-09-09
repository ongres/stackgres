/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import javax.validation.constraints.AssertTrue;

import io.stackgres.common.crd.SecretKeySelector;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFrom;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFromExternal;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFromInstance;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFromUserSecretKeyRef;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFromUsers;
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
    review.getRequest().getObject().getSpec().getReplicateFrom()
        .setUsers(new StackGresClusterReplicateFromUsers());
    review.getRequest().getObject().getSpec().getReplicateFrom()
        .getUsers().setSuperuser(new StackGresClusterReplicateFromUserSecretKeyRef());
    review.getRequest().getObject().getSpec().getReplicateFrom()
        .getUsers().getSuperuser().setUsername(new SecretKeySelector("test", "test"));
    review.getRequest().getObject().getSpec().getReplicateFrom()
        .getUsers().getSuperuser().setPassword(new SecretKeySelector("test", "test"));
    review.getRequest().getObject().getSpec().getReplicateFrom()
        .getUsers().setReplication(new StackGresClusterReplicateFromUserSecretKeyRef());
    review.getRequest().getObject().getSpec().getReplicateFrom()
        .getUsers().getReplication().setUsername(new SecretKeySelector("test", "test"));
    review.getRequest().getObject().getSpec().getReplicateFrom()
        .getUsers().getReplication().setPassword(new SecretKeySelector("test", "test"));
    review.getRequest().getObject().getSpec().getReplicateFrom()
        .getUsers().setAuthenticator(
            new StackGresClusterReplicateFromUserSecretKeyRef());
    review.getRequest().getObject().getSpec().getReplicateFrom()
        .getUsers().getAuthenticator().setUsername(new SecretKeySelector("test", "test"));
    review.getRequest().getObject().getSpec().getReplicateFrom()
        .getUsers().getAuthenticator().setPassword(new SecretKeySelector("test", "test"));
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
  void nullUsers_shouldFail() {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getReplicateFrom()
        .setUsers(null);

    checkNotNullErrorCause(StackGresClusterReplicateFrom.class,
        "spec.replicateFrom.users", review);
  }

  @Test
  void nullSuperuser_shouldFail() {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getReplicateFrom()
        .getUsers().setSuperuser(null);

    checkNotNullErrorCause(StackGresClusterReplicateFromUsers.class,
        "spec.replicateFrom.users.superuser", review);
  }

  @Test
  void nullSuperuserUsername_shouldFail() {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getReplicateFrom()
        .getUsers().getSuperuser().setUsername(null);

    checkNotNullErrorCause(StackGresClusterReplicateFromUserSecretKeyRef.class,
        "spec.replicateFrom.users.superuser.username", review);
  }

  @Test
  void nullSuperuserPassword_shouldFail() {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getReplicateFrom()
        .getUsers().getSuperuser().setPassword(null);

    checkNotNullErrorCause(StackGresClusterReplicateFromUserSecretKeyRef.class,
        "spec.replicateFrom.users.superuser.password", review);
  }

  @Test
  void nullSuperuserUsernameSecretName_shouldFail() {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getReplicateFrom()
        .getUsers().getSuperuser().getUsername().setName(null);

    checkErrorCause(SecretKeySelector.class,
        "spec.replicateFrom.users.superuser.username.name",
        "isNameNotEmpty", review, AssertTrue.class);
  }

  @Test
  void nullSuperuserUsernameSecretKey_shouldFail() {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getReplicateFrom()
        .getUsers().getSuperuser().getUsername().setKey(null);

    checkErrorCause(SecretKeySelector.class,
        "spec.replicateFrom.users.superuser.username.key",
        "isKeyNotEmpty", review, AssertTrue.class);
  }

  @Test
  void nullSuperuserPasswordSecretName_shouldFail() {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getReplicateFrom()
        .getUsers().getSuperuser().getPassword().setName(null);

    checkErrorCause(SecretKeySelector.class,
        "spec.replicateFrom.users.superuser.password.name",
        "isNameNotEmpty", review, AssertTrue.class);
  }

  @Test
  void nullSuperuserPasswordSecretKey_shouldFail() {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getReplicateFrom()
        .getUsers().getSuperuser().getPassword().setKey(null);

    checkErrorCause(SecretKeySelector.class,
        "spec.replicateFrom.users.superuser.password.key",
        "isKeyNotEmpty", review, AssertTrue.class);
  }

  @Test
  void nullReplication_shouldFail() {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getReplicateFrom()
        .getUsers().setReplication(null);

    checkNotNullErrorCause(StackGresClusterReplicateFromUsers.class,
        "spec.replicateFrom.users.replication", review);
  }

  @Test
  void nullReplicationUsername_shouldFail() {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getReplicateFrom()
        .getUsers().getReplication().setUsername(null);

    checkNotNullErrorCause(StackGresClusterReplicateFromUserSecretKeyRef.class,
        "spec.replicateFrom.users.replication.username", review);
  }

  @Test
  void nullReplicationPassword_shouldFail() {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getReplicateFrom()
        .getUsers().getReplication().setPassword(null);

    checkNotNullErrorCause(StackGresClusterReplicateFromUserSecretKeyRef.class,
        "spec.replicateFrom.users.replication.password", review);
  }

  @Test
  void nullReplicationUsernameSecretName_shouldFail() {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getReplicateFrom()
        .getUsers().getReplication().getUsername().setName(null);

    checkErrorCause(SecretKeySelector.class,
        "spec.replicateFrom.users.replication.username.name",
        "isNameNotEmpty", review, AssertTrue.class);
  }

  @Test
  void nullReplicationUsernameSecretKey_shouldFail() {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getReplicateFrom()
        .getUsers().getReplication().getUsername().setKey(null);

    checkErrorCause(SecretKeySelector.class,
        "spec.replicateFrom.users.replication.username.key",
        "isKeyNotEmpty", review, AssertTrue.class);
  }

  @Test
  void nullReplicationPasswordSecretName_shouldFail() {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getReplicateFrom()
        .getUsers().getReplication().getPassword().setName(null);

    checkErrorCause(SecretKeySelector.class,
        "spec.replicateFrom.users.replication.password.name",
        "isNameNotEmpty", review, AssertTrue.class);
  }

  @Test
  void nullReplicationPasswordSecretKey_shouldFail() {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getReplicateFrom()
        .getUsers().getReplication().getPassword().setKey(null);

    checkErrorCause(SecretKeySelector.class,
        "spec.replicateFrom.users.replication.password.key",
        "isKeyNotEmpty", review, AssertTrue.class);
  }

  @Test
  void nullAuthenticator_shouldFail() {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getReplicateFrom()
        .getUsers().setAuthenticator(null);

    checkNotNullErrorCause(StackGresClusterReplicateFromUsers.class,
        "spec.replicateFrom.users.authenticator", review);
  }

  @Test
  void nullAuthenticatorUsername_shouldFail() {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getReplicateFrom()
        .getUsers().getAuthenticator().setUsername(null);

    checkNotNullErrorCause(StackGresClusterReplicateFromUserSecretKeyRef.class,
        "spec.replicateFrom.users.authenticator.username", review);
  }

  @Test
  void nullAuthenticatorPassword_shouldFail() {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getReplicateFrom()
        .getUsers().getAuthenticator().setPassword(null);

    checkNotNullErrorCause(StackGresClusterReplicateFromUserSecretKeyRef.class,
        "spec.replicateFrom.users.authenticator.password", review);
  }

  @Test
  void nullAuthenticatorUsernameSecretName_shouldFail() {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getReplicateFrom()
        .getUsers().getAuthenticator().getUsername().setName(null);

    checkErrorCause(SecretKeySelector.class,
        "spec.replicateFrom.users.authenticator.username.name",
        "isNameNotEmpty", review, AssertTrue.class);
  }

  @Test
  void nullAuthenticatorUsernameSecretKey_shouldFail() {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getReplicateFrom()
        .getUsers().getAuthenticator().getUsername().setKey(null);

    checkErrorCause(SecretKeySelector.class,
        "spec.replicateFrom.users.authenticator.username.key",
        "isKeyNotEmpty", review, AssertTrue.class);
  }

  @Test
  void nullAuthenticatorPasswordSecretName_shouldFail() {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getReplicateFrom()
        .getUsers().getAuthenticator().getPassword().setName(null);

    checkErrorCause(SecretKeySelector.class,
        "spec.replicateFrom.users.authenticator.password.name",
        "isNameNotEmpty", review, AssertTrue.class);
  }

  @Test
  void nullAuthenticatorPasswordSecretKey_shouldFail() {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getReplicateFrom()
        .getUsers().getAuthenticator().getPassword().setKey(null);

    checkErrorCause(SecretKeySelector.class,
        "spec.replicateFrom.users.authenticator.password.key",
        "isKeyNotEmpty", review, AssertTrue.class);
  }

}
