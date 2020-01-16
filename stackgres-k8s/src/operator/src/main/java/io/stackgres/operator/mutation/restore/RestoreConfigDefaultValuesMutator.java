/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.restore;

import java.util.List;
import javax.enterprise.context.ApplicationScoped;

import com.github.fge.jsonpatch.JsonPatchOperation;
import io.stackgres.operator.common.RestoreConfigReview;
import io.stackgres.operator.customresource.sgrestoreconfig.StackgresRestoreConfig;
import io.stackgres.operator.mutation.DefaultValuesMutator;

@ApplicationScoped
public class RestoreConfigDefaultValuesMutator
    extends DefaultValuesMutator<StackgresRestoreConfig, RestoreConfigReview>
    implements RestoreMutator {

  @Override
  public List<JsonPatchOperation> mutate(RestoreConfigReview review) {
    return mutate(SG_RESTORE_CONFIG_POINTER, review.getRequest().getObject());
  }
}
