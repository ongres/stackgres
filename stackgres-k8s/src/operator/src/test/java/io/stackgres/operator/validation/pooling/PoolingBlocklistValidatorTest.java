/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pooling;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;

import io.stackgres.operator.common.StackGresPoolingConfigReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import org.junit.jupiter.api.Test;

class PoolingBlocklistValidatorTest {

  private PgBouncerBlocklistValidator validator = new PgBouncerBlocklistValidator();

  private static final StackGresPoolingConfigReview getCreatePoolingReview() {
    return AdmissionReviewFixtures.poolingConfig().loadCreate().get();
  }

  @Test
  void givenValidConfigurationCreation_shouldNotFail() {
    assertDoesNotThrow(() -> validator.validate(getCreatePoolingReview()));
  }

  @Test
  void givenValidConfigurationWithDatabases_shouldNotFail() {
    StackGresPoolingConfigReview review = getCreatePoolingReview();

    var pgBouncer = review.getRequest().getObject().getSpec()
        .getPgBouncer();
    pgBouncer.getPgbouncerIni().setDatabases(new HashMap<>());
    pgBouncer.getPgbouncerIni().getDatabases().put("foodb", Map.of("dbname", "bardb"));

    assertDoesNotThrow(() -> validator.validate(review));
  }

  @Test
  void givenValidConfigurationWithUsers_shouldNotFail() {
    StackGresPoolingConfigReview review = getCreatePoolingReview();

    var pgBouncer = review.getRequest().getObject().getSpec()
        .getPgBouncer();
    pgBouncer.getPgbouncerIni().setUsers(new HashMap<>());
    pgBouncer.getPgbouncerIni().getUsers().put("user1", Map.of("max_user_connections", "100"));

    assertDoesNotThrow(() -> validator.validate(review));
  }

  @Test
  void givenConfigurationWithDatabasesBlockedParameters_shouldFail() {
    StackGresPoolingConfigReview review = getCreatePoolingReview();

    var pgBouncer = review.getRequest().getObject().getSpec()
        .getPgBouncer();
    pgBouncer.getPgbouncerIni().setDatabases(new HashMap<>());
    pgBouncer.getPgbouncerIni().getDatabases()
        .put("foodb", Map.of("user", "user1", "host", "example.com"));

    ValidationFailed assertThrows =
        assertThrows(ValidationFailed.class, () -> validator.validate(review));
    assertEquals("Invalid PgBouncer configuration, properties: [host, user] cannot be set",
        assertThrows.getMessage());
  }

}
