/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.cluster.factory;

import java.util.List;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.operator.backup.BackupJob;
import io.stackgres.operator.common.StackGresGeneratorContext;
import io.stackgres.operator.dbops.DbOps;
import io.stackgres.operatorframework.resource.ResourceGenerator;
import io.stackgres.operatorframework.resource.factory.SubResourceStreamFactory;

@ApplicationScoped
public class Cluster
    implements SubResourceStreamFactory<HasMetadata,
    StackGresGeneratorContext> {

  private final ClusterStatefulSet clusterStatefulSet;
  private final BackupCronJob backupCronJob;
  private final BackupConfigMap backupConfigMap;
  private final BackupSecret backupSecret;
  private final RestoreConfigMap restoreConfigMap;
  private final RestoreSecret restoreSecret;
  private final BackupJob backupJob;
  private final DbOps dbOps;
  private final AnnotationDecorator annotationDecorator;

  @Dependent
  public static class Parameters {
    @Inject ClusterStatefulSet clusterStatefulSet;
    @Inject BackupCronJob backupCronJob;
    @Inject BackupConfigMap backupConfigMap;
    @Inject BackupSecret backupSecret;
    @Inject RestoreConfigMap restoreConfigMap;
    @Inject RestoreSecret restoreSecret;
    @Inject BackupJob backupJob;
    @Inject DbOps dbOps;
    @Inject AnnotationDecorator annotationDecorator;
  }

  @Inject
  public Cluster(Parameters parameters) {
    this.clusterStatefulSet = parameters.clusterStatefulSet;
    this.backupCronJob = parameters.backupCronJob;
    this.backupConfigMap = parameters.backupConfigMap;
    this.backupSecret = parameters.backupSecret;
    this.restoreConfigMap = parameters.restoreConfigMap;
    this.restoreSecret = parameters.restoreSecret;
    this.backupJob = parameters.backupJob;
    this.dbOps = parameters.dbOps;
    this.annotationDecorator = parameters.annotationDecorator;
  }

  @Override
  public Stream<HasMetadata> streamResources(StackGresGeneratorContext context) {
    final List<HasMetadata> resources = ResourceGenerator
        .with(context)
        .of(HasMetadata.class)
        .append(clusterStatefulSet)
        .append(backupCronJob)
        .append(backupConfigMap)
        .append(backupSecret)
        .append(restoreConfigMap)
        .append(restoreSecret)
        .append(backupJob)
        .append(dbOps)
        .stream()
        .collect(ImmutableList.toImmutableList());
    annotationDecorator.decorate(context.getClusterContext().getCluster(), resources);
    return resources.stream();
  }

}
