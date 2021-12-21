/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.comparator;

import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;

import io.stackgres.common.StackGresContext;

public class EndpointComparator extends StackGresAbstractComparator {

  private static final IgnorePatch[] IGNORE_PATCH_PATTERNS = {
      new SimpleIgnorePatch("/metadata/annotations/initialize",
          "add"),
      new SimpleIgnorePatch("/metadata/annotations/acquireTime",
          "add"),
      new SimpleIgnorePatch("/metadata/annotations/history",
          "add"),
      new SimpleIgnorePatch("/metadata/annotations/leader",
          "add"),
      new SimpleIgnorePatch("/metadata/annotations/optime",
          "add"),
      new SimpleIgnorePatch("/metadata/annotations/renewTime", "add"),
      new SimpleIgnorePatch("/metadata/annotations/transitions", "add"),
      new SimpleIgnorePatch("/metadata/annotations/ttl", "add"),
      new SimpleIgnorePatch("/subsets",
          "add"),
      new SimpleIgnorePatch("/metadata/managedFields",
           "add"),
      new FunctionValuePattern(Pattern
          .compile("/metadata/annotations"),
          "add",
          (v) -> v.startsWith("{") && v.endsWith("}") && v.contains("acquireTime")),
      new SimpleIgnorePatch("/metadata/annotations/"
          + StackGresContext.MANAGED_BY_SERVER_SIDE_APPLY_KEY,
          "add"),
  };

  @Override
  protected IgnorePatch[] getPatchPattersToIgnore() {
    return IGNORE_PATCH_PATTERNS;
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
