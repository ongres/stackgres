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
import io.stackgres.apiweb.dto.distributedlogs.DistributedLogsPodScheduling;
import io.stackgres.apiweb.dto.distributedlogs.DistributedLogsSpec;
import io.stackgres.apiweb.dto.distributedlogs.DistributedLogsSpecAnnotations;
import io.stackgres.apiweb.dto.distributedlogs.DistributedLogsSpecMetadata;
import io.stackgres.apiweb.dto.distributedlogs.DistributedLogsStatus;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsPersistentVolume;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsPodScheduling;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsSpec;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsSpecAnnotations;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsSpecMetadata;

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

    transformation.setScheduling(Optional.ofNullable(source.getScheduling())
        .map(sourceScheduling -> {
          StackGresDistributedLogsPodScheduling targetScheduling =
              new StackGresDistributedLogsPodScheduling();
          targetScheduling.setNodeSelector(sourceScheduling.getNodeSelector());
          targetScheduling.setTolerations(sourceScheduling.getTolerations());
          return targetScheduling;
        }).orElse(null));

    Optional.ofNullable(source.getMetadata())
        .map(DistributedLogsSpecMetadata::getAnnotations)
        .ifPresent(sourceAnnotations -> {
          transformation.setMetadata(new StackGresDistributedLogsSpecMetadata());

          final StackGresDistributedLogsSpecAnnotations targetAnnotations
              = new StackGresDistributedLogsSpecAnnotations();
          transformation.getMetadata().setAnnotations(targetAnnotations);

          if (sourceAnnotations.getAllResources() != null) {
            targetAnnotations.setAllResources(sourceAnnotations.getAllResources());
          }
          if (sourceAnnotations.getPods() != null) {
            targetAnnotations.setPods(sourceAnnotations.getPods());
          }
          if (sourceAnnotations.getServices() != null) {
            targetAnnotations.setServices(sourceAnnotations.getServices());
          }
        });

    return transformation;
  }

  private StackGresDistributedLogsPersistentVolume getCustomResourcePersistentVolume(
      DistributedLogsPersistentVolume source) {
    StackGresDistributedLogsPersistentVolume transformation =
        new StackGresDistributedLogsPersistentVolume();
    transformation.setSize(source.getSize());
    transformation.setStorageClass(source.getStorageClass());
    return transformation;
  }

  private io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsNonProduction
      getCustomResourceNonProduction(DistributedLogsNonProduction source) {
    if (source == null) {
      return null;
    }
    io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsNonProduction transformation =
        new io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsNonProduction();
    transformation.setDisableClusterPodAntiAffinity(source.getDisableClusterPodAntiAffinity());
    return transformation;
  }

  private DistributedLogsSpec getResourceSpec(StackGresDistributedLogsSpec source) {
    DistributedLogsSpec transformation = new DistributedLogsSpec();
    transformation.setPersistentVolume(
        getResourcePersistentVolume(source.getPersistentVolume()));
    transformation.setNonProduction(
        getResourceNonProduction(source.getNonProduction()));

    transformation.setScheduling(Optional.ofNullable(source.getScheduling())
        .map(sourcePodScheduling -> {
          DistributedLogsPodScheduling podScheduling = new DistributedLogsPodScheduling();
          podScheduling.setNodeSelector(sourcePodScheduling.getNodeSelector());
          podScheduling.setTolerations(sourcePodScheduling.getTolerations());
          return podScheduling;
        }).orElse(null));

    Optional.ofNullable(source.getMetadata())
        .map(StackGresDistributedLogsSpecMetadata::getAnnotations)
        .ifPresent(sourceAnnotations -> {
          transformation.setMetadata(new DistributedLogsSpecMetadata());

          final DistributedLogsSpecAnnotations targetAnnotations =
              new DistributedLogsSpecAnnotations();
          transformation.getMetadata().setAnnotations(targetAnnotations);

          if (sourceAnnotations.getAllResources() != null) {
            targetAnnotations.setAllResources(sourceAnnotations.getAllResources());
          }
          if (sourceAnnotations.getPods() != null) {
            targetAnnotations.setPods(sourceAnnotations.getPods());
          }
          if (sourceAnnotations.getServices() != null) {
            targetAnnotations.setServices(sourceAnnotations.getServices());
          }
        });

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
    transformation.setSize(source.getSize());
    transformation.setStorageClass(source.getStorageClass());
    return transformation;
  }

  private DistributedLogsNonProduction getResourceNonProduction(
      io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsNonProduction source) {
    if (source == null) {
      return null;
    }
    DistributedLogsNonProduction transformation = new DistributedLogsNonProduction();
    transformation.setDisableClusterPodAntiAffinity(source.getDisableClusterPodAntiAffinity());
    return transformation;
  }

}
