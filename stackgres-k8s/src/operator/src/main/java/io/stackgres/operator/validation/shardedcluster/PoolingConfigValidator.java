/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardedcluster;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterShard;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterShards;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpec;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operator.validation.AbstractReferenceValidator;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import org.jooq.lambda.Seq;

@Singleton
@ValidationType(ErrorType.INVALID_CR_REFERENCE)
public class PoolingConfigValidator
    implements ShardedClusterValidator {

  private final CustomResourceFinder<StackGresPoolingConfig> configFinder;

  @Inject
  public PoolingConfigValidator(
      CustomResourceFinder<StackGresPoolingConfig> configFinder) {
    this.configFinder = configFinder;
  }

  private class CoordinatorPoolingConfigValidator
      extends AbstractReferenceValidator<
        StackGresShardedCluster, StackGresShardedClusterReview, StackGresPoolingConfig> {

    private CoordinatorPoolingConfigValidator(
        CustomResourceFinder<StackGresPoolingConfig> configFinder) {
      super(configFinder);
    }

    @Override
    protected Class<StackGresPoolingConfig> getReferenceClass() {
      return StackGresPoolingConfig.class;
    }

    @Override
    protected String getReference(StackGresShardedCluster resource) {
      return Optional.ofNullable(resource.getSpec()
          .getCoordinator().getConfigurations())
          .map(StackGresClusterConfigurations::getSgPoolingConfig)
          .orElse(null);
    }

    @Override
    protected boolean checkReferenceFilter(StackGresShardedClusterReview review) {
      return !Optional.ofNullable(review.getRequest().getDryRun()).orElse(false);
    }

    @Override
    protected void onNotFoundReference(String message) throws ValidationFailed {
      PoolingConfigValidator.this.fail(message);
    }

    @Override
    protected String getCreateNotFoundErrorMessage(String reference) {
      return HasMetadata.getKind(getReferenceClass())
          + " " + reference + " not found for coordinator";
    }

    @Override
    protected String getUpdateNotFoundErrorMessage(String reference) {
      return "Cannot update coordinator to "
          + HasMetadata.getKind(getReferenceClass()) + " "
          + reference + " because it doesn't exists";
    }
  }

  private class ShardsPoolingConfigValidator
      extends CoordinatorPoolingConfigValidator {

    private ShardsPoolingConfigValidator(
        CustomResourceFinder<StackGresPoolingConfig> configFinder) {
      super(configFinder);
    }

    @Override
    protected String getReference(StackGresShardedCluster resource) {
      return Optional.ofNullable(resource.getSpec()
          .getShards().getConfigurations())
          .map(StackGresClusterConfigurations::getSgPoolingConfig)
          .orElse(null);
    }

    @Override
    protected String getCreateNotFoundErrorMessage(String reference) {
      return HasMetadata.getKind(getReferenceClass())
          + " " + reference + " not found for shards";
    }

    @Override
    protected String getUpdateNotFoundErrorMessage(String reference) {
      return "Cannot update shards to "
          + HasMetadata.getKind(getReferenceClass()) + " "
          + reference + " because it doesn't exists";
    }
  }

  private class ShardsOverridePoolingConfigValidator
      extends CoordinatorPoolingConfigValidator {

    private final int index;
    private final Integer shardIndex;

    private ShardsOverridePoolingConfigValidator(
        CustomResourceFinder<StackGresPoolingConfig> configFinder,
        int index,
        Integer shardIndex) {
      super(configFinder);
      this.index = index;
      this.shardIndex = shardIndex;
    }

    @Override
    protected String getReference(StackGresShardedCluster resource) {
      return Optional.ofNullable(resource.getSpec()
          .getShards().getOverrides())
          .map(overrides -> overrides.get(index))
          .map(StackGresShardedClusterShard::getConfigurationsForShards)
          .map(StackGresClusterConfigurations::getSgPoolingConfig)
          .orElse(null);
    }

    @Override
    protected boolean checkReferenceFilter(StackGresShardedClusterReview review) {
      return super.checkReferenceFilter(review)
          && !Objects.equals(
              review.getRequest().getObject().getSpec()
              .getShards().getOverrides().get(index)
              .getConfigurationsForShards().getSgPoolingConfig(),
              Optional.ofNullable(review.getRequest().getOldObject())
              .map(StackGresShardedCluster::getSpec)
              .map(StackGresShardedClusterSpec::getShards)
              .map(StackGresClusterSpec::getConfigurations)
              .map(StackGresClusterConfigurations::getSgPoolingConfig)
              .orElse(null));
    }

    @Override
    protected String getCreateNotFoundErrorMessage(String reference) {
      return HasMetadata.getKind(getReferenceClass())
          + " " + reference + " not found for shards override " + shardIndex;
    }

    @Override
    protected String getUpdateNotFoundErrorMessage(String reference) {
      return "Cannot update shards override " + shardIndex + " to "
          + HasMetadata.getKind(getReferenceClass()) + " "
          + reference + " because it doesn't exists";
    }
  }

  @Override
  @SuppressFBWarnings(value = "SF_SWITCH_NO_DEFAULT",
      justification = "False positive")
  public void validate(StackGresShardedClusterReview review) throws ValidationFailed {
    new CoordinatorPoolingConfigValidator(configFinder).validate(review);
    new ShardsPoolingConfigValidator(configFinder).validate(review);
    for (var overrideShard : Optional.of(review.getRequest().getObject().getSpec().getShards())
        .map(StackGresShardedClusterShards::getOverrides)
        .map(Seq::seq)
        .map(seq -> seq.zipWithIndex().toList())
        .orElse(List.of())) {
      if (overrideShard.v1.getConfigurationsForShards() == null
          || overrideShard.v1.getConfigurationsForShards().getSgPoolingConfig() == null) {
        continue;
      }
      new ShardsOverridePoolingConfigValidator(
          configFinder,
          overrideShard.v2.intValue(),
          overrideShard.v1.getIndex()).validate(review);
    }
  }

}
