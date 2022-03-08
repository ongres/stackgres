/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.script;

import java.util.ArrayList;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;

import io.stackgres.common.crd.ConfigMapKeySelector;
import io.stackgres.common.crd.SecretKeySelector;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.common.crd.sgscript.StackGresScriptEntry;
import io.stackgres.common.crd.sgscript.StackGresScriptFrom;
import io.stackgres.common.crd.sgscript.StackGresScriptTransactionIsolationLevel;
import io.stackgres.operator.common.StackGresScriptReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.validation.ConstraintValidationTest;
import io.stackgres.operator.validation.ConstraintValidator;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import org.junit.jupiter.api.Test;

class ScriptConstraintValidatorTest extends ConstraintValidationTest<StackGresScriptReview> {

  @Override
  protected ConstraintValidator<StackGresScriptReview> buildValidator() {
    return new ScriptConstraintValidator();
  }

  @Override
  protected StackGresScriptReview getValidReview() {
    return AdmissionReviewFixtures.script()
        .loadCreate().get();
  }

  @Override
  protected StackGresScriptReview getInvalidReview() {
    final StackGresScriptReview review = AdmissionReviewFixtures.script()
        .loadCreate().get();

    review.getRequest().getObject().setSpec(null);
    return review;
  }

  @Test
  void nullSpec_shouldFail() {
    StackGresScriptReview review = getValidReview();
    review.getRequest().getObject().setSpec(null);

    checkNotNullErrorCause(StackGresScript.class, "spec", review);
  }

  @Test
  void nullId_shouldFail() {
    StackGresScriptReview review = getValidReview();
    review.getRequest().getObject().getSpec().setScripts(new ArrayList<>());
    review.getRequest().getObject().getSpec().getScripts()
        .add(new StackGresScriptEntry());
    review.getRequest().getObject().getSpec().getScripts().get(0)
        .setVersion(0);
    review.getRequest().getObject().getSpec().getScripts().get(0)
        .setScript("SELECT 1");

    checkErrorCause(StackGresScriptEntry.class,
        "spec.scripts[0].id",
        review, NotNull.class);
  }

  @Test
  void nullVersion_shouldFail() {
    StackGresScriptReview review = getValidReview();
    review.getRequest().getObject().getSpec().setScripts(new ArrayList<>());
    review.getRequest().getObject().getSpec().getScripts()
        .add(new StackGresScriptEntry());
    review.getRequest().getObject().getSpec().getScripts().get(0)
        .setId(0);
    review.getRequest().getObject().getSpec().getScripts().get(0)
        .setScript("SELECT 1");

    checkErrorCause(StackGresScriptEntry.class,
        "spec.scripts[0].version",
        review, NotNull.class);
  }

  @Test
  void nullStatus_shouldFail() {
    StackGresScriptReview review = getValidReview();
    review.getRequest().getObject().getSpec().setScripts(new ArrayList<>());
    review.getRequest().getObject().getSpec().getScripts()
        .add(new StackGresScriptEntry());
    review.getRequest().getObject().getSpec().getScripts().get(0)
        .setId(0);
    review.getRequest().getObject().getSpec().getScripts().get(0)
        .setVersion(0);
    review.getRequest().getObject().getSpec().getScripts().get(0)
        .setScript("SELECT 1");
    review.getRequest().getObject().setStatus(null);

    checkErrorCause(StackGresScript.class,
        "status",
        review, NotNull.class);
  }

  @Test
  void validScript_shouldPass() throws ValidationFailed {
    StackGresScriptReview review = getValidReview();
    review.getRequest().getObject().getSpec().setScripts(new ArrayList<>());
    review.getRequest().getObject().getSpec().getScripts()
        .add(new StackGresScriptEntry());
    review.getRequest().getObject().getSpec().getScripts().get(0)
        .setId(0);
    review.getRequest().getObject().getSpec().getScripts().get(0)
        .setVersion(0);
    review.getRequest().getObject().getSpec().getScripts().get(0)
        .setScript("SELECT 1");

    validator.validate(review);
  }

  @Test
  void missingScript_shouldFail() {
    StackGresScriptReview review = getValidReview();
    review.getRequest().getObject().getSpec().setScripts(new ArrayList<>());
    review.getRequest().getObject().getSpec().getScripts()
        .add(new StackGresScriptEntry());
    review.getRequest().getObject().getSpec().getScripts().get(0)
        .setId(0);
    review.getRequest().getObject().getSpec().getScripts().get(0)
        .setVersion(0);

    checkErrorCause(StackGresScriptEntry.class,
        new String[] {"spec.scripts[0].script",
            "spec.scripts[0].scriptFrom"},
        "isScriptMutuallyExclusiveAndRequired", review, AssertTrue.class);
  }

