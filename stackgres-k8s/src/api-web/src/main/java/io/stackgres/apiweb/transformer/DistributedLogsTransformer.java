/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.apiweb.dto.distributedlogs.DistributedLogsDto;
import io.stackgres.apiweb.dto.distributedlogs.DistributedLogsNonProduction;
import io.stackgres.apiweb.dto.distributedlogs.DistributedLogsPersistentVolume;
import io.stackgres.apiweb.dto.distributedlogs.DistributedLogsSpec;
import io.stackgres.apiweb.dto.distributedlogs.DistributedLogsStatus;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsPersistentVolume;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsSpec;

@ApplicationScoped
public class DistributedLogsTransformer
    extends AbstractDependencyResourceTransformer<DistributedLogsDto, StackGresDistributedLogs> {

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
  public DistributedLogsDto toResource(StackGresDistributedLogs source, List<String> clusters) {
    DistributedLogsDto transformation = new DistributedLogsDto();
    transformation.setMetadata(getResourceMetadata(source));
    transformation.setSpec(getResourceSpec(source.getSpec()));
    transformation.setStatus(getResourceStatus(clusters));
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

  private io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedNonProduction
      getCustomResourceNonProduction(DistributedLogsNonProduction source) {
    if (source == null) {
      return null;
    }
    io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedNonProduction transformation =
        new io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedNonProduction();
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

  private DistributedLogsStatus getResourceStatus(List<String> clusters) {
    DistributedLogsStatus transformation = new DistributedLogsStatus();
    transformation.setClusters(clusters);
    return transformation;
  }

  private DistributedLogsPersistentVolume getResourcePersistentVolume(
      StackGresDistributedLogsPersistentVolume source) {
    DistributedLogsPersistentVolume transformation = new DistributedLogsPersistentVolume();
    transformation.setVolumeSize(source.getVolumeSize());
    transformation.setStorageClass(source.getStorageClass());
    return transformation;
  }

  private DistributedLogsNonProduction getResourceNonProduction(
      io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedNonProduction source) {
    if (source == null) {
      return null;
    }
    DistributedLogsNonProduction transformation = new DistributedLogsNonProduction();
    transformation.setDisableClusterPodAntiAffinity(source.getDisableClusterPodAntiAffinity());
    return transformation;
  }

}
