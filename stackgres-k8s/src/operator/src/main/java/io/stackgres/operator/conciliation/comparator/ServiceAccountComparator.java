/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.comparator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.zjsonpatch.JsonDiff;

public class ServiceAccountComparator extends AbstractComparator {

  private static final IgnorePatch[] IGNORE_PATCH_PATTERNS = {
      new AbstractComparator.SimpleIgnorePatch("/managedFields",
          "add"),
      new AbstractComparator.SimpleIgnorePatch("/secrets",
          "add"),
  };

  @Override
  protected IgnorePatch[] getPatchPattersToIgnore() {
    return IGNORE_PATCH_PATTERNS;
  }

  @Override
  public ArrayNode getRawJsonDiff(HasMetadata required, HasMetadata deployed) {
    final JsonNode source = PATCH_MAPPER.valueToTree(required.getMetadata());
    final JsonNode target = PATCH_MAPPER.valueToTree(deployed.getMetadata());
    ArrayNode diff = (ArrayNode) JsonDiff.asJson(source, target);
    return diff;
  }

}
