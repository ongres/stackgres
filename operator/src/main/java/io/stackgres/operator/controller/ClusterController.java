/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.controller;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.common.collect.ImmutableList;

import io.fabric8.kubernetes.api.model.EventBuilder;
import io.fabric8.kubernetes.api.model.EventSourceBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectReferenceBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.SidecarEntry;
import io.stackgres.common.StackGresClusterConfig;
import io.stackgres.common.StackGresSidecarTransformer;
import io.stackgres.common.StackGresUtil;
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
import io.stackgres.operator.services.ResourceCreationSelector;
import io.stackgres.operator.services.SidecarFinder;

import org.jooq.lambda.Unchecked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ClusterController {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClusterController.class);

  @Inject
  KubernetesClientFactory kubClientFactory;

  @Inject
  SidecarFinder sidecarFinder;

  @Inject
  Patroni patroni;

  @Inject
  ResourceCreationSelector creationSelector;

  /**
   * Create all the infrastructure of StackGres.
   *
   * @param cluster Custom Resource with the specification to create the cluster
   */
  public void create(StackGresCluster cluster) throws Exception {
    try (KubernetesClient client = kubClientFactory.create()) {
      try {
        StackGresClusterConfig config = getClusterConfig(cluster, client);
        List<HasMetadata> sgResources = patroni.getResources(config);
        for (HasMetadata sgResource : sgResources) {
          creationSelector.createOrReplace(client, sgResource);
        }
        LOGGER.info("Cluster created: '{}.{}'",
            cluster.getMetadata().getNamespace(),
            cluster.getMetadata().getName());
        sendEvent(EventReason.CLUSTER_CREATED,
            "StackGres Cluster " + cluster.getMetadata().getName() + " created",
            cluster, client);
      } catch (RuntimeException ex) {
        sendEvent(EventReason.CLUSTER_CONFIG_ERROR,
            "StackGres Cluster " + cluster.getMetadata().getName() + " creation failed: "
                + ex.getMessage(), cluster, client);
        throw new RuntimeException(
            "Error while creating resource " + cluster.getMetadata().getNamespace() + "."
                + cluster.getMetadata().getName() + " of type " + cluster.getKind()
                + " (API version " + cluster.getApiVersion() + ")",
            ex);
      }
    }
  }

  /**
   * Update all the infrastructure of StackGres.
   *
   * @param cluster Custom Resource with the specification to create the cluster
   */
  public void update(StackGresCluster cluster) throws Exception {
    try (KubernetesClient client = kubClientFactory.create()) {
      try {
        StackGresClusterConfig config = getClusterConfig(cluster, client);
        List<HasMetadata> sgResources = patroni.getResources(config);
        for (HasMetadata sgResource : sgResources) {
          patroni.update(config, sgResource, client);
        }
        LOGGER.info("Cluster updated: '{}.{}'",
            cluster.getMetadata().getNamespace(),
            cluster.getMetadata().getName());
        sendEvent(EventReason.CLUSTER_UPDATED,
            "StackGres Cluster " + cluster.getMetadata().getName() + " updated",
            cluster, client);
      } catch (RuntimeException ex) {
        sendEvent(EventReason.CLUSTER_CONFIG_ERROR,
            "StackGres Cluster " + cluster.getMetadata().getName() + " update failed: "
                + ex.getMessage(), cluster, client);
        throw new RuntimeException(
            "Error while updating resource " + cluster.getMetadata().getNamespace() + "."
                + cluster.getMetadata().getName() + " of type " + cluster.getKind()
                + " (API version " + cluster.getApiVersion() + ")",
            ex);
      }
    }
  }

  /**
   * Delete full cluster.
   */
  public void delete(StackGresCluster cluster) throws Exception {
    try (KubernetesClient client = kubClientFactory.create()) {
      try {
        StackGresClusterConfig config = getClusterConfig(cluster, client);
        List<HasMetadata> sgResources = new ArrayList<>(patroni.getResources(config));
        Collections.reverse(sgResources);
        for (HasMetadata sgResource : sgResources) {
          client.resource(sgResource).delete();
        }
        LOGGER.info("Cluster deleted: '{}.{}'",
            cluster.getMetadata().getNamespace(),
            cluster.getMetadata().getName());
        sendEvent(EventReason.CLUSTER_DELETED,
            "StackGres Cluster " + cluster.getMetadata().getName() + " deleted",
            cluster, client);
      } catch (RuntimeException ex) {
        sendEvent(EventReason.CLUSTER_CONFIG_ERROR,
            "StackGres Cluster " + cluster.getMetadata().getName() + " deletion failed: "
                + ex.getMessage(), cluster, client);
        throw new RuntimeException(
            "Error while deleting resource " + cluster.getMetadata().getNamespace() + "."
                + cluster.getMetadata().getName() + " of type " + cluster.getKind()
                + " (API version " + cluster.getApiVersion() + ")",
            ex);
      }
    }
  }

  private StackGresClusterConfig getClusterConfig(StackGresCluster cluster,
      KubernetesClient client) {
    return StackGresClusterConfig.builder()
        .withCluster(cluster)
        .withProfile(getProfile(cluster, client))
        .withPostgresConfig(getPostgresConfig(cluster, client))
        .withSidecars(cluster.getSpec().getSidecars().stream()
            .map(sidecar -> sidecarFinder.getSidecarTransformer(sidecar))
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

  /**
   * Send an event.
   */
  public void sendEvent(EventReason reason, String message) {
    sendEvent(reason, message);
  }

  /**
   * Send an event related to a stackgres cluster.
   */
  public void sendEvent(EventReason reason, String message, StackGresCluster cluster) {
    try (KubernetesClient client = kubClientFactory.create()) {
      sendEvent(reason, message, cluster, client);
    }
  }

  private void sendEvent(EventReason reason, String message, StackGresCluster cluster,
      KubernetesClient client) {
    Instant now = Instant.now();
    Long id = new Random().nextLong();
    EventBuilder eventBuilder = new EventBuilder()
        .withNewMetadata()
        .withName((cluster == null ? StackGresUtil.OPERATOR_NAME : cluster.getMetadata().getName())
            + "." + Long.toHexString(id))
        .withNamespace(cluster == null ? StackGresUtil.OPERATOR_NAMESPACE
            : cluster.getMetadata().getNamespace())
        .withLabels(cluster == null ? ResourceUtil.defaultLabels()
            : ResourceUtil.defaultLabels(cluster.getMetadata().getName()))
        .endMetadata()
        .withFirstTimestamp(now.toString())
        .withLastTimestamp(now.toString())
        .withMessage(message)
        .withReason(reason.reason())
        .withSource(new EventSourceBuilder()
            .withComponent(StackGresUtil.OPERATOR_NAME)
            .build());

    if (cluster != null) {
      eventBuilder.withInvolvedObject(new ObjectReferenceBuilder()
          .withApiVersion(cluster.getApiVersion())
          .withKind(cluster.getKind())
          .withNamespace(cluster.getMetadata().getNamespace())
          .withName(cluster.getMetadata().getName())
          .withResourceVersion(cluster.getMetadata().getResourceVersion())
          .withUid(cluster.getMetadata().getUid())
          .build());
    }

    client.resource(eventBuilder.build()).createOrReplace();
  }

}
