/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import java.util.Optional;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.stackgres.apiweb.dto.config.ConfigDto;
import io.stackgres.apiweb.dto.config.ConfigSpec;
import io.stackgres.apiweb.dto.config.ConfigStatus;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgconfig.StackGresConfigSpec;
import io.stackgres.common.crd.sgconfig.StackGresConfigStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jetbrains.annotations.Nullable;

@ApplicationScoped
public class ConfigTransformer
    extends AbstractResourceTransformer<ConfigDto, StackGresConfig> {

  private final ObjectMapper mapper;

  @Inject
  public ConfigTransformer(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public StackGresConfig toCustomResource(
      @Nonnull ConfigDto source,
      @Nullable StackGresConfig original) {
    StackGresConfig transformation = Optional.ofNullable(original)
        .map(o -> mapper.convertValue(original, StackGresConfig.class))
        .orElseGet(StackGresConfig::new);

    transformation.setMetadata(getCustomResourceMetadata(source, original));
    transformation.setSpec(getCustomResourceSpec(source.getSpec()));

    return transformation;
  }

  @Override
  public ConfigDto toDto(StackGresConfig source) {
    ConfigDto transformation = new ConfigDto();
    transformation.setMetadata(getDtoMetadata(source));
    transformation.setSpec(getResourceSpec(source.getSpec()));
    transformation.setStatus(getResourceStatus(source.getStatus()));
    return transformation;
  }

  private StackGresConfigSpec getCustomResourceSpec(ConfigSpec source) {
    return mapper.convertValue(source, StackGresConfigSpec.class);
  }

  private ConfigSpec getResourceSpec(StackGresConfigSpec source) {
    return mapper.convertValue(source, ConfigSpec.class);
  }

  private ConfigStatus getResourceStatus(StackGresConfigStatus source) {
    return mapper.convertValue(source, ConfigStatus.class);
  }

}
