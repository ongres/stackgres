/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.stackgres.apiweb.dto.dbops.DbOpsDto;
import io.stackgres.apiweb.dto.dbops.DbOpsSpec;
import io.stackgres.apiweb.dto.dbops.DbOpsStatus;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsSpec;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DbOpsTransformer
    extends AbstractResourceTransformer<DbOpsDto, StackGresDbOps> {

  private final ObjectMapper mapper;

  @Inject
  public DbOpsTransformer(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public StackGresDbOps toCustomResource(DbOpsDto source,
                                         StackGresDbOps original) {

    StackGresDbOps transformation = Optional.ofNullable(original)
        .map(crd -> mapper.convertValue(crd, StackGresDbOps.class))
        .orElseGet(StackGresDbOps::new);
    transformation.setMetadata(getCustomResourceMetadata(source, original));
    transformation.setSpec(getCustomResourceSpec(source.getSpec()));
    return transformation;
  }

  @Override
  public DbOpsDto toDto(StackGresDbOps source) {
    DbOpsDto transformation = new DbOpsDto();
    transformation.setMetadata(getDtoMetadata(source));
    transformation.setSpec(getResourceSpec(source.getSpec()));
    transformation.setStatus(getResourceStatus(source.getStatus()));
    return transformation;
  }

  private StackGresDbOpsSpec getCustomResourceSpec(DbOpsSpec source) {
    if (source == null) {
      return null;
    }
    return mapper.convertValue(source, StackGresDbOpsSpec.class);
  }

  private DbOpsSpec getResourceSpec(StackGresDbOpsSpec source) {
    return mapper.convertValue(source, DbOpsSpec.class);
  }

  private DbOpsStatus getResourceStatus(StackGresDbOpsStatus status) {
    return mapper.convertValue(status, DbOpsStatus.class);
  }

}
