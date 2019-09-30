/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import java.util.Optional;

import io.stackgres.operator.services.PgBouncerConfigFinder;
import io.stackgres.operator.utils.JsonUtil;
import io.stackgres.operator.validation.AdmissionReview;
import io.stackgres.operator.validation.Operation;
import io.stackgres.operator.validation.ValidationFailed;
import io.stackgres.sidecars.pgbouncer.customresources.StackGresPgbouncerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;


class PgBouncerValidatorTest {

  private PgBouncerValidator validator;

  private PgBouncerConfigFinder configFinder;

  private StackGresPgbouncerConfig pgbouncerConfig;

  @BeforeEach
  void setUp(){
    configFinder = mock(PgBouncerConfigFinder.class);

    validator = new PgBouncerValidator(configFinder);

    pgbouncerConfig = JsonUtil.readFromJson("pgbouncer_config/default.json", StackGresPgbouncerConfig.class);

  }

  @Test
  void givenValidStackgresPoolingOnCreation_shouldNotFail() throws ValidationFailed {

    final AdmissionReview review = JsonUtil
        .readFromJson("allowed_requests/valid_creation.json", AdmissionReview.class);

    String poolingConfig = review.getRequest().getObject().getSpec().getConnectionPoolingConfig();
    when(configFinder.findPgBouncerConfig(poolingConfig))
        .thenReturn(Optional.of(pgbouncerConfig));

    validator.validate(review);

    verify(configFinder).findPgBouncerConfig(eq(poolingConfig));

  }

  @Test
  void giveInvalidStackgresPoolingOnCreation_shouldFail() {

    final AdmissionReview review = JsonUtil
        .readFromJson("allowed_requests/valid_creation.json", AdmissionReview.class);

    String poolingConfig = review.getRequest().getObject().getSpec().getConnectionPoolingConfig();

    when(configFinder.findPgBouncerConfig(poolingConfig))
        .thenReturn(Optional.empty());

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = ex.getMessage();

    assertEquals("Pooling config " + poolingConfig + " not found", resultMessage);

  }

  @Test
  void giveAnAttemptToUpdateToAnUnknownPoolingConfig_shouldFail() {

    final AdmissionReview review = JsonUtil
        .readFromJson("allowed_requests/connectionpooling_config_update.json", AdmissionReview.class);

    String poolingConfig = review.getRequest().getObject().getSpec().getConnectionPoolingConfig();

    when(configFinder.findPgBouncerConfig(poolingConfig))
        .thenReturn(Optional.empty());

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = ex.getMessage();

    assertEquals("Cannot update to pooling config " + poolingConfig
        + " because it doesn't exists", resultMessage);

    verify(configFinder).findPgBouncerConfig(eq(poolingConfig));

  }

  @Test
  void giveAnAttemptToUpdateToAnKnownPooling_shouldNotFail() throws ValidationFailed {

    final AdmissionReview review = JsonUtil
        .readFromJson("allowed_requests/connectionpooling_config_update.json", AdmissionReview.class);

    String poolingConfig = review.getRequest().getObject().getSpec().getConnectionPoolingConfig();


    when(configFinder.findPgBouncerConfig(poolingConfig))
        .thenReturn(Optional.of(pgbouncerConfig));

    validator.validate(review);

    verify(configFinder).findPgBouncerConfig(eq(poolingConfig));

  }

  @Test
  void giveAnAttemptToDelete_shouldNotFail() throws ValidationFailed {

    final AdmissionReview review = JsonUtil
        .readFromJson("allowed_requests/connectionpooling_config_update.json", AdmissionReview.class);
    review.getRequest().setOperation(Operation.DELETE);

    String poolingConfig = review.getRequest().getObject().getSpec().getConnectionPoolingConfig();

    when(configFinder.findPgBouncerConfig(poolingConfig))
        .thenReturn(Optional.of(pgbouncerConfig));

    validator.validate(review);

    verify(configFinder, never()).findPgBouncerConfig(eq(poolingConfig));

  }

}
