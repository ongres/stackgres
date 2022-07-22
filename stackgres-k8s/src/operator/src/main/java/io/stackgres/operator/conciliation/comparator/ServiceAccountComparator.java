/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.comparator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.zjsonpatch.JsonDiff;
import io.stackgres.common.StackGresContext;

public class ServiceAccountComparator extends AbstractComparator {

  private static final IgnorePatch[] IGNORE_PATCH_PATTERNS = {
      new AbstractComparator.SimpleIgnorePatch("/managedFields",
          "add"),
      new ManagedByServerSideApplyIgnorePatch(),
      new AbstractComparator.SimpleIgnorePatch("/secrets",
          "add"),
  };

  static class ManagedByServerSideApplyIgnorePatch implements IgnorePatch {
    private static final String MANAGED_BY_SERVER_SIDE_APPLY_PATH =
        "/annotations/"
        + ResourceComparator.escapePatchPath(StackGresContext.MANAGED_BY_SERVER_SIDE_APPLY_KEY);

    public boolean matches(JsonPatch patch) {
      return patch.getOp().equals("add")
          && (patch.getPath().equals(MANAGED_BY_SERVER_SIDE_APPLY_PATH)
              || (
                  patch.getPath().equals("/annotations")
                  && patch.getJsonValue().has(StackGresContext.MANAGED_BY_SERVER_SIDE_APPLY_KEY)
                  )
              );
    }
  }

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
