/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.stackgres.apiweb.dto.distributedlogs.DistributedLogsDto;
import io.stackgres.apiweb.dto.distributedlogs.DistributedLogsSpec;
import io.stackgres.apiweb.dto.distributedlogs.DistributedLogsStatus;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsSpec;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsStatus;

@ApplicationScoped
public class DistributedLogsTransformer
    extends AbstractDependencyResourceTransformer<DistributedLogsDto, StackGresDistributedLogs> {

  private final ObjectMapper mapper;

  @Inject
  public DistributedLogsTransformer(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public StackGresDistributedLogs toCustomResource(
      DistributedLogsDto source,
      StackGresDistributedLogs original) {
    StackGresDistributedLogs transformation = Optional.ofNullable(original)
        .map(crd -> mapper.convertValue(original, StackGresDistributedLogs.class))
        .orElseGet(StackGresDistributedLogs::new);
    transformation.setMetadata(getCustomResourceMetadata(source, original));
    transformation.setSpec(getCustomResourceSpec(source.getSpec()));
    if (original != null) {
      if (original.getSpec() != null) {
        transformation.getSpec().setToInstallPostgresExtensions(
            original.getSpec().getToInstallPostgresExtensions());
      }
      if (original.getStatus() != null) {
        transformation.setStatus(original.getStatus());
      }
    }
    return transformation;
  }

  @Override
  public DistributedLogsDto toResource(StackGresDistributedLogs source, List<String> clusters) {
    DistributedLogsDto transformation = new DistributedLogsDto();
    transformation.setMetadata(getResourceMetadata(source));
    transformation.setSpec(getResourceSpec(source.getSpec()));
    transformation.setStatus(getResourceStatus(source.getStatus()));
    if (transformation.getStatus() == null) {
      transformation.setStatus(new DistributedLogsStatus());
    }
    transformation.getStatus().setClusters(clusters);
    return transformation;
  }

  private StackGresDistributedLogsSpec getCustomResourceSpec(DistributedLogsSpec source) {
    return mapper.convertValue(source, StackGresDistributedLogsSpec.class);
  }

  private DistributedLogsSpec getResourceSpec(StackGresDistributedLogsSpec source) {
    return mapper.convertValue(source, DistributedLogsSpec.class);
  }

  private DistributedLogsStatus getResourceStatus(StackGresDistributedLogsStatus source) {
    return mapper.convertValue(source, DistributedLogsStatus.class);
  }

}
