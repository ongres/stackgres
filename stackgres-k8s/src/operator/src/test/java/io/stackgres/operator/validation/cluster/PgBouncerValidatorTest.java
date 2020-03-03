/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import java.util.Optional;

import io.stackgres.operator.resource.AbstractCustomResourceFinder;
import io.stackgres.operator.sidecars.pgbouncer.customresources.StackGresPgbouncerConfig;
import io.stackgres.operator.utils.JsonUtil;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
class PgBouncerValidatorTest {

  private PoolingConfigValidator validator;

  @Mock()
  private AbstractCustomResourceFinder<StackGresPgbouncerConfig> configFinder;

  private StackGresPgbouncerConfig pgbouncerConfig;

  @BeforeEach
  void setUp(){
    validator = new PoolingConfigValidator(configFinder);

    pgbouncerConfig = JsonUtil.readFromJson("pgbouncer_config/default.json", StackGresPgbouncerConfig.class);

  }

  @Test
  void givenValidStackGresPoolingOnCreation_shouldNotFail() throws ValidationFailed {

    final StackGresClusterReview review = JsonUtil
        .readFromJson("cluster_allow_requests/valid_creation.json", StackGresClusterReview.class);

    String poolingConfig = review.getRequest().getObject().getSpec().getConnectionPoolingConfig();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();
    when(configFinder.findByNameAndNamespace(poolingConfig, namespace))
        .thenReturn(Optional.of(pgbouncerConfig));

    validator.validate(review);

    verify(configFinder).findByNameAndNamespace(eq(poolingConfig), eq(namespace));

  }

  @Test
  void giveInvalidStackGresPoolingOnCreation_shouldFail() {

    final StackGresClusterReview review = JsonUtil
        .readFromJson("cluster_allow_requests/valid_creation.json", StackGresClusterReview.class);

    String poolingConfig = review.getRequest().getObject().getSpec().getConnectionPoolingConfig();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(configFinder.findByNameAndNamespace(poolingConfig, namespace))
        .thenReturn(Optional.empty());

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = ex.getMessage();

    assertEquals("Pooling config " + poolingConfig + " not found", resultMessage);

  }

  @Test
  void giveAnAttemptToUpdateToAnUnknownPoolingConfig_shouldFail() {

    final StackGresClusterReview review = JsonUtil
        .readFromJson("cluster_allow_requests/connectionpooling_config_update.json", StackGresClusterReview.class);

    String poolingConfig = review.getRequest().getObject().getSpec().getConnectionPoolingConfig();

    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(configFinder.findByNameAndNamespace(poolingConfig, namespace))
        .thenReturn(Optional.empty());

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = ex.getMessage();

    assertEquals("Cannot update to pooling config " + poolingConfig
        + " because it doesn't exists", resultMessage);

    verify(configFinder).findByNameAndNamespace(eq(poolingConfig), eq(namespace));

  }

  @Test
  void giveAnAttemptToUpdateToAnKnownPooling_shouldNotFail() throws ValidationFailed {

    final StackGresClusterReview review = JsonUtil
        .readFromJson("cluster_allow_requests/connectionpooling_config_update.json", StackGresClusterReview.class);

    String poolingConfig = review.getRequest().getObject().getSpec().getConnectionPoolingConfig();

    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(configFinder.findByNameAndNamespace(poolingConfig, namespace))
        .thenReturn(Optional.of(pgbouncerConfig));

    validator.validate(review);

    verify(configFinder).findByNameAndNamespace(eq(poolingConfig), eq(namespace));

  }

  @Test
  void giveAnAttemptToDelete_shouldNotFail() throws ValidationFailed {

    final StackGresClusterReview review = JsonUtil
        .readFromJson("cluster_allow_requests/connectionpooling_config_update.json", StackGresClusterReview.class);
    review.getRequest().setOperation(Operation.DELETE);

    String poolingConfig = review.getRequest().getObject().getSpec().getConnectionPoolingConfig();

    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    validator.validate(review);

    verify(configFinder, never()).findByNameAndNamespace(eq(poolingConfig), eq(namespace));

  }

}
