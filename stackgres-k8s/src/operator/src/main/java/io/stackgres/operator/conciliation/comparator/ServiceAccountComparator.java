/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.comparator;

import static io.stackgres.common.StackGresContext.MANAGED_BY_SERVER_SIDE_APPLY_KEY;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.zjsonpatch.JsonDiff;

public class ServiceAccountComparator extends StackGresAbstractComparator {

  private static final IgnorePatch[] IGNORE_PATCH_PATTERNS = {
      new StackGresAbstractComparator.SimpleIgnorePatch("/managedFields",
          "add"),
      new StackGresAbstractComparator.SimpleIgnorePatch("/annotations",
          "add", "{\"" + MANAGED_BY_SERVER_SIDE_APPLY_KEY + "\":\"true\"}"),
      new StackGresAbstractComparator.SimpleIgnorePatch("/annotations/"
          + ResourceComparator.escapePatchPath(MANAGED_BY_SERVER_SIDE_APPLY_KEY),
          "add", "\"true\""),
      new StackGresAbstractComparator.SimpleIgnorePatch("/secrets",
          "add"),
  };

  @Override
  protected IgnorePatch[] getPatchPattersToIgnore() {
    return IGNORE_PATCH_PATTERNS;
  }

  public ArrayNode getRawJsonDiff(HasMetadata required, HasMetadata deployed) {
    final JsonNode source = PATCH_MAPPER.valueToTree(required.getMetadata());
    final JsonNode target = PATCH_MAPPER.valueToTree(deployed.getMetadata());
    ArrayNode diff = (ArrayNode) JsonDiff.asJson(source, target);
    return diff;
  }

}
