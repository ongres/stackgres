/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.patroni;

import java.util.AbstractMap.SimpleEntry;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;

import com.google.common.collect.ImmutableList;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.PersistentVolume;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.operator.common.StackGresClusterConfig;
import io.stackgres.operator.controller.ResourceHandlerContext;
import io.stackgres.operator.resource.AbstractResourceHandler;
import io.stackgres.operator.resource.ResourceUtil;
import io.stackgres.operatorframework.resource.PairVisitor;
import io.stackgres.operatorframework.resource.ResourcePairVisitor;

@ApplicationScoped
public class BackupPersistentVolumeHandler extends AbstractResourceHandler {

  @Override
  public boolean isHandlerForResource(StackGresClusterConfig config, HasMetadata resource) {
    return config != null
        && resource.getKind().equals("PersistentVolume")
        && Objects.equals(config.getCluster().getMetadata().getNamespace(),
            resource.getMetadata().getLabels().get(ResourceUtil.CLUSTER_NAMESPACE_KEY))
        && resource.getMetadata().getName().equals(
            config.getCluster().getMetadata().getNamespace() + "-"
                + config.getCluster().getMetadata().getName() + StackGresStatefulSet.BACKUP_SUFFIX);
  }

  @Override
  public boolean equals(ResourceHandlerContext resourceHandlerContext,
      HasMetadata existingResource, HasMetadata requiredResource) {
    return ResourcePairVisitor.equals(new PersistentVolumeVisitor<>(resourceHandlerContext),
        existingResource, requiredResource);
  }

  @Override
  public HasMetadata update(ResourceHandlerContext resourceHandlerContext,
      HasMetadata existingResource, HasMetadata requiredResource) {
    return ResourcePairVisitor.update(new PersistentVolumeVisitor<>(resourceHandlerContext),
        existingResource, requiredResource);
  }

  @Override
  public Stream<HasMetadata> getOrphanResources(KubernetesClient client,
      ImmutableList<StackGresClusterConfig> existingConfigs) {
    ImmutableList<Map.Entry<String, String>> existingConfigsLabels = existingConfigs.stream()
        .map(config -> new SimpleEntry<>(config.getCluster().getMetadata().getName(),
            config.getCluster().getMetadata().getNamespace()))
        .collect(ImmutableList.toImmutableList());
    return client.persistentVolumes()
            .withLabels(ResourceUtil.defaultLabels())
            .list()
            .getItems()
            .stream()
            .filter(serviceMonitor -> !existingConfigsLabels.stream()
                .allMatch(e -> Objects.equals(e.getValue(),
                    serviceMonitor.getMetadata().getLabels().get(e.getKey()))))
            .map(r -> (HasMetadata) r);
  }

  @Override
  public Stream<HasMetadata> getResources(KubernetesClient client,
      StackGresClusterConfig config) {
    return client.persistentVolumes()
        .withLabels(ResourceUtil.defaultLabels(
            config.getCluster().getMetadata().getNamespace(),
            config.getCluster().getMetadata().getName()))
        .list()
        .getItems()
        .stream()
        .map(r -> (HasMetadata) r);
  }

  @Override
  public Optional<HasMetadata> find(KubernetesClient client, HasMetadata resource) {
    return Optional.ofNullable(client.persistentVolumes()
        .withName(resource.getMetadata().getName())
        .get());
  }

  @Override
  public HasMetadata create(KubernetesClient client, HasMetadata resource) {
    return client.persistentVolumes()
        .create((PersistentVolume) resource);
  }

  @Override
  public HasMetadata patch(KubernetesClient client, HasMetadata resource) {
    return client.persistentVolumes()
        .withName(resource.getMetadata().getName())
        .patch((PersistentVolume) resource);
  }

  @Override
  public boolean delete(KubernetesClient client, HasMetadata resource) {
    return client.persistentVolumes()
        .withName(resource.getMetadata().getName())
        .delete();
  }

  private class PersistentVolumeVisitor<T> extends ResourcePairVisitor<T, ResourceHandlerContext> {

    public PersistentVolumeVisitor(ResourceHandlerContext resourceHandlerContext) {
      super(resourceHandlerContext);
    }

    @Override
    public PairVisitor<HasMetadata, T> visit(
        PairVisitor<HasMetadata, T> pairVisitor) {
      return pairVisitor.visit()
          .visit(HasMetadata::getApiVersion, HasMetadata::setApiVersion)
          .visit(HasMetadata::getKind)
          .visitWith(HasMetadata::getMetadata, HasMetadata::setMetadata,
              this::visitMetadata)
          .lastVisit(this::visitPersistentVolume);
    }

  }

}
