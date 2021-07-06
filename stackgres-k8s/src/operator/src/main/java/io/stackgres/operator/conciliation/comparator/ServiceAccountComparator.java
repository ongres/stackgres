/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.comparator;

import java.util.Arrays;

import com.fasterxml.jackson.databind.JsonNode;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.zjsonpatch.JsonDiff;

public class ServiceAccountComparator extends StackGresAbstractComparator {

  private static final IgnorePatch[] IGNORE_PATCH_PATTERNS = {
      new StackGresAbstractComparator.SimpleIgnorePatch("/managedFields",
          "add"),
  };

  @Override
  public boolean isResourceContentEqual(HasMetadata required, HasMetadata deployed) {

    final JsonNode source = PATCH_MAPPER.valueToTree(required.getMetadata());
    final JsonNode target = PATCH_MAPPER.valueToTree(deployed.getMetadata());

    JsonNode diff = JsonDiff.asJson(source, target);

    int ignore = countPatchesToIgnore(diff);

    final int actualDifferences = diff.size() - ignore;
    if (LOGGER.isDebugEnabled() && actualDifferences != 0) {
      for (JsonNode jsonPatch : diff) {
        JsonPatch patch = new JsonPatch(jsonPatch);
        if (Arrays.stream(getPatchPattersToIgnore())
            .noneMatch(patchPattern -> patchPattern.matches(patch))) {
          LOGGER.debug("{} diff {}", required.getKind(), jsonPatch.toPrettyString());
        }
      }
    }

    return actualDifferences == 0;

  }

  @Override
  protected IgnorePatch[] getPatchPattersToIgnore() {
    return IGNORE_PATCH_PATTERNS;
  }
}
