/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.comparator;

import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.internal.PatchUtils;
import io.fabric8.zjsonpatch.JsonDiff;

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
          "add")
  };

  @Override
  protected IgnorePatch[] getPatchPattersToIgnore() {
    return IGNORE_PATCH_PATTERNS;
  }

  @Override
  public boolean isResourceContentEqual(HasMetadata r1, HasMetadata r2) {
    JsonNode diff = JsonDiff.asJson(PatchUtils.patchMapper().valueToTree(r1),
        PatchUtils.patchMapper().valueToTree(r2));

    int ignore = countPatchesToIgnore(diff);

    return diff.size() - ignore == 0;
  }

}
