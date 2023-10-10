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
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
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
public class ProfileReferenceValidator implements ShardedClusterValidator {

  private final CustomResourceFinder<StackGresProfile> profileFinder;

  @Inject
  public ProfileReferenceValidator(CustomResourceFinder<StackGresProfile> profileFinder) {
    this.profileFinder = profileFinder;
  }

  private class CoordinatorProfileReference
      extends AbstractReferenceValidator<
        StackGresShardedCluster, StackGresShardedClusterReview, StackGresProfile> {

    private CoordinatorProfileReference(
        CustomResourceFinder<StackGresProfile> profileFinder) {
      super(profileFinder);
    }

    @Override
    protected Class<StackGresProfile> getReferenceClass() {
      return StackGresProfile.class;
    }

    @Override
    protected String getReference(StackGresShardedCluster resource) {
      return Optional.ofNullable(resource.getSpec()
          .getCoordinator().getSgInstanceProfile())
          .orElse(null);
    }

    @Override
    protected boolean checkReferenceFilter(StackGresShardedClusterReview review) {
      return !Optional.ofNullable(review.getRequest().getDryRun()).orElse(false);
    }

    @Override
    protected void onNotFoundReference(String message) throws ValidationFailed {
      ProfileReferenceValidator.this.fail(message);
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

  private class ShardsProfileReference
      extends CoordinatorProfileReference {

    private ShardsProfileReference(
        CustomResourceFinder<StackGresProfile> profileFinder) {
      super(profileFinder);
    }

    @Override
    protected String getReference(StackGresShardedCluster resource) {
      return Optional.ofNullable(resource.getSpec()
          .getShards().getSgInstanceProfile())
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

  private class ShardsOverrideProfileReference
      extends CoordinatorProfileReference {

    private final int index;
    private final Integer shardIndex;

    private ShardsOverrideProfileReference(
        CustomResourceFinder<StackGresProfile> profileFinder,
        int index,
        Integer shardIndex) {
      super(profileFinder);
      this.index = index;
      this.shardIndex = shardIndex;
    }

    @Override
    protected String getReference(StackGresShardedCluster resource) {
      return Optional.ofNullable(resource.getSpec()
          .getShards().getOverrides())
          .map(overrides -> overrides.get(index))
          .map(StackGresShardedClusterShard::getSgInstanceProfile)
          .orElse(null);
    }

    @Override
    protected boolean checkReferenceFilter(StackGresShardedClusterReview review) {
      return super.checkReferenceFilter(review)
          && !Objects.equals(
              review.getRequest().getObject().getSpec()
              .getShards().getOverrides().get(index).getSgInstanceProfile(),
              Optional.ofNullable(review.getRequest().getOldObject())
              .map(StackGresShardedCluster::getSpec)
              .map(StackGresShardedClusterSpec::getShards)
              .map(StackGresClusterSpec::getSgInstanceProfile)
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
    new CoordinatorProfileReference(profileFinder).validate(review);
    new ShardsProfileReference(profileFinder).validate(review);
    for (var overrideShard : Optional.ofNullable(review.getRequest().getObject())
        .map(StackGresShardedCluster::getSpec)
        .map(StackGresShardedClusterSpec::getShards)
        .map(StackGresShardedClusterShards::getOverrides)
        .map(Seq::seq)
        .map(seq -> seq.zipWithIndex().toList())
        .orElse(List.of())) {
      if (overrideShard.v1.getSgInstanceProfile() == null) {
        continue;
      }
      new ShardsOverrideProfileReference(
          profileFinder,
          overrideShard.v2.intValue(),
          overrideShard.v1.getIndex()).validate(review);
    }
  }

}
