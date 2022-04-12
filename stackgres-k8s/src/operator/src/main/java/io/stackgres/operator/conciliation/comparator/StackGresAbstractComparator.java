/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.comparator;

import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.base.MoreObjects;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.StackGresContext;

public abstract class StackGresAbstractComparator
    extends DefaultComparator {

  static final SimpleIgnorePatch MANAGED_FIELDS_IGNORE_PATCH =
      new SimpleIgnorePatch("/metadata/managedFields", "add");
  static final ManagedByServerSideApplyIgnorePatch MANAGED_BY_SERVER_SIDE_APPLY_IGNORE_PATCH =
      new ManagedByServerSideApplyIgnorePatch();

  @Override
  public ArrayNode getJsonDiff(HasMetadata required, HasMetadata deployed) {
    ArrayNode diff = getRawJsonDiff(required, deployed);
    for (int index = diff.size() - 1; index >= 0; index--) {
      JsonNode singleDiff = diff.get(index);
      JsonPatch patch = new JsonPatch(singleDiff);
      if (MANAGED_FIELDS_IGNORE_PATCH.matches(patch)
          || MANAGED_BY_SERVER_SIDE_APPLY_IGNORE_PATCH.matches(patch)
          || Arrays.stream(getPatchPattersToIgnore())
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

  protected ArrayNode getRawJsonDiff(HasMetadata required, HasMetadata deployed) {
    return super.getJsonDiff(required, deployed);
  }

  protected abstract IgnorePatch[] getPatchPattersToIgnore();

  protected interface IgnorePatch {

    boolean matches(JsonPatch patch);

  }

  static class ManagedByServerSideApplyIgnorePatch implements IgnorePatch {
    private static final String MANAGED_BY_SERVER_SIDE_APPLY_PATH =
        "/metadata/annotations/"
        + ResourceComparator.escapePatchPath(StackGresContext.MANAGED_BY_SERVER_SIDE_APPLY_KEY);

    public boolean matches(JsonPatch patch) {
      return patch.op.equals("add")
          && (patch.path.equals(MANAGED_BY_SERVER_SIDE_APPLY_PATH)
              || (
                  patch.path.equals("/metadata/annotations")
                  && patch.jsonValue.has(StackGresContext.MANAGED_BY_SERVER_SIDE_APPLY_KEY)
                  )
              );
    }
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
    private final JsonNode jsonValue;

    public JsonPatch(JsonNode jsonPatch) {
      this.op = jsonPatch.get("op").asText();
      this.path = jsonPatch.get("path").asText();
      if (jsonPatch.has("value")) {
        this.jsonValue = jsonPatch.get("value");
        this.value = jsonValue.isObject() ? jsonValue.toString() : jsonValue.asText();
      } else {
        this.jsonValue = null;
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

    public JsonNode getJsonValue() {
      return jsonValue;
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