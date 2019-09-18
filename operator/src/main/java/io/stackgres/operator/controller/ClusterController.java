/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.common.collect.ImmutableList;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.SidecarEntry;
import io.stackgres.common.StackGresClusterConfig;
import io.stackgres.common.StackGresSidecarTransformer;
import io.stackgres.common.customresource.sgcluster.StackGresCluster;
import io.stackgres.common.customresource.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.customresource.sgpgconfig.StackGresPostgresConfigDefinition;
import io.stackgres.common.customresource.sgpgconfig.StackGresPostgresConfigDoneable;
import io.stackgres.common.customresource.sgpgconfig.StackGresPostgresConfigList;
import io.stackgres.common.customresource.sgprofile.StackGresProfile;
import io.stackgres.common.customresource.sgprofile.StackGresProfileDefinition;
import io.stackgres.common.customresource.sgprofile.StackGresProfileDoneable;
import io.stackgres.common.customresource.sgprofile.StackGresProfileList;
import io.stackgres.common.resource.ResourceUtil;
import io.stackgres.operator.app.KubernetesClientFactory;
import io.stackgres.operator.patroni.Patroni;
import io.stackgres.sidecars.StackGresSidecar;

import org.jooq.lambda.Unchecked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ClusterController {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClusterController.class);

  @Inject
  KubernetesClientFactory kubClientFactory;

  @Inject
  Patroni patroni;

  /**
   * Create all the infrastructure of StackGres.
   *
   * @param cluster Custom Resource with the specification to create the cluster
   */
  public void create(StackGresCluster cluster) throws Exception {
    try (KubernetesClient client = kubClientFactory.create()) {
      StackGresClusterConfig config = getClusterConfig(cluster, client);
      List<HasMetadata> sgResources = patroni.getResources(config);
      for (HasMetadata sgResource : sgResources) {
        try {
          client.resource(sgResource).createOrReplace();
        } catch (RuntimeException ex) {
          throw new RuntimeException(
              "Error while creating resource " + sgResource.getMetadata().getNamespace() + "."
                  + sgResource.getMetadata().getName() + " of type " + sgResource.getKind()
                  + " (API version " + sgResource.getApiVersion() + ")",
              ex);
        }
      }
      LOGGER.info("Cluster created: '{}.{}'",
          cluster.getMetadata().getNamespace(),
          cluster.getMetadata().getName());
    }
  }

  /**
   * Update all the infrastructure of StackGres.
   *
   * @param cluster Custom Resource with the specification to create the cluster
   */
  public void update(StackGresCluster cluster) throws Exception {
    try (KubernetesClient client = kubClientFactory.create()) {
      StackGresClusterConfig config = getClusterConfig(cluster, client);
      List<HasMetadata> sgResources = patroni.getResources(config);
      for (HasMetadata sgResource : sgResources) {
        try {
          client.resource(sgResource).createOrReplace();
        } catch (RuntimeException ex) {
          throw new RuntimeException(
              "Error while updating resource " + sgResource.getMetadata().getNamespace() + "."
                  + sgResource.getMetadata().getName() + " of type " + sgResource.getKind()
                  + " (API version " + sgResource.getApiVersion() + ")",
              ex);
        }
      }
      LOGGER.info("Cluster updated: '{}.{}'",
          cluster.getMetadata().getNamespace(),
          cluster.getMetadata().getName());
    }
  }

  /**
   * Delete full cluster.
   */
  public void delete(StackGresCluster cluster) throws Exception {
    try (KubernetesClient client = kubClientFactory.create()) {
      StackGresClusterConfig config = getClusterConfig(cluster, client);
      List<HasMetadata> sgResources = new ArrayList<>(patroni.getResources(config));
      Collections.reverse(sgResources);
      for (HasMetadata sgResource : sgResources) {
        try {
          client.resource(sgResource).deletingExisting();
        } catch (RuntimeException ex) {
          throw new RuntimeException(
              "Error while deleting resource " + sgResource.getMetadata().getNamespace() + "."
                  + sgResource.getMetadata().getName() + " of type " + sgResource.getKind()
                  + " (API version " + sgResource.getApiVersion() + ")",
              ex);
        }
      }
      LOGGER.info("Cluster deleted: '{}.{}'",
          cluster.getMetadata().getNamespace(),
          cluster.getMetadata().getName());
    }
  }

  private StackGresClusterConfig getClusterConfig(StackGresCluster cluster,
      KubernetesClient client) {
    return StackGresClusterConfig.builder()
        .withCluster(cluster)
        .withProfile(getProfile(cluster, client))
        .withPostgresConfig(getPostgresConfig(cluster, client))
        .withSidecars(cluster.getSpec().getSidecars().stream()
            .map(sidecar -> StackGresSidecar.fromName(sidecar).getSidecar())
            .map(Unchecked.function(sidecar -> getSidecarEntry(cluster, client, sidecar)))
            .collect(ImmutableList.toImmutableList()))
        .build();
  }

  private <T extends CustomResource> SidecarEntry<T> getSidecarEntry(StackGresCluster cluster,
      KubernetesClient client, StackGresSidecarTransformer<T> sidecar) throws Exception {
    Optional<T> sidecarConfig = sidecar.getConfig(cluster, client);
    return new SidecarEntry<T>(sidecar, sidecarConfig);
  }

  private Optional<StackGresPostgresConfig> getPostgresConfig(StackGresCluster cluster,
      KubernetesClient client) {
    final String namespace = cluster.getMetadata().getNamespace();
    final String pgConfig = cluster.getSpec().getPostgresConfig();
    if (pgConfig != null) {
      Optional<CustomResourceDefinition> crd =
          ResourceUtil.getCustomResource(client, StackGresPostgresConfigDefinition.NAME);
      if (crd.isPresent()) {
        return Optional.ofNullable(client
            .customResources(crd.get(),
                StackGresPostgresConfig.class,
                StackGresPostgresConfigList.class,
                StackGresPostgresConfigDoneable.class)
            .inNamespace(namespace)
            .withName(pgConfig)
            .get());
      }
    }
    return Optional.empty();
  }

  private Optional<StackGresProfile> getProfile(StackGresCluster cluster,
      KubernetesClient client) {
    final String namespace = cluster.getMetadata().getNamespace();
    final String profileName = cluster.getSpec().getResourceProfile();
    if (profileName != null) {
      Optional<CustomResourceDefinition> crd =
          ResourceUtil.getCustomResource(client, StackGresProfileDefinition.NAME);
      if (crd.isPresent()) {
        return Optional.ofNullable(client
            .customResources(crd.get(),
                StackGresProfile.class,
                StackGresProfileList.class,
                StackGresProfileDoneable.class)
            .inNamespace(namespace)
            .withName(profileName)
            .get());
      }
    }
    return Optional.empty();
  }

}
