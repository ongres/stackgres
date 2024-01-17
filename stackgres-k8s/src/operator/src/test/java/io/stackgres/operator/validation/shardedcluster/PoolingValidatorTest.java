/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardedcluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterShardBuilder;
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
  void givenValidCoordinatorStackGresPoolingOnCreation_shouldNotFail() throws ValidationFailed {
    final StackGresShardedClusterReview review = AdmissionReviewFixtures.shardedCluster()
        .loadCreate().get();

    String poolingConfig =
        review.getRequest().getObject().getSpec().getCoordinator()
        .getConfigurationsForCoordinator().getSgPoolingConfig();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();
    when(configFinder.findByNameAndNamespace(poolingConfig, namespace))
        .thenReturn(Optional.of(pgbouncerConfig));

    validator.validate(review);

    verify(configFinder, times(2)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenValidShardsStackGresPoolingOnCreation_shouldNotFail() throws ValidationFailed {
    final StackGresShardedClusterReview review = AdmissionReviewFixtures.shardedCluster()
        .loadCreate().get();

    String poolingConfig =
        review.getRequest().getObject().getSpec().getCoordinator()
        .getConfigurationsForCoordinator().getSgPoolingConfig();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();
    when(configFinder.findByNameAndNamespace(poolingConfig, namespace))
        .thenReturn(Optional.of(pgbouncerConfig));

    validator.validate(review);

    verify(configFinder, times(2)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenValidOverrideShardsStackGresPoolingOnCreation_shouldNotFail() throws ValidationFailed {
    final StackGresShardedClusterReview review = AdmissionReviewFixtures.shardedCluster()
        .loadCreate().get();

    String poolingConfig =
        review.getRequest().getObject().getSpec().getCoordinator()
        .getConfigurationsForCoordinator().getSgPoolingConfig();
    review.getRequest().getObject().getSpec().getShards().setOverrides(List.of(
        new StackGresShardedClusterShardBuilder()
        .withIndex(0)
        .withNewConfigurationsForShards()
        .withSgPoolingConfig(poolingConfig)
        .endConfigurationsForShards()
        .build()));
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();
    when(configFinder.findByNameAndNamespace(poolingConfig, namespace))
        .thenReturn(Optional.of(pgbouncerConfig));

    validator.validate(review);

    verify(configFinder, times(3)).findByNameAndNamespace(any(), any());
  }

  @Test
  void giveInvalidCoordinatorStackGresPoolingOnCreation_shouldFail() {
    final StackGresShardedClusterReview review = AdmissionReviewFixtures.shardedCluster()
        .loadCreate().get();

    String poolingConfig =
        review.getRequest().getObject().getSpec().getCoordinator()
        .getConfigurationsForCoordinator().getSgPoolingConfig();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(configFinder.findByNameAndNamespace(poolingConfig, namespace))
        .thenReturn(Optional.empty());

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = ex.getMessage();

    assertEquals("SGPoolingConfig " + poolingConfig + " not found for coordinator", resultMessage);

    verify(configFinder, times(1)).findByNameAndNamespace(any(), any());
  }

  @Test
  void giveInvalidShardsStackGresPoolingOnCreation_shouldFail() {
    final StackGresShardedClusterReview review = AdmissionReviewFixtures.shardedCluster()
        .loadCreate().get();

    review.getRequest().getObject().getSpec().getShards()
        .getConfigurations().setSgPoolingConfig("test");
    String poolingConfig =
        review.getRequest().getObject().getSpec().getShards()
        .getConfigurations().getSgPoolingConfig();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(configFinder.findByNameAndNamespace(review.getRequest().getObject().getSpec()
        .getCoordinator().getConfigurationsForCoordinator().getSgPoolingConfig(), namespace))
        .thenReturn(Optional.of(pgbouncerConfig));
    when(configFinder.findByNameAndNamespace(poolingConfig, namespace))
        .thenReturn(Optional.empty());

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = ex.getMessage();

    assertEquals("SGPoolingConfig " + poolingConfig + " not found for shards", resultMessage);

    verify(configFinder, times(2)).findByNameAndNamespace(any(), any());
  }

  @Test
  void giveInvalidOverrideShardsStackGresPoolingOnCreation_shouldFail() {
    final StackGresShardedClusterReview review = AdmissionReviewFixtures.shardedCluster()
        .loadCreate().get();

    review.getRequest().getObject().getSpec().getShards().setOverrides(List.of(
        new StackGresShardedClusterShardBuilder()
        .withIndex(0)
        .withNewConfigurationsForShards()
        .withSgPoolingConfig("overrideTest")
        .endConfigurationsForShards()
        .build()));
    String poolingConfig =
        review.getRequest().getObject().getSpec().getShards().getOverrides().get(0)
        .getConfigurationsForShards().getSgPoolingConfig();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(configFinder.findByNameAndNamespace(review.getRequest().getObject().getSpec()
        .getCoordinator().getConfigurationsForCoordinator().getSgPoolingConfig(), namespace))
        .thenReturn(Optional.of(pgbouncerConfig));
    when(configFinder.findByNameAndNamespace(review.getRequest().getObject().getSpec()
        .getShards().getConfigurations().getSgPoolingConfig(), namespace))
        .thenReturn(Optional.of(pgbouncerConfig));
    when(configFinder.findByNameAndNamespace(poolingConfig, namespace))
        .thenReturn(Optional.empty());

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = ex.getMessage();

    assertEquals("SGPoolingConfig " + poolingConfig
        + " not found for shards override 0", resultMessage);

    verify(configFinder, times(3)).findByNameAndNamespace(any(), any());
  }

  @Test
  void giveAnAttemptToUpdateToAnUnknownCoordinatorPoolingConfig_shouldFail() {
    final StackGresShardedClusterReview review = AdmissionReviewFixtures.shardedCluster()
        .loadConnectionPoolingConfigUpdate().get();

    review.getRequest().getObject().getSpec().getCoordinator()
        .getConfigurationsForCoordinator().setSgPoolingConfig("test");
    String poolingConfig =
        review.getRequest().getObject().getSpec().getCoordinator()
        .getConfigurationsForCoordinator().getSgPoolingConfig();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(configFinder.findByNameAndNamespace(poolingConfig, namespace))
        .thenReturn(Optional.empty());

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = ex.getMessage();

    assertEquals("Cannot update coordinator to SGPoolingConfig " + poolingConfig
        + " because it doesn't exists", resultMessage);

    verify(configFinder, times(1)).findByNameAndNamespace(any(), any());
  }

  @Test
  void giveAnAttemptToUpdateToAnUnknownShardsPoolingConfig_shouldFail() {
    final StackGresShardedClusterReview review = AdmissionReviewFixtures.shardedCluster()
        .loadConnectionPoolingConfigUpdate().get();

    review.getRequest().getObject().getSpec().getShards()
        .getConfigurations().setSgPoolingConfig("test");
    String poolingConfig =
        review.getRequest().getObject().getSpec().getShards()
        .getConfigurations().getSgPoolingConfig();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(configFinder.findByNameAndNamespace(poolingConfig, namespace))
        .thenReturn(Optional.empty());

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = ex.getMessage();

    assertEquals("Cannot update shards to SGPoolingConfig " + poolingConfig
        + " because it doesn't exists", resultMessage);

    verify(configFinder, times(1)).findByNameAndNamespace(any(), any());
  }

  @Test
  void giveAnAttemptToUpdateToAnUnknownOverrideShardsPoolingConfig_shouldFail() {
    final StackGresShardedClusterReview review = AdmissionReviewFixtures.shardedCluster()
        .loadConnectionPoolingConfigUpdate().get();

    review.getRequest().getObject().getSpec().getShards().setOverrides(List.of(
        new StackGresShardedClusterShardBuilder()
        .withIndex(0)
        .withNewConfigurationsForShards()
        .withSgPoolingConfig("overrideTest")
        .endConfigurationsForShards()
        .build()));
    String poolingConfig =
        review.getRequest().getObject().getSpec().getShards().getOverrides().get(0)
        .getConfigurationsForShards().getSgPoolingConfig();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(configFinder.findByNameAndNamespace(poolingConfig, namespace))
        .thenReturn(Optional.empty());

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = ex.getMessage();

    assertEquals("Cannot update shards override 0 to SGPoolingConfig " + poolingConfig
        + " because it doesn't exists", resultMessage);

    verify(configFinder, times(1)).findByNameAndNamespace(any(), any());
  }

  @Test
  void giveAnAttemptToUpdateToKnownPoolings_shouldNotFail() throws ValidationFailed {
    final StackGresShardedClusterReview review = AdmissionReviewFixtures.shardedCluster()
        .loadConnectionPoolingConfigUpdate().get();

    validator.validate(review);

    verify(configFinder, never()).findByNameAndNamespace(any(), any());
  }

  void giveAnAttemptToUpdateToAnKnownOverrideShardsPooling_shouldNotFail() throws ValidationFailed {
    final StackGresShardedClusterReview review = AdmissionReviewFixtures.shardedCluster()
        .loadConnectionPoolingConfigUpdate().get();

    String poolingConfig =
        review.getRequest().getObject().getSpec().getShards()
        .getConfigurations().getSgPoolingConfig();
    review.getRequest().getObject().getSpec().getShards().setOverrides(List.of(
        new StackGresShardedClusterShardBuilder()
        .withIndex(0)
        .withNewConfigurationsForShards()
        .withSgPoolingConfig(poolingConfig)
        .endConfigurationsForShards()
        .build()));

    validator.validate(review);

    verify(configFinder, never()).findByNameAndNamespace(any(), any());
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
