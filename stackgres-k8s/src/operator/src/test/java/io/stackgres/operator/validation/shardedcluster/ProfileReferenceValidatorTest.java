/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardedcluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import io.stackgres.common.crd.sgprofile.StackGresProfile;
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
class ProfileReferenceValidatorTest {

  private ProfileReferenceValidator validator;

  @Mock
  private AbstractCustomResourceFinder<StackGresProfile> profileFinder;

  private StackGresProfile profileSizeXs;

  @BeforeEach
  void setUp() throws Exception {
    validator = new ProfileReferenceValidator(profileFinder);

    profileSizeXs = Fixtures.instanceProfile().loadSizeXs().get();
  }

  @Test
  void givenValidCoordinatorStackGresReferenceOnCreation_shouldNotFail() throws ValidationFailed {
    final StackGresShardedClusterReview review =
        AdmissionReviewFixtures.shardedCluster().loadCreate().get();

    String resourceProfile = review.getRequest().getObject().getSpec().getCoordinator()
        .getSgInstanceProfile();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(profileFinder.findByNameAndNamespace(resourceProfile, namespace))
        .thenReturn(Optional.of(profileSizeXs));

    validator.validate(review);

    verify(profileFinder, times(2)).findByNameAndNamespace(eq(resourceProfile), eq(namespace));
  }

  @Test
  void givenValidShardsStackGresReferenceOnCreation_shouldNotFail() throws ValidationFailed {
    final StackGresShardedClusterReview review =
        AdmissionReviewFixtures.shardedCluster().loadCreate().get();

    String resourceProfile = review.getRequest().getObject().getSpec().getShards()
        .getSgInstanceProfile();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(profileFinder.findByNameAndNamespace(resourceProfile, namespace))
        .thenReturn(Optional.of(profileSizeXs));

    validator.validate(review);

    verify(profileFinder, times(2)).findByNameAndNamespace(eq(resourceProfile), eq(namespace));
  }

  @Test
  void givenValidOverrideShardsStackGresReferenceOnCreation_shouldNotFail()
      throws ValidationFailed {
    final StackGresShardedClusterReview review =
        AdmissionReviewFixtures.shardedCluster().loadCreate().get();

    String resourceProfile = review.getRequest().getObject().getSpec().getShards()
        .getSgInstanceProfile();
    review.getRequest().getObject().getSpec().getShards().setOverrides(List.of(
        new StackGresShardedClusterShardBuilder()
        .withIndex(0)
        .withSgInstanceProfile(resourceProfile)
        .build()));
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(profileFinder.findByNameAndNamespace(resourceProfile, namespace))
        .thenReturn(Optional.of(profileSizeXs));

    validator.validate(review);

    verify(profileFinder, times(3)).findByNameAndNamespace(anyString(), anyString());
  }

  @Test
  void giveInvalidCoordinatorStackGresReferenceOnCreation_shouldFail() {
    final StackGresShardedClusterReview review =
        AdmissionReviewFixtures.shardedCluster().loadCreate().get();

    String resourceProfile = review.getRequest().getObject().getSpec().getCoordinator()
        .getSgInstanceProfile();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(profileFinder.findByNameAndNamespace(resourceProfile, namespace))
        .thenReturn(Optional.empty());

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = ex.getMessage();

    assertEquals("SGInstanceProfile " + resourceProfile
        + " not found for coordinator", resultMessage);

    verify(profileFinder, times(1)).findByNameAndNamespace(anyString(), anyString());
  }

  @Test
  void giveInvalidShardsStackGresReferenceOnCreation_shouldFail() {
    final StackGresShardedClusterReview review =
        AdmissionReviewFixtures.shardedCluster().loadCreate().get();

    review.getRequest().getObject().getSpec().getShards()
        .setSgInstanceProfile("test");
    String resourceProfile = review.getRequest().getObject().getSpec().getShards()
        .getSgInstanceProfile();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(profileFinder.findByNameAndNamespace(review.getRequest().getObject().getSpec()
        .getCoordinator().getSgInstanceProfile(), namespace))
        .thenReturn(Optional.of(profileSizeXs));
    when(profileFinder.findByNameAndNamespace(resourceProfile, namespace))
        .thenReturn(Optional.empty());

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = ex.getMessage();

    assertEquals("SGInstanceProfile " + resourceProfile + " not found for shards", resultMessage);

    verify(profileFinder, times(2)).findByNameAndNamespace(anyString(), anyString());
  }

