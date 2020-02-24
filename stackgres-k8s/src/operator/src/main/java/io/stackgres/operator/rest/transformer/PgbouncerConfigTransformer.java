/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest.transformer;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.operator.rest.dto.pgbouncerconfig.PgbouncerConfigDto;
import io.stackgres.operator.rest.dto.pgbouncerconfig.PgbouncerConfigSpec;
import io.stackgres.operator.sidecars.pgbouncer.customresources.StackGresPgbouncerConfig;
import io.stackgres.operator.sidecars.pgbouncer.customresources.StackGresPgbouncerConfigSpec;

@ApplicationScoped
public class PgbouncerConfigTransformer
    extends AbstractResourceTransformer<PgbouncerConfigDto, StackGresPgbouncerConfig> {

  @Override
  public StackGresPgbouncerConfig toCustomResource(PgbouncerConfigDto source) {
    StackGresPgbouncerConfig transformation = new StackGresPgbouncerConfig();
    transformation.setMetadata(getCustomResourceMetadata(source));
    transformation.setSpec(getCustomResourceSpec(source.getSpec()));
    return transformation;
  }

  @Override
  public PgbouncerConfigDto toResource(StackGresPgbouncerConfig source) {
    PgbouncerConfigDto transformation = new PgbouncerConfigDto();
    transformation.setMetadata(getResourceMetadata(source));
    transformation.setSpec(getResourceSpec(source.getSpec()));
    return transformation;
  }

  private StackGresPgbouncerConfigSpec getCustomResourceSpec(PgbouncerConfigSpec source) {
    StackGresPgbouncerConfigSpec transformation = new StackGresPgbouncerConfigSpec();
    transformation.setPgbouncerConf(source.getPgbouncerConf());
    return transformation;
  }

  private PgbouncerConfigSpec getResourceSpec(StackGresPgbouncerConfigSpec source) {
    PgbouncerConfigSpec transformation = new PgbouncerConfigSpec();
    transformation.setPgbouncerConf(source.getPgbouncerConf());
    return transformation;
  }

}
