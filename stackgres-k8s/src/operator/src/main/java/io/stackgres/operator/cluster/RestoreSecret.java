/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.cluster;

import java.util.Optional;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;

import com.google.common.collect.ImmutableList;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.common.StackGresClusterResourceStreamFactory;
import io.stackgres.operator.common.StackGresGeneratorContext;
import io.stackgres.operator.resource.ResourceUtil;

import org.jooq.lambda.Seq;

@ApplicationScoped
public class RestoreSecret extends AbstractBackupSecret
    implements StackGresClusterResourceStreamFactory {

  private static final String RESTORE_SECRET_SUFFIX = "-restore";

  public static String name(StackGresClusterContext context) {
    return ResourceUtil.resourceName(
        context.getCluster().getMetadata().getName() + RESTORE_SECRET_SUFFIX);
  }

  @Override
  public Stream<HasMetadata> create(StackGresGeneratorContext context) {
    return Seq.of(context.getClusterContext().getRestoreContext()
        .map(restoreContext -> new SecretBuilder()
            .withNewMetadata()
            .withNamespace(context.getClusterContext().getCluster().getMetadata().getNamespace())
            .withName(name(context.getClusterContext()))
            .withLabels(ResourceUtil.clusterLabels(context.getClusterContext().getCluster()))
            .withOwnerReferences(ImmutableList.of(
                ResourceUtil.getOwnerReference(context.getClusterContext().getCluster())))
            .endMetadata()
            .withType("Opaque")
            .withData(ResourceUtil.addMd5Sum(
                getBackupSecrets(restoreContext.getBackup().getStatus().getBackupConfig(),
                    restoreContext.getSecrets())))
            .build()))
        .filter(Optional::isPresent)
        .map(Optional::get);
  }

  @Override
  protected String getGcsCredentialsFileName() {
    return ClusterStatefulSet.GCS_RESTORE_CREDENTIALS_FILE_NAME;
  }

}
