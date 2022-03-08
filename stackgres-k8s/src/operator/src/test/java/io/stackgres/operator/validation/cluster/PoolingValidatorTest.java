/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.AbstractCustomResourceFinder;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
class PoolingValidatorTest {

  private PoolingConfigValidator validator;

  @Mock()
  private AbstractCustomResourceFinder<StackGresPoolingConfig> configFinder;

  private StackGresPoolingConfig pgbouncerConfig;

  @BeforeEach
  void setUp() {
    validator = new PoolingConfigValidator(configFinder);

    pgbouncerConfig = Fixtures.poolingConfig().loadDefault().get();

  }

  @Test
  void givenValidStackGresPoolingOnCreation_shouldNotFail() throws ValidationFailed {

    final StackGresClusterReview review = AdmissionReviewFixtures.cluster().loadCreate().get();

    String poolingConfig =
        review.getRequest().getObject().getSpec().getConfiguration().getConnectionPoolingConfig();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();
    when(configFinder.findByNameAndNamespace(poolingConfig, namespace))
        .thenReturn(Optional.of(pgbouncerConfig));

    validator.validate(review);

    verify(configFinder).findByNameAndNamespace(eq(poolingConfig), eq(namespace));

  }

  @Test
  void giveInvalidStackGresPoolingOnCreation_shouldFail() {

    final StackGresClusterReview review = AdmissionReviewFixtures.cluster().loadCreate().get();

    String poolingConfig =
        review.getRequest().getObject().getSpec().getConfiguration().getConnectionPoolingConfig();
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

    final StackGresClusterReview review = AdmissionReviewFixtures.cluster()
        .loadConnectionPoolingConfigUpdate().get();

    String poolingConfig =
        review.getRequest().getObject().getSpec().getConfiguration().getConnectionPoolingConfig();

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

    final StackGresClusterReview review = AdmissionReviewFixtures.cluster()
        .loadConnectionPoolingConfigUpdate().get();

    String poolingConfig =
        review.getRequest().getObject().getSpec().getConfiguration().getConnectionPoolingConfig();

    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(configFinder.findByNameAndNamespace(poolingConfig, namespace))
        .thenReturn(Optional.of(pgbouncerConfig));

    validator.validate(review);

    verify(configFinder).findByNameAndNamespace(eq(poolingConfig), eq(namespace));

  }

  @Test
  void giveAnAttemptToDelete_shouldNotFail() throws ValidationFailed {

    final StackGresClusterReview review = AdmissionReviewFixtures.cluster()
        .loadConnectionPoolingConfigUpdate().get();
    review.getRequest().setOperation(Operation.DELETE);

    String poolingConfig =
        review.getRequest().getObject().getSpec().getConfiguration().getConnectionPoolingConfig();

    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    validator.validate(review);

    verify(configFinder, never()).findByNameAndNamespace(eq(poolingConfig), eq(namespace));

  }

}
