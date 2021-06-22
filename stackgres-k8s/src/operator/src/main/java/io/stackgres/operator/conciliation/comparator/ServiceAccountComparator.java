/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.comparator;

import com.fasterxml.jackson.databind.JsonNode;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.zjsonpatch.JsonDiff;

public class ServiceAccountComparator extends DefaultComparator {

  @Override
  public boolean isResourceContentEqual(HasMetadata required, HasMetadata deployed) {

    final JsonNode source = PATCH_MAPPER.valueToTree(required.getMetadata());
    final JsonNode target = PATCH_MAPPER.valueToTree(deployed.getMetadata());

    JsonNode diff = JsonDiff.asJson(source, target);

    return diff.size() == 0;
  }
}