  @Test
  void validScriptAndScriptFrom_shouldFail() {
    StackGresScriptReview review = getValidReview();
    review.getRequest().getObject().getSpec().setScripts(new ArrayList<>());
    review.getRequest().getObject().getSpec().getScripts()
        .add(new StackGresScriptEntry());
    review.getRequest().getObject().getSpec().getScripts().get(0)
        .setId(0);
    review.getRequest().getObject().getSpec().getScripts().get(0)
        .setVersion(0);
    review.getRequest().getObject().getSpec().getScripts().get(0)
        .setScriptFrom(new StackGresScriptFrom());
    review.getRequest().getObject().getSpec().getScripts().get(0)
        .setScript("SELECT 1");
    review.getRequest().getObject().getSpec().getScripts().get(0).getScriptFrom()
        .setConfigMapKeyRef(new ConfigMapKeySelector());
    review.getRequest().getObject().getSpec().getScripts().get(0).getScriptFrom()
        .getConfigMapKeyRef().setName("test");
    review.getRequest().getObject().getSpec().getScripts().get(0).getScriptFrom()
        .getConfigMapKeyRef().setKey("test");

    checkErrorCause(StackGresScriptEntry.class,
        new String[] {"spec.scripts[0].script",
            "spec.scripts[0].scriptFrom"},
        "isScriptMutuallyExclusiveAndRequired", review, AssertTrue.class);
  }

  @Test
  void scriptWithEmptyDatabaseName_shouldFail() throws ValidationFailed {
    StackGresScriptReview review = getValidReview();
    review.getRequest().getObject().getSpec().setScripts(new ArrayList<>());
    review.getRequest().getObject().getSpec().getScripts()
        .add(new StackGresScriptEntry());
    review.getRequest().getObject().getSpec().getScripts().get(0)
        .setId(0);
    review.getRequest().getObject().getSpec().getScripts().get(0)
        .setVersion(0);
    review.getRequest().getObject().getSpec().getScripts().get(0).setDatabase("");
    review.getRequest().getObject().getSpec().getScripts().get(0)
        .setScript("SELECT 1");

    checkErrorCause(StackGresScriptEntry.class,
        new String[] {"spec.scripts[0].database"},
        "isDatabaseNameNonEmpty", review, AssertTrue.class);
  }

  @Test
  void validScriptFromConfigMap_shouldPass() throws ValidationFailed {
    StackGresScriptReview review = getValidReview();
    review.getRequest().getObject().getSpec().setScripts(new ArrayList<>());
    review.getRequest().getObject().getSpec().getScripts()
        .add(new StackGresScriptEntry());
    review.getRequest().getObject().getSpec().getScripts().get(0)
        .setId(0);
    review.getRequest().getObject().getSpec().getScripts().get(0)
        .setVersion(0);
    review.getRequest().getObject().getSpec().getScripts().get(0)
        .setScriptFrom(new StackGresScriptFrom());
    review.getRequest().getObject().getSpec().getScripts().get(0).getScriptFrom()
        .setConfigMapKeyRef(new ConfigMapKeySelector());
    review.getRequest().getObject().getSpec().getScripts().get(0).getScriptFrom()
        .getConfigMapKeyRef().setName("test");
    review.getRequest().getObject().getSpec().getScripts().get(0).getScriptFrom()
        .getConfigMapKeyRef().setKey("test");

    validator.validate(review);
  }

  @Test
  void validScriptFromSecret_shouldPass() throws ValidationFailed {
    StackGresScriptReview review = getValidReview();
    review.getRequest().getObject().getSpec().setScripts(new ArrayList<>());
    review.getRequest().getObject().getSpec().getScripts()
        .add(new StackGresScriptEntry());
    review.getRequest().getObject().getSpec().getScripts().get(0)
        .setId(0);
    review.getRequest().getObject().getSpec().getScripts().get(0)
        .setVersion(0);
    review.getRequest().getObject().getSpec().getScripts().get(0)
        .setScriptFrom(new StackGresScriptFrom());
    review.getRequest().getObject().getSpec().getScripts().get(0).getScriptFrom()
        .setSecretKeyRef(new SecretKeySelector());
    review.getRequest().getObject().getSpec().getScripts().get(0).getScriptFrom()
        .getSecretKeyRef().setName("test");
    review.getRequest().getObject().getSpec().getScripts().get(0).getScriptFrom()
        .getSecretKeyRef().setKey("test");

    validator.validate(review);
  }

