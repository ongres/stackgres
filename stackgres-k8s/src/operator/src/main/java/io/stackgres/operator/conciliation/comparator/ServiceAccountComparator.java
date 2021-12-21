/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.comparator;

import java.util.Arrays;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.zjsonpatch.JsonDiff;
import io.stackgres.common.StackGresContext;

public class ServiceAccountComparator extends StackGresAbstractComparator {

  private static final IgnorePatch[] IGNORE_PATCH_PATTERNS = {
      new StackGresAbstractComparator.SimpleIgnorePatch("/managedFields",
          "add"),
      new StackGresAbstractComparator.SimpleIgnorePatch("/secrets",
          "add"),
      new SimpleIgnorePatch("/metadata/annotations/"
          + StackGresContext.MANAGED_BY_SERVER_SIDE_APPLY_KEY,
          "add"),
  };

  @Override
  protected IgnorePatch[] getPatchPattersToIgnore() {
    return IGNORE_PATCH_PATTERNS;
  }

  @Override
  public boolean isResourceContentEqual(HasMetadata required, HasMetadata deployed) {
    return super.isResourceContentEqual(required, deployed);
  }

  @Override
  public ArrayNode getJsonDiff(HasMetadata required, HasMetadata deployed) {

    final JsonNode source = PATCH_MAPPER.valueToTree(required.getMetadata());
    final JsonNode target = PATCH_MAPPER.valueToTree(deployed.getMetadata());
    ArrayNode diff = (ArrayNode) JsonDiff.asJson(source, target);

    for (int index = diff.size() - 1; index >= 0; index--) {
      JsonNode singleDiff = diff.get(index);
      JsonPatch patch = new JsonPatch(singleDiff);
      if (Arrays.stream(getPatchPattersToIgnore())
          .anyMatch(patchPattern -> patchPattern.matches(patch))) {
        diff.remove(index);
      }
    }
    if (LOGGER.isDebugEnabled()) {
      for (JsonNode singleDiff : diff) {
        LOGGER.debug("{}: {} diff {}",
            getClass().getSimpleName(), required.getKind(), singleDiff.toPrettyString());
      }
    }

    return diff;
  }
}
