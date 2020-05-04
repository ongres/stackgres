/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest.transformer;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsPersistentVolume;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsSpec;
import io.stackgres.operator.rest.dto.distributedlogs.DistributedLogsDto;
import io.stackgres.operator.rest.dto.distributedlogs.DistributedLogsPersistentVolume;
import io.stackgres.operator.rest.dto.distributedlogs.DistributedLogsSpec;
import io.stackgres.operator.rest.dto.distributedlogs.NonProduction;

@ApplicationScoped
public class DistributedLogsTransformer
    extends AbstractResourceTransformer<DistributedLogsDto, StackGresDistributedLogs> {

  @Override
  public StackGresDistributedLogs toCustomResource(DistributedLogsDto source,
      StackGresDistributedLogs original) {
    StackGresDistributedLogs transformation = Optional.ofNullable(original)
        .orElseGet(StackGresDistributedLogs::new);
    transformation.setMetadata(getCustomResourceMetadata(source, original));
    final DistributedLogsSpec spec = source.getSpec();
    if (spec != null) {
      transformation.setSpec(getCustomResourceSpec(spec));
    }
    return transformation;
  }

  @Override
  public DistributedLogsDto toResource(StackGresDistributedLogs source) {
    DistributedLogsDto transformation = new DistributedLogsDto();
    transformation.setMetadata(getResourceMetadata(source));
    transformation.setSpec(getResourceSpec(source.getSpec()));
    return transformation;
  }

  private StackGresDistributedLogsSpec getCustomResourceSpec(DistributedLogsSpec source) {
    StackGresDistributedLogsSpec transformation = new StackGresDistributedLogsSpec();
    transformation.setPersistentVolume(
        getCustomResourcePersistentVolume(source.getPersistentVolume()));
    transformation.setNonProduction(
        getCustomResourceNonProduction(source.getNonProduction()));
    return transformation;
  }

  private StackGresDistributedLogsPersistentVolume getCustomResourcePersistentVolume(
      DistributedLogsPersistentVolume source) {
    StackGresDistributedLogsPersistentVolume transformation =
        new StackGresDistributedLogsPersistentVolume();
    transformation.setVolumeSize(source.getVolumeSize());
    transformation.setStorageClass(source.getStorageClass());
    return transformation;
  }

  private io.stackgres.common.crd.sgdistributedlogs.NonProduction
      getCustomResourceNonProduction(NonProduction source) {
    if (source == null) {
      return null;
    }
    io.stackgres.common.crd.sgdistributedlogs.NonProduction transformation =
        new io.stackgres.common.crd.sgdistributedlogs.NonProduction();
    transformation.setDisableClusterPodAntiAffinity(source.getDisableClusterPodAntiAffinity());
    return transformation;
  }

  private DistributedLogsSpec getResourceSpec(StackGresDistributedLogsSpec source) {
    DistributedLogsSpec transformation = new DistributedLogsSpec();
    transformation.setPersistentVolume(
        getResourcePersistentVolume(source.getPersistentVolume()));
    transformation.setNonProduction(
        getResourceNonProduction(source.getNonProduction()));
    return transformation;
  }

  private DistributedLogsPersistentVolume getResourcePersistentVolume(
      StackGresDistributedLogsPersistentVolume source) {
    DistributedLogsPersistentVolume transformation = new DistributedLogsPersistentVolume();
    transformation.setVolumeSize(source.getVolumeSize());
    transformation.setStorageClass(source.getStorageClass());
    return transformation;
  }

  private NonProduction getResourceNonProduction(
      io.stackgres.common.crd.sgdistributedlogs.NonProduction source) {
    if (source == null) {
      return null;
    }
    NonProduction transformation = new NonProduction();
    transformation.setDisableClusterPodAntiAffinity(source.getDisableClusterPodAntiAffinity());
    return transformation;
  }

}