  @Test
  void missingScriptFrom_shouldFail() {
    StackGresScriptReview review = getValidReview();
    review.getRequest().getObject().getSpec().setScripts(new ArrayList<>());
    review.getRequest().getObject().getSpec().getScripts()
        .add(new StackGresScriptEntry());
    review.getRequest().getObject().getSpec().getScripts().get(0)
        .setId(0);
    review.getRequest().getObject().getSpec().getScripts().get(0)
        .setVersion(0);
    review.getRequest().getObject().getSpec().getScripts().get(0)
        .setScriptFrom(new StackGresScriptFrom());

    checkErrorCause(StackGresScriptFrom.class,
        new String[] {"spec.scripts[0].scriptFrom.secretKeyRef",
            "spec.scripts[0].scriptFrom.configMapKeyRef"},
        "isSecretKeySelectorAndConfigMapKeySelectorMutuallyExclusiveAndRequired",
        review, AssertTrue.class);
  }

  @Test
  void validScriptFromConfigMapAndSecret_shouldFail() {
    StackGresScriptReview review = getValidReview();
    review.getRequest().getObject().getSpec().setScripts(new ArrayList<>());
    review.getRequest().getObject().getSpec().getScripts()
        .add(new StackGresScriptEntry());
    review.getRequest().getObject().getSpec().getScripts().get(0)
        .setId(0);
    review.getRequest().getObject().getSpec().getScripts().get(0)
        .setVersion(0);
    review.getRequest().getObject().getSpec().getScripts().get(0)
        .setScriptFrom(new StackGresScriptFrom());
    review.getRequest().getObject().getSpec().getScripts().get(0).getScriptFrom()
        .setConfigMapKeyRef(new ConfigMapKeySelector());
    review.getRequest().getObject().getSpec().getScripts().get(0).getScriptFrom()
        .getConfigMapKeyRef().setName("test");
    review.getRequest().getObject().getSpec().getScripts().get(0).getScriptFrom()
        .getConfigMapKeyRef().setKey("test");
    review.getRequest().getObject().getSpec().getScripts().get(0).getScriptFrom()
        .setSecretKeyRef(new SecretKeySelector());
    review.getRequest().getObject().getSpec().getScripts().get(0).getScriptFrom()
        .getSecretKeyRef().setName("test");
    review.getRequest().getObject().getSpec().getScripts().get(0).getScriptFrom()
        .getSecretKeyRef().setKey("test");

    checkErrorCause(StackGresScriptFrom.class,
        new String[] {"spec.scripts[0].scriptFrom.secretKeyRef",
            "spec.scripts[0].scriptFrom.configMapKeyRef"},
        "isSecretKeySelectorAndConfigMapKeySelectorMutuallyExclusiveAndRequired",
        review, AssertTrue.class);
  }

  @Test
  void scriptFromConfigMapWithEmptyKey_shouldFail() {
    StackGresScriptReview review = getValidReview();
    review.getRequest().getObject().getSpec().setScripts(new ArrayList<>());
    review.getRequest().getObject().getSpec().getScripts()
        .add(new StackGresScriptEntry());
    review.getRequest().getObject().getSpec().getScripts().get(0)
        .setId(0);
    review.getRequest().getObject().getSpec().getScripts().get(0)
        .setVersion(0);
    review.getRequest().getObject().getSpec().getScripts().get(0)
        .setScriptFrom(new StackGresScriptFrom());
    review.getRequest().getObject().getSpec().getScripts().get(0).getScriptFrom()
        .setConfigMapKeyRef(new ConfigMapKeySelector());
    review.getRequest().getObject().getSpec().getScripts().get(0).getScriptFrom()
        .getConfigMapKeyRef().setName("test");
    review.getRequest().getObject().getSpec().getScripts().get(0).getScriptFrom()
        .getConfigMapKeyRef().setKey("");

    checkErrorCause(SecretKeySelector.class,
        "spec.scripts[0].scriptFrom.configMapKeyRef.key",
        "isKeyNotEmpty", review, AssertTrue.class);
  }

  @Test
  void scriptFromConfigMapWithEmptyName_shouldFail() {
    StackGresScriptReview review = getValidReview();
    review.getRequest().getObject().getSpec().setScripts(new ArrayList<>());
    review.getRequest().getObject().getSpec().getScripts()
        .add(new StackGresScriptEntry());
    review.getRequest().getObject().getSpec().getScripts().get(0)
        .setId(0);
    review.getRequest().getObject().getSpec().getScripts().get(0)
        .setVersion(0);
    review.getRequest().getObject().getSpec().getScripts().get(0)
        .setScriptFrom(new StackGresScriptFrom());
    review.getRequest().getObject().getSpec().getScripts().get(0).getScriptFrom()
        .setConfigMapKeyRef(new ConfigMapKeySelector());
    review.getRequest().getObject().getSpec().getScripts().get(0).getScriptFrom()
        .getConfigMapKeyRef().setName("");
    review.getRequest().getObject().getSpec().getScripts().get(0).getScriptFrom()
        .getConfigMapKeyRef().setKey("test");

    checkErrorCause(SecretKeySelector.class,
        "spec.scripts[0].scriptFrom.configMapKeyRef.name",
        "isNameNotEmpty", review, AssertTrue.class);
  }

