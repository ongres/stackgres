/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.comparator;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.zjsonpatch.JsonDiff;

public class EndpointComparator extends StackGresAbstractComparator {

  private static final IgnorePatch[] IGNORE_PATCH_PATTERNS = {
      new SimpleIgnorePatch("/metadata/annotations/initialize",
          "add"),
      new SimpleIgnorePatch("/metadata/annotations/history",
          "add"),
      new SimpleIgnorePatch("/subsets",
          "add"),
      new SimpleIgnorePatch("/metadata/managedFields",
           "add"),
      new FunctionValuePattern(Pattern
          .compile("/metadata/annotations"),
          "add",
          (v) -> v.startsWith("{") && v.endsWith("}") && v.contains("acquireTime")),
  };

  @Override
  protected IgnorePatch[] getPatchPattersToIgnore() {
    return IGNORE_PATCH_PATTERNS;
  }

  @Override
  public boolean isResourceContentEqual(HasMetadata required, HasMetadata deployed) {
    JsonNode diff = JsonDiff.asJson(PATCH_MAPPER.valueToTree(required),
        PATCH_MAPPER.valueToTree(deployed));

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

  private static class FunctionValuePattern extends PatchPattern {

    private final Function<String, Boolean> valueCheck;

    public FunctionValuePattern(Pattern pathPattern, String ops,
                                Function<String, Boolean> valueCheck) {
      super(pathPattern, ops, null);
      this.valueCheck = valueCheck;
    }

    @Override
    public boolean matches(JsonPatch patch) {
      return Objects.equals(op, patch.getOp())
          && valueCheck.apply(patch.getValue())
          && pathPattern.matcher(patch.getPath()).matches();
    }
  }
}
