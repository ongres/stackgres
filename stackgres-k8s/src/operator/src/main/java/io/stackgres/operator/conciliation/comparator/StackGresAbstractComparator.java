/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.comparator;

import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.MoreObjects;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.zjsonpatch.JsonDiff;

public abstract class StackGresAbstractComparator
    extends DefaultComparator {

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

    return diff.size() - ignore == 0;
  }

  protected abstract IgnorePatch[] getPatchPattersToIgnore();

  protected int countPatchesToIgnore(JsonNode diff) {
    int diffToIgnore = 0;
    for (JsonNode jsonPatch : diff) {
      JsonPatch patch = new JsonPatch(jsonPatch);
      if (Arrays.stream(getPatchPattersToIgnore())
          .anyMatch(patchPattern -> patchPattern.matches(patch))) {
        diffToIgnore++;
      }
    }
    return diffToIgnore;
  }

  protected interface IgnorePatch {

    boolean matches(JsonPatch patch);

  }

  protected static class SimpleIgnorePatch implements IgnorePatch {
    protected final String path;

    protected final String op;

    protected final String value;

    public SimpleIgnorePatch(String pathPattern, String op, String value) {
      this.path = pathPattern;
      this.op = op;
      this.value = value;
    }

    public SimpleIgnorePatch(String pathPattern, String op) {
      this.path = pathPattern;
      this.op = op;
      this.value = null;
    }

    public boolean matches(JsonPatch patch) {
      if (value != null) {
        return Objects.equals(op, patch.op)
            && Objects.equals(value, patch.value)
            && Objects.equals(path, patch.path);
      } else {
        return Objects.equals(op, patch.op)
            && Objects.equals(path, patch.path);
      }
    }
  }

  protected static class PatchPattern extends SimpleIgnorePatch {
    protected final Pattern pathPattern;

    public PatchPattern(Pattern pathPattern, String ops, String value) {
      super(null, ops, value);
      this.pathPattern = pathPattern;
    }

    public PatchPattern(Pattern pathPattern, String ops) {
      super(null, ops, null);
      this.pathPattern = pathPattern;
    }

    public boolean matches(JsonPatch patch) {
      if (value != null) {
        return Objects.equals(op, patch.op)
            && Objects.equals(value, patch.value)
            && pathPattern.matcher(patch.path).matches();
      } else {
        return Objects.equals(op, patch.op)
            && pathPattern.matcher(patch.path).matches();
      }
    }
  }

  protected static class JsonPatch {
    private final String op;
    private final String path;
    private final String value;

    public JsonPatch(JsonNode jsonPatch) {
      this.op = jsonPatch.get("op").asText();
      this.path = jsonPatch.get("path").asText();
      if (jsonPatch.has("value")) {
        final JsonNode diffValue = jsonPatch.get("value");
        this.value = diffValue.isObject() ? diffValue.toString() : diffValue.asText();
      } else {
        this.value = null;
      }
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("op", op)
          .add("path", path)
          .add("value", value)
          .toString();
    }

    public String getOp() {
      return op;
    }

    public String getPath() {
      return path;
    }

    public String getValue() {
      return value;
    }
  }

  protected static class PatchValuePattern extends PatchPattern {

    private final Pattern valuePatter;

    public PatchValuePattern(Pattern pathPattern, String ops, String value) {
      super(pathPattern, ops, value);
      this.valuePatter = Pattern.compile(value);
    }

    @Override
    public boolean matches(JsonPatch patch) {
      return Objects.equals(op, patch.getOp())
          && valuePatter.matcher(patch.getValue()).matches()
          && pathPattern.matcher(patch.getPath()).matches();
    }
  }
}
