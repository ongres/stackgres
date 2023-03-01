/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.comparator;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.PatroniUtil;
import org.jooq.lambda.Seq;

public abstract class EndpointsComparator extends AbstractComparator {

  private static final Pattern ANNOTATIONS_PATTERN =
      Pattern.compile("/metadata/annotations(|/.*)$");

  private final IgnorePatch[] ignorePatchPatterns = {
      new ExcludeExceptPattern(
          List.of(
              new PatchPattern(ANNOTATIONS_PATTERN,
                  "add"),
              new PatchPattern(ANNOTATIONS_PATTERN,
                  "replace")),
          List.of(
              new ConfigAnnotationsIgnorePatch(),
              new InitializeAnnotationsIgnorePatch())),
      new SimpleIgnorePatch("/subsets",
          "add"),
  };

  private final ObjectMapper objectMapper;

  protected EndpointsComparator(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  protected IgnorePatch[] getPatchPattersToIgnore() {
    return ignorePatchPatterns;
  }

  protected class ConfigAnnotationsIgnorePatch extends AnnotationsIgnorePatch {

    public ConfigAnnotationsIgnorePatch() {
      super(PatroniUtil.CONFIG_KEY);
    }

    @Override
    public boolean matches(HasMetadata required, HasMetadata deployed, JsonPatch patch) {
      if (super.matches(required, deployed, patch)) {
        try {
          JsonNode deployedConfig = objectMapper.readTree(
              Optional.ofNullable(deployed.getMetadata().getAnnotations())
              .map(deployedAnnotations -> deployedAnnotations.get(PatroniUtil.CONFIG_KEY))
              .orElse("{}"));
          JsonNode requiredConfig = objectMapper.readTree(
              required.getMetadata().getAnnotations().get(PatroniUtil.CONFIG_KEY));
          return !contained(requiredConfig, deployedConfig);
        } catch (JsonProcessingException ex) {
          throw new RuntimeException(ex);
        }
      }
      return false;
    }

    private boolean contained(JsonNode required, JsonNode deployed) {
      if (required instanceof ObjectNode requiredObject
          && deployed instanceof ObjectNode deployedObject) {
        return objectContained(requiredObject, deployedObject);
      }

      if (required instanceof ArrayNode requiredArray
          && deployed instanceof ArrayNode deployedArray) {
        return arrayContained(requiredArray, deployedArray);
      }

      return required.equals(deployed);
    }

    private boolean objectContained(ObjectNode required, ObjectNode deployed) {
      return Seq.seq(required.fields()).allMatch(e -> deployed.has(e.getKey())
          && contained(e.getValue(), deployed.get(e.getKey())));
    }

    private boolean arrayContained(ArrayNode required, ArrayNode deployed) {
      return required.size() == deployed.size()
          && Seq.zip(Seq.seq(required.elements()), Seq.seq(deployed.elements()))
          .allMatch(e -> contained(e.v1, deployed.get(e.v2.intValue())));
    }
  }

  protected class InitializeAnnotationsIgnorePatch extends AnnotationsIgnorePatch {

    public InitializeAnnotationsIgnorePatch() {
      super(PatroniUtil.INITIALIZE_KEY);
    }

    @Override
    public boolean matches(HasMetadata required, HasMetadata deployed, JsonPatch patch) {
      return !patch.getOp().equals("add") && super.matches(required, deployed, patch);
    }

  }

}
