/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.cluster.factory;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.common.LabelFactoryDelegator;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.common.StackGresClusterResourceStreamFactory;
import io.stackgres.operator.common.StackGresGeneratorContext;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jooq.lambda.Seq;

@ApplicationScoped
public class BackupSecret extends AbstractBackupSecret
    implements StackGresClusterResourceStreamFactory {

  private static final String BACKUP_SECRET_SUFFIX = "-backup";

  private LabelFactoryDelegator factoryDelegator;

  public static String name(StackGresClusterContext context) {
    return ResourceUtil.resourceName(
        context.getCluster().getMetadata().getName() + BACKUP_SECRET_SUFFIX);
  }

  @Override
  public Stream<HasMetadata> streamResources(StackGresGeneratorContext context) {
    Map<String, String> data = new HashMap<String, String>();

    final StackGresClusterContext clusterContext = context.getClusterContext();
    clusterContext.getBackupContext().ifPresent(backupContext -> {
      data.put("BACKUP_CONFIG_RESOURCE_VERSION",
          backupContext.getBackupConfig().getMetadata().getResourceVersion());
      data.putAll(getBackupSecrets(backupContext.getBackupConfig().getSpec(),
          backupContext.getSecrets()));
    });

    final StackGresCluster cluster = clusterContext.getCluster();
    final LabelFactory<?> labelFactory = factoryDelegator.pickFactory(clusterContext);
    return Seq.of(new SecretBuilder()
        .withNewMetadata()
        .withNamespace(cluster.getMetadata().getNamespace())
        .withName(name(clusterContext))
        .withLabels(labelFactory.clusterLabels(cluster))
        .withOwnerReferences(clusterContext.getOwnerReferences())
        .endMetadata()
        .withType("Opaque")
        .withStringData(StackGresUtil.addMd5Sum(data))
        .build());
  }

  @Inject
  public void setFactoryDelegator(LabelFactoryDelegator factoryDelegator) {
    this.factoryDelegator = factoryDelegator;
  }
}
