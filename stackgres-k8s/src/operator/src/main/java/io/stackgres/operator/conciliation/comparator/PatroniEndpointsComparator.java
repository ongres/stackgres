/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.comparator;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.patroni.PatroniConfig;

public abstract class PatroniEndpointsComparator extends EndpointsComparator {

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
  };

  private final ObjectMapper objectMapper;

  protected PatroniEndpointsComparator(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  protected IgnorePatch[] getPatchPattersToIgnore() {
    return Stream
        .concat(Stream.of(ignorePatchPatterns), Stream.of(super.getPatchPattersToIgnore()))
        .toArray(IgnorePatch[]::new);
  }

  protected class ConfigAnnotationsIgnorePatch extends AnnotationsIgnorePatch {

    public ConfigAnnotationsIgnorePatch() {
      super(PatroniUtil.CONFIG_KEY);
    }

    @Override
    public boolean matches(HasMetadata required, HasMetadata deployed, JsonPatch patch) {
      if (super.matches(required, deployed, patch)) {
        try {
          String foundConfigString = Optional.ofNullable(deployed.getMetadata().getAnnotations())
              .map(deployedAnnotations -> deployedAnnotations.get(PatroniUtil.CONFIG_KEY))
              .orElse("{}");
          JsonNode previousConfig = objectMapper.valueToTree(
              objectMapper.readValue(foundConfigString, PatroniConfig.class));
          JsonNode requiredConfig = objectMapper.readTree(
              required.getMetadata().getAnnotations().get(PatroniUtil.CONFIG_KEY));
          return !Objects.equals(previousConfig, requiredConfig);
        } catch (JsonProcessingException ex) {
          throw new RuntimeException(ex);
        }
      }
      return false;
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
