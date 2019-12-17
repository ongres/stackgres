/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;

import com.google.common.collect.ImmutableList;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.operator.customresource.sgbackup.StackGresBackup;

@ApplicationScoped
public class DefaultBackupResourceHandler
    extends AbstractBackupResourceHandler {

  @Override
  public Stream<HasMetadata> getOrphanResources(KubernetesClient client,
      ImmutableList<StackGresBackup> existingConfigs) {
    return STACKGRES_RESOURCE_OPERATIONS.values()
        .stream()
        .flatMap(resourceOperationGetter -> resourceOperationGetter.apply(client)
            .inAnyNamespace()
            .withLabels(ResourceUtil.defaultLabels())
            .withLabelNotIn(ResourceUtil.BACKUP_NAME_KEY, existingConfigs.stream()
                .map(config -> config.getMetadata().getName())
                .toArray(String[]::new))
            .list()
            .getItems()
            .stream());
  }

  @Override
  public Stream<HasMetadata> getResources(KubernetesClient client,
      StackGresBackup config) {
    return STACKGRES_RESOURCE_OPERATIONS.values()
        .stream()
        .flatMap(resourceOperationGetter -> resourceOperationGetter.apply(client)
            .inNamespace(config.getMetadata().getNamespace())
            .withLabels(ResourceUtil.backupLabels(config.getMetadata().getName()))
            .list()
            .getItems()
            .stream());
  }

}
