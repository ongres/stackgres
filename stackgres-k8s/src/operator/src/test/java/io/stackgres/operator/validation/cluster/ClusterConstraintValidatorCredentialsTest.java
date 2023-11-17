/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import io.stackgres.common.crd.SecretKeySelector;
import io.stackgres.common.crd.sgcluster.StackGresClusterCredentials;
import io.stackgres.common.crd.sgcluster.StackGresClusterPatroniCredentials;
import io.stackgres.common.crd.sgcluster.StackGresClusterUserSecretKeyRef;
import io.stackgres.common.crd.sgcluster.StackGresClusterUsersCredentials;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.validation.AbstractConstraintValidator;
import io.stackgres.operator.validation.ConstraintValidationTest;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import jakarta.validation.constraints.AssertTrue;
import org.junit.jupiter.api.Test;

class ClusterConstraintValidatorCredentialsTest
    extends ConstraintValidationTest<StackGresClusterReview> {

  @Override
  protected AbstractConstraintValidator<StackGresClusterReview> buildValidator() {
    return new ClusterConstraintValidator();
  }

  @Override
  protected StackGresClusterReview getValidReview() {
    var review = AdmissionReviewFixtures.cluster().loadCreate().get();
    review.getRequest().getObject().getSpec().getConfigurations()
        .setCredentials(new StackGresClusterCredentials());
    review.getRequest().getObject().getSpec().getConfigurations()
        .getCredentials().setUsers(new StackGresClusterUsersCredentials());
    review.getRequest().getObject().getSpec().getConfigurations()
        .getCredentials().getUsers()
        .setSuperuser(new StackGresClusterUserSecretKeyRef());
    review.getRequest().getObject().getSpec().getConfigurations()
        .getCredentials().getUsers().getSuperuser()
        .setUsername(new SecretKeySelector("test", "test"));
    review.getRequest().getObject().getSpec().getConfigurations()
        .getCredentials().getUsers().getSuperuser()
        .setPassword(new SecretKeySelector("test", "test"));
    review.getRequest().getObject().getSpec().getConfigurations()
        .getCredentials().getUsers()
        .setReplication(new StackGresClusterUserSecretKeyRef());
    review.getRequest().getObject().getSpec().getConfigurations()
        .getCredentials().getUsers().getReplication()
        .setUsername(new SecretKeySelector("test", "test"));
    review.getRequest().getObject().getSpec().getConfigurations()
        .getCredentials().getUsers().getReplication()
        .setPassword(new SecretKeySelector("test", "test"));
    review.getRequest().getObject().getSpec().getConfigurations()
        .getCredentials().getUsers()
        .setAuthenticator(new StackGresClusterUserSecretKeyRef());
    review.getRequest().getObject().getSpec().getConfigurations()
        .getCredentials().getUsers().getAuthenticator()
        .setUsername(new SecretKeySelector("test", "test"));
    review.getRequest().getObject().getSpec().getConfigurations()
        .getCredentials().getUsers().getAuthenticator()
        .setPassword(new SecretKeySelector("test", "test"));
    review.getRequest().getObject().getSpec().getConfigurations()
        .getCredentials().setPatroni(new StackGresClusterPatroniCredentials());
    review.getRequest().getObject().getSpec().getConfigurations()
        .getCredentials().getPatroni()
        .setRestApiPassword(new SecretKeySelector("test", "test"));
    return review;
  }

  @Override
  protected StackGresClusterReview getInvalidReview() {
    final StackGresClusterReview review = AdmissionReviewFixtures.cluster().loadCreate().get();

    review.getRequest().getObject().setSpec(null);

    return review;
  }

  @Test
  void nullUsers_shouldPass() throws ValidationFailed {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getConfigurations()
        .getCredentials().setUsers(null);

    validator.validate(review);
  }

  @Test
  void nullpatroni_shouldPass() throws ValidationFailed {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getConfigurations()
        .getCredentials().setPatroni(null);

    validator.validate(review);
  }

  @Test
  void nullSuperuser_shouldPass() throws ValidationFailed {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getConfigurations()
        .getCredentials().getUsers().setSuperuser(null);

    validator.validate(review);
  }

  @Test
  void nullSuperuserUsername_shouldPass() throws ValidationFailed {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getConfigurations()
        .getCredentials().getUsers().getSuperuser().setUsername(null);

    validator.validate(review);
  }

  @Test
  void nullSuperuserPassword_shouldPass() throws ValidationFailed {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getConfigurations()
        .getCredentials().getUsers().getSuperuser().setPassword(null);

    validator.validate(review);
  }

  @Test
  void nullSuperuserUsernameSecretName_shouldFail() {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getConfigurations()
        .getCredentials().getUsers().getSuperuser().getUsername().setName(null);

    checkErrorCause(SecretKeySelector.class,
        "spec.configurations.credentials.users.superuser.username.name",
        "isNameNotEmpty", review, AssertTrue.class);
  }

  @Test
  void nullSuperuserUsernameSecretKey_shouldFail() {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getConfigurations()
        .getCredentials().getUsers().getSuperuser().getUsername().setKey(null);

    checkErrorCause(SecretKeySelector.class,
        "spec.configurations.credentials.users.superuser.username.key",
        "isKeyNotEmpty", review, AssertTrue.class);
  }

  @Test
  void nullSuperuserPasswordSecretName_shouldFail() {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getConfigurations()
        .getCredentials().getUsers().getSuperuser().getPassword().setName(null);

    checkErrorCause(SecretKeySelector.class,
        "spec.configurations.credentials.users.superuser.password.name",
        "isNameNotEmpty", review, AssertTrue.class);
  }

  @Test
  void nullSuperuserPasswordSecretKey_shouldFail() {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getConfigurations()
        .getCredentials().getUsers().getSuperuser().getPassword().setKey(null);

    checkErrorCause(SecretKeySelector.class,
        "spec.configurations.credentials.users.superuser.password.key",
        "isKeyNotEmpty", review, AssertTrue.class);
  }

  @Test
  void nullReplication_shouldPass() throws ValidationFailed {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getConfigurations()
        .getCredentials().getUsers().setReplication(null);

    validator.validate(review);
  }

  @Test
  void nullReplicationUsername_shouldPass() throws ValidationFailed {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getConfigurations()
        .getCredentials().getUsers().getReplication().setUsername(null);

    validator.validate(review);
  }

  @Test
  void nullReplicationPassword_shouldPass() throws ValidationFailed {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getConfigurations()
        .getCredentials().getUsers().getReplication().setPassword(null);

    validator.validate(review);
  }

  @Test
  void nullReplicationUsernameSecretName_shouldFail() {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getConfigurations()
        .getCredentials().getUsers().getReplication().getUsername().setName(null);

    checkErrorCause(SecretKeySelector.class,
        "spec.configurations.credentials.users.replication.username.name",
        "isNameNotEmpty", review, AssertTrue.class);
  }

  @Test
  void nullReplicationUsernameSecretKey_shouldFail() {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getConfigurations()
        .getCredentials().getUsers().getReplication().getUsername().setKey(null);

    checkErrorCause(SecretKeySelector.class,
        "spec.configurations.credentials.users.replication.username.key",
        "isKeyNotEmpty", review, AssertTrue.class);
  }

  @Test
  void nullReplicationPasswordSecretName_shouldFail() {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getConfigurations()
        .getCredentials().getUsers().getReplication().getPassword().setName(null);

    checkErrorCause(SecretKeySelector.class,
        "spec.configurations.credentials.users.replication.password.name",
        "isNameNotEmpty", review, AssertTrue.class);
  }

  @Test
  void nullReplicationPasswordSecretKey_shouldFail() {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getConfigurations()
        .getCredentials().getUsers().getReplication().getPassword().setKey(null);

    checkErrorCause(SecretKeySelector.class,
        "spec.configurations.credentials.users.replication.password.key",
        "isKeyNotEmpty", review, AssertTrue.class);
  }

  @Test
  void nullAuthenticator_shouldPass() throws ValidationFailed {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getConfigurations()
        .getCredentials().getUsers().setAuthenticator(null);

    validator.validate(review);
  }

  @Test
  void nullAuthenticatorUsername_shouldPass() throws ValidationFailed {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getConfigurations()
        .getCredentials().getUsers().getAuthenticator().setUsername(null);

    validator.validate(review);
  }

  @Test
  void nullAuthenticatorPassword_shouldPass() throws ValidationFailed {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getConfigurations()
        .getCredentials().getUsers().getAuthenticator().setPassword(null);

    validator.validate(review);
  }

  @Test
  void nullAuthenticatorUsernameSecretName_shouldFail() {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getConfigurations()
        .getCredentials().getUsers().getAuthenticator().getUsername().setName(null);

    checkErrorCause(SecretKeySelector.class,
        "spec.configurations.credentials.users.authenticator.username.name",
        "isNameNotEmpty", review, AssertTrue.class);
  }

  @Test
  void nullAuthenticatorUsernameSecretKey_shouldFail() {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getConfigurations()
        .getCredentials().getUsers().getAuthenticator().getUsername().setKey(null);

    checkErrorCause(SecretKeySelector.class,
        "spec.configurations.credentials.users.authenticator.username.key",
        "isKeyNotEmpty", review, AssertTrue.class);
  }

  @Test
  void nullAuthenticatorPasswordSecretName_shouldFail() {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getConfigurations()
        .getCredentials().getUsers().getAuthenticator().getPassword().setName(null);

    checkErrorCause(SecretKeySelector.class,
        "spec.configurations.credentials.users.authenticator.password.name",
        "isNameNotEmpty", review, AssertTrue.class);
  }

  @Test
  void nullAuthenticatorPasswordSecretKey_shouldFail() {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getConfigurations()
        .getCredentials().getUsers().getAuthenticator().getPassword().setKey(null);

    checkErrorCause(SecretKeySelector.class,
        "spec.configurations.credentials.users.authenticator.password.key",
        "isKeyNotEmpty", review, AssertTrue.class);
  }

  @Test
  void nullPatroniRestApiPassword_shouldPass() throws ValidationFailed {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getConfigurations()
        .getCredentials().getPatroni().setRestApiPassword(null);

    validator.validate(review);
  }

  @Test
  void nullPatroniRestApiPasswordSecretName_shouldFail() {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getConfigurations()
        .getCredentials().getPatroni().getRestApiPassword().setName(null);

    checkErrorCause(SecretKeySelector.class,
        "spec.configurations.credentials.patroni.restApiPassword.name",
        "isNameNotEmpty", review, AssertTrue.class);
  }

  @Test
  void nullPatroniRestApiPasswordSecretKey_shouldFail() {
    StackGresClusterReview review = getValidReview();

    review.getRequest().getObject().getSpec().getConfigurations()
        .getCredentials().getPatroni().getRestApiPassword().setKey(null);

    checkErrorCause(SecretKeySelector.class,
        "spec.configurations.credentials.patroni.restApiPassword.key",
        "isKeyNotEmpty", review, AssertTrue.class);
  }

}