  @Test
  void giveInvalidOverrideShardsStackGresReferenceOnCreation_shouldFail() {
    final StackGresShardedClusterReview review =
        AdmissionReviewFixtures.shardedCluster().loadCreate().get();

    review.getRequest().getObject().getSpec().getShards().setOverrides(List.of(
        new StackGresShardedClusterShardBuilder()
        .withIndex(0)
        .withSgInstanceProfile("test")
        .build()));
    String ovverideResourceProfile = review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0).getSgInstanceProfile();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(profileFinder.findByNameAndNamespace(review.getRequest().getObject().getSpec()
        .getCoordinator().getSgInstanceProfile(), namespace))
        .thenReturn(Optional.of(profileSizeXs));
    when(profileFinder.findByNameAndNamespace(review.getRequest().getObject().getSpec()
        .getShards().getSgInstanceProfile(), namespace))
        .thenReturn(Optional.of(profileSizeXs));
    when(profileFinder.findByNameAndNamespace(ovverideResourceProfile, namespace))
        .thenReturn(Optional.empty());

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = ex.getMessage();

    assertEquals("SGInstanceProfile " + ovverideResourceProfile
        + " not found for shards override 0", resultMessage);

    verify(profileFinder, times(3)).findByNameAndNamespace(anyString(), anyString());
  }

  @Test
  void giveAnAttemptToUpdateCoordinatorToAnUnknownProfile_shouldFail() {
    final StackGresShardedClusterReview review =
        AdmissionReviewFixtures.shardedCluster().loadProfileConfigUpdate().get();

    review.getRequest().getObject().getSpec().getCoordinator()
        .setSgInstanceProfile("test");
    String resourceProfile = review.getRequest().getObject().getSpec().getCoordinator()
        .getSgInstanceProfile();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(profileFinder.findByNameAndNamespace(resourceProfile, namespace))
        .thenReturn(Optional.empty());

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = ex.getMessage();

    assertEquals("Cannot update coordinator to SGInstanceProfile " + resourceProfile
        + " because it doesn't exists", resultMessage);

    verify(profileFinder, times(1)).findByNameAndNamespace(anyString(), anyString());
  }

  @Test
  void giveAnAttemptToUpdateShardsToAnUnknownProfile_shouldFail() {
    final StackGresShardedClusterReview review =
        AdmissionReviewFixtures.shardedCluster().loadProfileConfigUpdate().get();

    review.getRequest().getObject().getSpec().getShards()
        .setSgInstanceProfile("test");
    String resourceProfile = review.getRequest().getObject().getSpec().getShards()
        .getSgInstanceProfile();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(profileFinder.findByNameAndNamespace(resourceProfile, namespace))
        .thenReturn(Optional.empty());

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = ex.getMessage();

    assertEquals("Cannot update shards to SGInstanceProfile " + resourceProfile
        + " because it doesn't exists", resultMessage);

    verify(profileFinder, times(1)).findByNameAndNamespace(anyString(), anyString());
  }

  @Test
  void giveAnAttemptToUpdateOverrideShardsToAnUnknownProfile_shouldFail() {
    final StackGresShardedClusterReview review =
        AdmissionReviewFixtures.shardedCluster().loadProfileConfigUpdate().get();

    review.getRequest().getObject().getSpec().getShards().setOverrides(List.of(
        new StackGresShardedClusterShardBuilder()
        .withIndex(0)
        .withSgInstanceProfile("test")
        .build()));
    String ovverideResourceProfile = review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0).getSgInstanceProfile();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(profileFinder.findByNameAndNamespace(ovverideResourceProfile, namespace))
        .thenReturn(Optional.empty());

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = ex.getMessage();

    assertEquals("Cannot update shards override 0 to SGInstanceProfile "
        + ovverideResourceProfile + " because it doesn't exists", resultMessage);

    verify(profileFinder, times(1)).findByNameAndNamespace(anyString(), anyString());
  }

  @Test
  void giveAnAttemptToUpdateToKnownProfiles_shouldNotFail() throws ValidationFailed {
    final StackGresShardedClusterReview review =
        AdmissionReviewFixtures.shardedCluster().loadProfileConfigUpdate().get();

    validator.validate(review);

    verify(profileFinder, never()).findByNameAndNamespace(anyString(), anyString());
  }

  @Test
  void giveAnAttemptToUpdateOverrideShardsToAnKnownProfile_shouldNotFail() throws ValidationFailed {
    final StackGresShardedClusterReview review =
        AdmissionReviewFixtures.shardedCluster().loadProfileConfigUpdate().get();

    String resourceProfile = review.getRequest().getObject().getSpec().getShards()
        .getSgInstanceProfile();
    review.getRequest().getObject().getSpec().getShards().setOverrides(List.of(
        new StackGresShardedClusterShardBuilder()
        .withIndex(0)
        .withSgInstanceProfile(resourceProfile)
        .build()));

    validator.validate(review);

    verify(profileFinder, never()).findByNameAndNamespace(anyString(), anyString());
  }

  @Test
  void giveAnAttemptToDelete_shouldNotFail() throws ValidationFailed {

    final StackGresShardedClusterReview review =
        AdmissionReviewFixtures.shardedCluster().loadProfileConfigUpdate().get();
    review.getRequest().setOperation(Operation.DELETE);

    validator.validate(review);

    verify(profileFinder, never()).findByNameAndNamespace(anyString(), anyString());
  }

}
