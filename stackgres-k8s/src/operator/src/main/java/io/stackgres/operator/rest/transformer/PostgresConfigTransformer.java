/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest.transformer;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.operator.customresource.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.operator.customresource.sgpgconfig.StackGresPostgresConfigSpec;
import io.stackgres.operator.rest.dto.pgconfig.PostgresConfigDto;
import io.stackgres.operator.rest.dto.pgconfig.PostgresConfigSpec;

@ApplicationScoped
public class PostgresConfigTransformer
    extends AbstractResourceTransformer<PostgresConfigDto, StackGresPostgresConfig> {

  @Override
  public StackGresPostgresConfig toCustomResource(PostgresConfigDto source) {
    StackGresPostgresConfig transformation = new StackGresPostgresConfig();
    transformation.setMetadata(getCustomResourceMetadata(source));
    transformation.setSpec(getCustomResourceSpec(source.getSpec()));
    return transformation;
  }

  @Override
  public PostgresConfigDto toResource(StackGresPostgresConfig source) {
    PostgresConfigDto transformation = new PostgresConfigDto();
    transformation.setMetadata(getResourceMetadata(source));
    transformation.setSpec(getResourceSpec(source.getSpec()));
    return transformation;
  }

  private StackGresPostgresConfigSpec getCustomResourceSpec(PostgresConfigSpec source) {
    StackGresPostgresConfigSpec transformation = new StackGresPostgresConfigSpec();
    transformation.setPgVersion(source.getPgVersion());
    transformation.setPostgresqlConf(source.getPostgresqlConf());
    return transformation;
  }

  private PostgresConfigSpec getResourceSpec(StackGresPostgresConfigSpec source) {
    PostgresConfigSpec transformation = new PostgresConfigSpec();
    transformation.setPgVersion(source.getPgVersion());
    transformation.setPostgresqlConf(source.getPostgresqlConf());
    return transformation;
  }

}
