/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 */

package io.stackgres.operator.validation.cluster;

import java.util.Optional;

import io.stackgres.common.customresource.sgprofile.StackGresProfile;
import io.stackgres.operator.services.KubernetesCustomResourceFinder;
import io.stackgres.operator.utils.JsonUtil;
import io.stackgres.operator.validation.StackgresClusterReview;
import io.stackgres.operator.validation.Operation;
import io.stackgres.operator.validation.ValidationFailed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
class ProfileReferenceValidatorValidatorTest {

  private ProfileReferenceValidator validator;

  @Mock
  private KubernetesCustomResourceFinder<StackGresProfile> profileFinder;

  private StackGresProfile xsProfile;

  @BeforeEach
  void setUp() throws Exception {
    validator = new ProfileReferenceValidator(profileFinder);

    xsProfile = JsonUtil.readFromJson("stackgres_profiles/size-xs.json",
        StackGresProfile.class);

  }

  @Test
  void givenValidStackgresReferenceOnCreation_shouldNotFail() throws ValidationFailed {

    final StackgresClusterReview review = JsonUtil
        .readFromJson("allowed_requests/valid_creation.json", StackgresClusterReview.class);

    String resourceProfile = review.getRequest().getObject().getSpec().getResourceProfile();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(profileFinder.findByNameAndNamespace(resourceProfile, namespace))
        .thenReturn(Optional.of(xsProfile));

    validator.validate(review);

    verify(profileFinder).findByNameAndNamespace(eq(resourceProfile), eq(namespace));

  }

  @Test
  void giveInvalidStackgresReferenceOnCreation_shouldFail() {

    final StackgresClusterReview review = JsonUtil
        .readFromJson("allowed_requests/valid_creation.json", StackgresClusterReview.class);

    String resourceProfile = review.getRequest().getObject().getSpec().getResourceProfile();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(profileFinder.findByNameAndNamespace(resourceProfile, namespace))
        .thenReturn(Optional.empty());

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = ex.getMessage();

    assertEquals("Invalid profile " + resourceProfile, resultMessage);

    verify(profileFinder).findByNameAndNamespace(anyString(), anyString());
  }

  @Test
  void giveAnAttemptToUpdateToAnUnknownProfile_shouldFail() {

    final StackgresClusterReview review = JsonUtil
        .readFromJson("allowed_requests/profile_config_update.json", StackgresClusterReview.class);

    String resourceProfile = review.getRequest().getObject().getSpec().getResourceProfile();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(profileFinder.findByNameAndNamespace(resourceProfile, namespace))
        .thenReturn(Optional.empty());

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = ex.getMessage();

    assertEquals("Cannot update to profile " + resourceProfile
        + " because it doesn't exists", resultMessage);

    verify(profileFinder).findByNameAndNamespace(anyString(), anyString());

  }

  @Test
  void giveAnAttemptToUpdateToAnKnownProfile_shouldNotFail() throws ValidationFailed {

    final StackgresClusterReview review = JsonUtil
        .readFromJson("allowed_requests/profile_config_update.json", StackgresClusterReview.class);

    String resourceProfile = review.getRequest().getObject().getSpec().getResourceProfile();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    StackGresProfile sProfile = JsonUtil.readFromJson("stackgres_profiles/size-s.json",
        StackGresProfile.class);

    when(profileFinder.findByNameAndNamespace(resourceProfile, namespace))
        .thenReturn(Optional.of(sProfile));

    validator.validate(review);

    verify(profileFinder).findByNameAndNamespace(anyString(), anyString());

  }

  @Test
  void giveAnAttemptToDelete_shouldNotFail() throws ValidationFailed {

    final StackgresClusterReview review = JsonUtil
        .readFromJson("allowed_requests/profile_config_update.json", StackgresClusterReview.class);
    review.getRequest().setOperation(Operation.DELETE);

    validator.validate(review);

    verify(profileFinder, never()).findByNameAndNamespace(anyString(), anyString());

  }



}
