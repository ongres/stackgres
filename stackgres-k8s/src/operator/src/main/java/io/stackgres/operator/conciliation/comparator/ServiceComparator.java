/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.comparator;

import java.util.regex.Pattern;

import io.stackgres.common.StackGresContext;

public class ServiceComparator extends StackGresAbstractComparator {

  private static final IgnorePatch[] IGNORE_PATCH_PATTERNS = {
      new PatchPattern(Pattern
          .compile("/spec/ports/\\d+/protocol"),
          "add",
          "TCP"),
      new PatchPattern(Pattern
          .compile("/spec/ports/\\d+/targetPort"),
          "add"),
      new PatchValuePattern(Pattern
          .compile("/spec/clusterIP"),
          "add",
          "\\d+\\.\\d+\\.\\d+\\.\\d+"),
      new SimpleIgnorePatch("/spec/sessionAffinity",
          "add",
          "None"),
      new SimpleIgnorePatch("/spec/type",
          "add",
          "ClusterIP"),
      new SimpleIgnorePatch("/status",
          "add"),
      new SimpleIgnorePatch("/metadata/managedFields", "add"),
      new SimpleIgnorePatch("/metadata/annotations/"
          + StackGresContext.MANAGED_BY_SERVER_SIDE_APPLY_KEY,
          "add"),
  };

  @Override
  protected IgnorePatch[] getPatchPattersToIgnore() {
    return IGNORE_PATCH_PATTERNS;
  }

}
