/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardedcluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.AbstractCustomResourceFinder;
import io.stackgres.operator.common.StackGresShardedClusterReview;
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
    final StackGresShardedClusterReview review = AdmissionReviewFixtures.shardedCluster()
        .loadCreate().get();

    String coordinatorPoolingConfig =
        review.getRequest().getObject().getSpec().getCoordinator()
        .getConfiguration().getConnectionPoolingConfig();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();
    when(configFinder.findByNameAndNamespace(coordinatorPoolingConfig, namespace))
        .thenReturn(Optional.of(pgbouncerConfig));

    validator.validate(review);

    verify(configFinder, times(2)).findByNameAndNamespace(
        eq(coordinatorPoolingConfig), eq(namespace));
  }

  @Test
  void giveInvalidCoordinatorStackGresPoolingOnCreation_shouldFail() {
    final StackGresShardedClusterReview review = AdmissionReviewFixtures.shardedCluster()
        .loadCreate().get();

    String poolingConfig =
        review.getRequest().getObject().getSpec().getCoordinator()
        .getConfiguration().getConnectionPoolingConfig();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(configFinder.findByNameAndNamespace(poolingConfig, namespace))
        .thenReturn(Optional.empty());

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = ex.getMessage();

    assertEquals("Pooling config " + poolingConfig + " not found for coordinator", resultMessage);
  }

  @Test
  void giveInvalidShardsStackGresPoolingOnCreation_shouldFail() {
    final StackGresShardedClusterReview review = AdmissionReviewFixtures.shardedCluster()
        .loadCreate().get();

    review.getRequest().getObject().getSpec().getShards()
        .getConfiguration().setConnectionPoolingConfig("test");
    String poolingConfig =
        review.getRequest().getObject().getSpec().getShards()
        .getConfiguration().getConnectionPoolingConfig();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(configFinder.findByNameAndNamespace(review.getRequest().getObject().getSpec()
        .getCoordinator().getConfiguration().getConnectionPoolingConfig(), namespace))
        .thenReturn(Optional.of(pgbouncerConfig));
    when(configFinder.findByNameAndNamespace(poolingConfig, namespace))
        .thenReturn(Optional.empty());

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = ex.getMessage();

    assertEquals("Pooling config " + poolingConfig + " not found for shards", resultMessage);
  }

  @Test
  void giveAnAttemptToUpdateToAnUnknownCoordinatorPoolingConfig_shouldFail() {
    final StackGresShardedClusterReview review = AdmissionReviewFixtures.shardedCluster()
        .loadConnectionPoolingConfigUpdate().get();

    String poolingConfig =
        review.getRequest().getObject().getSpec().getCoordinator()
        .getConfiguration().getConnectionPoolingConfig();

    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(configFinder.findByNameAndNamespace(poolingConfig, namespace))
        .thenReturn(Optional.empty());

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = ex.getMessage();

    assertEquals("Cannot update coordinator to pooling config " + poolingConfig
        + " because it doesn't exists", resultMessage);

    verify(configFinder).findByNameAndNamespace(eq(poolingConfig), eq(namespace));
  }

  @Test
  void giveAnAttemptToUpdateToAnUnknownShardsPoolingConfig_shouldFail() {
    final StackGresShardedClusterReview review = AdmissionReviewFixtures.shardedCluster()
        .loadConnectionPoolingConfigUpdate().get();

    String poolingConfig =
        review.getRequest().getObject().getSpec().getShards()
        .getConfiguration().getConnectionPoolingConfig();

    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(configFinder.findByNameAndNamespace(poolingConfig, namespace))
        .thenReturn(Optional.empty());

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = ex.getMessage();

    assertEquals("Cannot update coordinator to pooling config " + poolingConfig
        + " because it doesn't exists", resultMessage);

    verify(configFinder).findByNameAndNamespace(eq(poolingConfig), eq(namespace));
  }

  @Test
  void giveAnAttemptToUpdateToAnKnownCoordinatorPooling_shouldNotFail() throws ValidationFailed {
    final StackGresShardedClusterReview review = AdmissionReviewFixtures.shardedCluster()
        .loadConnectionPoolingConfigUpdate().get();

    String poolingConfig =
        review.getRequest().getObject().getSpec().getCoordinator()
        .getConfiguration().getConnectionPoolingConfig();

    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(configFinder.findByNameAndNamespace(poolingConfig, namespace))
        .thenReturn(Optional.of(pgbouncerConfig));

    validator.validate(review);

    verify(configFinder, times(2)).findByNameAndNamespace(
        eq(poolingConfig), eq(namespace));
  }

  @Test
  void giveAnAttemptToUpdateToAnKnownShardsPooling_shouldNotFail() throws ValidationFailed {
    final StackGresShardedClusterReview review = AdmissionReviewFixtures.shardedCluster()
        .loadConnectionPoolingConfigUpdate().get();

    String poolingConfig =
        review.getRequest().getObject().getSpec().getShards()
        .getConfiguration().getConnectionPoolingConfig();

    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(configFinder.findByNameAndNamespace(poolingConfig, namespace))
        .thenReturn(Optional.of(pgbouncerConfig));

    validator.validate(review);

    verify(configFinder, times(2)).findByNameAndNamespace(eq(poolingConfig), eq(namespace));
  }

  @Test
  void giveAnAttemptToDelete_shouldNotFail() throws ValidationFailed {
    final StackGresShardedClusterReview review = AdmissionReviewFixtures.shardedCluster()
        .loadConnectionPoolingConfigUpdate().get();
    review.getRequest().setOperation(Operation.DELETE);

    validator.validate(review);

    verify(configFinder, never()).findByNameAndNamespace(any(), any());
  }

}
