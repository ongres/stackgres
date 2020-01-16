/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.restore;

import com.fasterxml.jackson.databind.JsonNode;
import io.stackgres.operator.WithRestoreReviewResources;
import io.stackgres.operator.customresource.sgrestoreconfig.StackgresRestoreConfig;
import io.stackgres.operator.customresource.sgrestoreconfig.StackgresRestoreConfigSpec;
import io.stackgres.operator.mutation.DefaultValuesMutator;
import io.stackgres.operator.mutation.DefaultValuesMutatorTest;
import io.stackgres.operator.utils.JsonUtil;
import io.stackgres.operator.common.RestoreConfigReview;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RestoreConfigDefaultValuesMutatorTest
    extends DefaultValuesMutatorTest<StackgresRestoreConfig, RestoreConfigReview>
    implements WithRestoreReviewResources {


  @Override
  protected DefaultValuesMutator<StackgresRestoreConfig, RestoreConfigReview> getMutatorInstance() {
    return new RestoreConfigDefaultValuesMutator();
  }

  @Override
  protected RestoreConfigReview getEmptyReview() {
    RestoreConfigReview creationReview = getCreationReview();
    creationReview.getRequest().getObject().setSpec(new StackgresRestoreConfigSpec());
    return creationReview;
  }

  @Override
  protected RestoreConfigReview getDefaultReview() {
    return getCreationReview();
  }

  @Override
  protected StackgresRestoreConfig getDefaultResource() {
    StackgresRestoreConfig stackgresRestoreConfig = JsonUtil
        .readFromJson("restore_config/default.json",
        StackgresRestoreConfig.class);
    return stackgresRestoreConfig;
  }

  @Override
  protected JsonNode getConfJson(JsonNode crJson) {
    return crJson.get("spec");
  }
}