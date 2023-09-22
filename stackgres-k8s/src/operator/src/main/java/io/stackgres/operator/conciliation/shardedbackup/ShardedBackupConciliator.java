/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedbackup;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;
import io.stackgres.operator.conciliation.AbstractConciliator;
import io.stackgres.operator.conciliation.AbstractDeployedResourcesScanner;
import io.stackgres.operator.conciliation.DeployedResourcesCache;
import io.stackgres.operator.conciliation.RequiredResourceGenerator;

@ApplicationScoped
public class ShardedBackupConciliator extends AbstractConciliator<StackGresShardedBackup> {

  @Inject
  public ShardedBackupConciliator(
      KubernetesClient client,
      RequiredResourceGenerator<StackGresShardedBackup> requiredResourceGenerator,
      AbstractDeployedResourcesScanner<StackGresShardedBackup> deployedResourcesScanner,
      DeployedResourcesCache deployedResourcesCache) {
    super(client, requiredResourceGenerator, deployedResourcesScanner, deployedResourcesCache);
  }

  @Override
  protected boolean skipDeletion(HasMetadata requiredResource, StackGresShardedBackup config) {
    return requiredResource instanceof Job;
  }

}
