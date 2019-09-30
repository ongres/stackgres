/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 */

package io.stackgres.operator.validation.cluster;

import java.util.Optional;

import io.stackgres.common.customresource.sgprofile.StackGresProfile;
import io.stackgres.operator.services.StackgresProfileFinder;
import io.stackgres.operator.utils.JsonUtil;
import io.stackgres.operator.validation.AdmissionReview;
import io.stackgres.operator.validation.Operation;
import io.stackgres.operator.validation.ValidationFailed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.Mockito.*;

class ProfileReferenceValidatorTest {

  private ProfileReference validator;

  private StackgresProfileFinder profileFinder;

  private StackGresProfile xsProfile;

  @BeforeEach
  void setUp() throws Exception {
    profileFinder = mock(StackgresProfileFinder.class);
    validator = new ProfileReference(profileFinder);

    xsProfile = JsonUtil.readFromJson("stackgres_profiles/size-xs.json",
        StackGresProfile.class);

  }

  @Test
  void givenValidStackgresReferenceOnCreation_shouldNotFail() throws ValidationFailed {

    final AdmissionReview review = JsonUtil
        .readFromJson("allowed_requests/valid_creation.json", AdmissionReview.class);

    String resourceProfile = review.getRequest().getObject().getSpec().getResourceProfile();
    when(profileFinder.findProfile(resourceProfile))
        .thenReturn(Optional.of(xsProfile));

    validator.validate(review);

    verify(profileFinder).findProfile(eq(resourceProfile));

  }

  @Test
  void giveInvalidStackgresReferenceOnCreation_shouldFail() {

    final AdmissionReview review = JsonUtil
        .readFromJson("allowed_requests/valid_creation.json", AdmissionReview.class);

    String resourceProfile = review.getRequest().getObject().getSpec().getResourceProfile();

    when(profileFinder.findProfile(resourceProfile))
        .thenReturn(Optional.empty());

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = ex.getMessage();

    assertEquals("Invalid profile " + resourceProfile, resultMessage);

  }

  @Test
  void giveAnAttemptToUpdateToAnUnknownProfile_shouldFail() {

    final AdmissionReview review = JsonUtil
        .readFromJson("allowed_requests/profile_config_update.json", AdmissionReview.class);

    String resourceProfile = review.getRequest().getObject().getSpec().getResourceProfile();

    when(profileFinder.findProfile(resourceProfile))
        .thenReturn(Optional.empty());

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = ex.getMessage();

    assertEquals("Cannot update to profile " + resourceProfile
        + " because it doesn't exists", resultMessage);

    verify(profileFinder).findProfile(eq(resourceProfile));

  }

  @Test
  void giveAnAttemptToUpdateToAnKnownProfile_shouldNotFail() throws ValidationFailed {

    final AdmissionReview review = JsonUtil
        .readFromJson("allowed_requests/profile_config_update.json", AdmissionReview.class);

    String resourceProfile = review.getRequest().getObject().getSpec().getResourceProfile();

    StackGresProfile sProfile = JsonUtil.readFromJson("stackgres_profiles/size-s.json",
        StackGresProfile.class);

    when(profileFinder.findProfile(resourceProfile))
        .thenReturn(Optional.of(sProfile));

    validator.validate(review);

    verify(profileFinder).findProfile(eq(resourceProfile));

  }

  @Test
  void giveAnAttemptToDelete_shouldNotFail() throws ValidationFailed {

    final AdmissionReview review = JsonUtil
        .readFromJson("allowed_requests/profile_config_update.json", AdmissionReview.class);
    review.getRequest().setOperation(Operation.DELETE);

    String resourceProfile = review.getRequest().getObject().getSpec().getResourceProfile();

    when(profileFinder.findProfile(resourceProfile))
        .thenReturn(Optional.of(xsProfile));

    validator.validate(review);

    verify(profileFinder, never()).findProfile(eq(resourceProfile));

  }



}