  @Test
  void scriptFromSecretWithEmptyKey_shouldFail() {
    StackGresScriptReview review = getValidReview();
    review.getRequest().getObject().getSpec().setScripts(new ArrayList<>());
    review.getRequest().getObject().getSpec().getScripts()
        .add(new StackGresScriptEntry());
    review.getRequest().getObject().getSpec().getScripts().get(0)
        .setId(0);
    review.getRequest().getObject().getSpec().getScripts().get(0)
        .setVersion(0);
    review.getRequest().getObject().getSpec().getScripts().get(0)
        .setScriptFrom(new StackGresScriptFrom());
    review.getRequest().getObject().getSpec().getScripts().get(0).getScriptFrom()
        .setSecretKeyRef(new SecretKeySelector());
    review.getRequest().getObject().getSpec().getScripts().get(0).getScriptFrom()
        .getSecretKeyRef().setName("test");
    review.getRequest().getObject().getSpec().getScripts().get(0).getScriptFrom()
        .getSecretKeyRef().setKey("");

    checkErrorCause(SecretKeySelector.class, "spec.scripts[0].scriptFrom.secretKeyRef.key",
        "isKeyNotEmpty", review, AssertTrue.class);
  }

  @Test
  void scriptFromSecretWithEmptyName_shouldFail() {
    StackGresScriptReview review = getValidReview();
    review.getRequest().getObject().getSpec().setScripts(new ArrayList<>());
    review.getRequest().getObject().getSpec().getScripts()
        .add(new StackGresScriptEntry());
    review.getRequest().getObject().getSpec().getScripts().get(0)
        .setId(0);
    review.getRequest().getObject().getSpec().getScripts().get(0)
        .setVersion(0);
    review.getRequest().getObject().getSpec().getScripts().get(0)
        .setScriptFrom(new StackGresScriptFrom());
    review.getRequest().getObject().getSpec().getScripts().get(0).getScriptFrom()
        .setSecretKeyRef(new SecretKeySelector());
    review.getRequest().getObject().getSpec().getScripts().get(0).getScriptFrom()
        .getSecretKeyRef().setName("");
    review.getRequest().getObject().getSpec().getScripts().get(0).getScriptFrom()
        .getSecretKeyRef().setKey("test");

    checkErrorCause(SecretKeySelector.class,
        "spec.scripts[0].scriptFrom.secretKeyRef.name",
        "isNameNotEmpty", review, AssertTrue.class);
  }

  @Test
  void scriptWithStoreStatusInDatabaseAndNoWrapInTransaction_shouldFail() {
    StackGresScriptReview review = getValidReview();
    review.getRequest().getObject().getSpec().setScripts(new ArrayList<>());
    review.getRequest().getObject().getSpec().getScripts()
        .add(new StackGresScriptEntry());
    review.getRequest().getObject().getSpec().getScripts().get(0)
        .setId(0);
    review.getRequest().getObject().getSpec().getScripts().get(0)
        .setVersion(0);
    review.getRequest().getObject().getSpec().getScripts().get(0)
        .setScript("SELECT 1");
    review.getRequest().getObject().getSpec().getScripts().get(0)
        .setStoreStatusInDatabase(true);

    checkErrorCause(StackGresScriptEntry.class,
        new String[] {"spec.scripts[0].storeStatusInDatabase"},
        "isWrapInTransactionSetWhenStoreStatusInDatabaseIsSet", review, AssertTrue.class);
  }

  @Test
  void scriptWithStoreStatusInDatabaseAndNoWrapInTransaction_shouldPass() throws Exception {
    StackGresScriptReview review = getValidReview();
    review.getRequest().getObject().getSpec().setScripts(new ArrayList<>());
    review.getRequest().getObject().getSpec().getScripts()
        .add(new StackGresScriptEntry());
    review.getRequest().getObject().getSpec().getScripts().get(0)
        .setId(0);
    review.getRequest().getObject().getSpec().getScripts().get(0)
        .setVersion(0);
    review.getRequest().getObject().getSpec().getScripts().get(0)
        .setScript("SELECT 1");
    review.getRequest().getObject().getSpec().getScripts().get(0)
        .setWrapInTransaction(StackGresScriptTransactionIsolationLevel.READ_COMMITTED.toString());

    validator.validate(review);
  }

}
