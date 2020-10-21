/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.comparator;

import com.fasterxml.jackson.databind.JsonNode;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.internal.PatchUtils;
import io.fabric8.zjsonpatch.JsonDiff;

public class DefaultComparator implements ResourceComparator {

  @Override
  public boolean isResourceContentEqual(HasMetadata r1, HasMetadata r2) {
    JsonNode diff = JsonDiff.asJson(PatchUtils.patchMapper().valueToTree(r1),
        PatchUtils.patchMapper().valueToTree(r2));

    return diff.size() == 0;
  }
}
