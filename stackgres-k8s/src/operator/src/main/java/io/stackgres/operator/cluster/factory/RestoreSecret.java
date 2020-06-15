/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.cluster.factory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.common.StackGresClusterOptionalResourceStreamFactory;
import io.stackgres.operator.common.StackGresGeneratorContext;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jooq.lambda.Seq;

@ApplicationScoped
public class RestoreSecret extends AbstractBackupSecret
    implements StackGresClusterOptionalResourceStreamFactory {

  private static final String RESTORE_SECRET_SUFFIX = "-restore";

  private LabelFactory<StackGresCluster> labelFactory;

  public static String name(StackGresClusterContext context) {
    return ResourceUtil.resourceName(
        context.getCluster().getMetadata().getName() + RESTORE_SECRET_SUFFIX);
  }

  @Override
  public Stream<Optional<HasMetadata>> streamOptionalResources(StackGresGeneratorContext context) {
    final StackGresClusterContext clusterContext = context.getClusterContext();
    return Seq.of(clusterContext.getRestoreContext()
        .map(restoreContext -> {
          Map<String, String> data = new HashMap<String, String>();

          data.put("BACKUP_RESOURCE_VERSION",
              restoreContext.getBackup().getMetadata().getResourceVersion());

          data.putAll(getBackupSecrets(restoreContext.getBackup().getStatus().getBackupConfig(),
              restoreContext.getSecrets()));

          final StackGresCluster cluster = clusterContext.getCluster();
          return new SecretBuilder()
              .withNewMetadata()
              .withNamespace(cluster.getMetadata().getNamespace())
              .withName(name(clusterContext))
              .withLabels(labelFactory.clusterLabels(cluster))
              .withOwnerReferences(clusterContext.getOwnerReferences())
              .endMetadata()
              .withType("Opaque")
              .withStringData(StackGresUtil.addMd5Sum(data))
              .build();
        }));
  }

  @Inject
  public void setLabelFactory(LabelFactory<StackGresCluster> labelFactory) {
    this.labelFactory = labelFactory;
  }
}
