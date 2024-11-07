/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.replication;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackup.StackGresBackupBuilder;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresReplicationInitializationMode;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operatorframework.resource.ResourceUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@OperatorVersionBinder
public class ReplicationInitializationNewBackup
    implements ResourceGenerator<StackGresClusterContext> {

  private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss")
      .withZone(ZoneId.systemDefault());

  private final LabelFactoryForCluster labelFactory;

  @Inject
  public ReplicationInitializationNewBackup(
      LabelFactoryForCluster labelFactory) {
    super();
    this.labelFactory = labelFactory;
  }

  public static String name(StackGresClusterContext context) {
    return context.getReplicationInitializationBackupToCreate()
        .map(StackGresBackup::getMetadata)
        .map(ObjectMeta::getName)
        .orElseGet(() -> ResourceUtil.resourceName(
            context.getSource().getMetadata().getName()
            + "-" + DATE_TIME_FORMATTER.format(Instant.now())));
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresClusterContext context) {
    if (StackGresReplicationInitializationMode.FROM_NEWLY_CREATED_BACKUP.equals(
        context.getCluster().getSpec().getReplication().getInitializationModeOrDefault())
        && ((context.getReplicationInitializationBackup().isEmpty()
            && Optional.of(context.getCluster().getSpec().getInstances())
            .orElse(0).intValue() > context.getCurrentInstances())
            || (context.getReplicationInitializationBackup().isPresent()
                && Objects.equals(
                    context.getReplicationInitializationBackup()
                    .map(HasMetadata::getMetadata)
                    .map(ObjectMeta::getName),
                    context.getReplicationInitializationBackupToCreate()
                    .map(HasMetadata::getMetadata)
                    .map(ObjectMeta::getName))))) {
      return Stream.of(createReplicationInitializationNewBackup(context));
    } else {
      return Stream.of();
    }
  }

  private StackGresBackup createReplicationInitializationNewBackup(StackGresClusterContext context) {
    final StackGresCluster cluster = context.getSource();
    return new StackGresBackupBuilder()
        .withNewMetadata()
        .withNamespace(cluster.getMetadata().getNamespace())
        .withName(name(context))
        .withLabels(labelFactory.replicationInitializationBackupLabels(cluster))
        .endMetadata()
        .withNewSpec()
        .withSgCluster(context.getSource().getMetadata().getName())
        .withManagedLifecycle(true)
        .endSpec()
        .build();
  }

}
