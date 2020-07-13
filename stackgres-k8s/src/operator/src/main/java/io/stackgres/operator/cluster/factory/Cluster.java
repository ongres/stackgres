/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.cluster.factory;

import java.util.List;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.operator.backup.Backup;
import io.stackgres.operator.common.StackGresGeneratorContext;
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
  private final Backup backup;
  private final AnnotationDecorator annotationDecorator;

  @Inject
  public Cluster(ClusterStatefulSet clusterStatefulSet,
                 BackupConfigMap backupConfigMap,
                 RestoreConfigMap restoreConfigMap,
                 BackupSecret backupSecret,
                 RestoreSecret restoreSecret,
                 BackupCronJob backupCronJob,
                 Backup backup,
                 AnnotationDecorator annotationDecorator) {
    super();
    this.clusterStatefulSet = clusterStatefulSet;
    this.backupCronJob = backupCronJob;
    this.backupConfigMap = backupConfigMap;
    this.backupSecret = backupSecret;
    this.restoreConfigMap = restoreConfigMap;
    this.restoreSecret = restoreSecret;
    this.backup = backup;
    this.annotationDecorator = annotationDecorator;
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
        .append(backup)
        .stream()
        .collect(ImmutableList.toImmutableList());
    annotationDecorator.decorate(context.getClusterContext().getCluster(), resources);
    return resources.stream();
  }

}
