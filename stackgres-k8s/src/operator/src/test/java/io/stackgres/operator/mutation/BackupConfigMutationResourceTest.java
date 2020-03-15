/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation;

import io.stackgres.operator.common.BackupConfigReview;
import io.stackgres.operator.utils.JsonUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BackupConfigMutationResourceTest extends MutationResourceTest<BackupConfigReview> {

  @BeforeEach
  void setUp() {
    final BackupConfigMutationResource resource = new BackupConfigMutationResource();
    resource.setPipeline(pipeline);
    this.resource = resource;

    review = JsonUtil
        .readFromJson("backupconfig_allow_request/create.json", BackupConfigReview.class);
  }

  @Override
  @Test
  void givenAnValidAdmissionReview_itShouldReturnAnyPath() {
    super.givenAnValidAdmissionReview_itShouldReturnAnyPath();
  }

  @Override
  @Test
  void givenAnInvalidAdmissionReview_itShouldReturnABase64EncodedPath() {
    super.givenAnInvalidAdmissionReview_itShouldReturnABase64EncodedPath();
  }

}