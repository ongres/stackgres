/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.app;

import java.util.function.Predicate;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.VersionInfo;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.stackgres.crd.StackGresCluster;
import io.stackgres.crd.StackGresClusterCrd;
import io.stackgres.crd.StackGresClusterDoneable;
import io.stackgres.crd.StackGresClusterList;
import io.stackgres.watcher.StackGresClusterWatcher;

@ApplicationScoped
public class StackGresOperatorApp {

  private static final Logger LOGGER = LoggerFactory.getLogger(StackGresOperatorApp.class);

  @ConfigProperty(name = "stackgres.namespace", defaultValue = "stackgres")
  String namespace;

  @Inject
  KubernetesClientFactory kubClientFactory;

  void onStart(@Observes StartupEvent ev) {
    LOGGER.info("The application is starting...");
    try (KubernetesClient client = kubClientFactory.retrieveKubernetesClient()) {
      log(client.getVersion());
      // Create a namespace for all our stuff
      Namespace ns = new NamespaceBuilder()
          .withNewMetadata()
          .withName(namespace)
          .endMetadata()
          .build();
      client.namespaces().createOrReplace(ns);
      LOGGER.info("Created or replaced namespace: {}", ns);
      startClusterCRDWatcher(client);
    }
  }

  void onStop(@Observes ShutdownEvent ev) {
    LOGGER.info("The application is stopping...");
  }

  private void startClusterCRDWatcher(KubernetesClient client) {
    LOGGER.info("startClusterCRDWatcher");
    createCRDIfNotExists(client, StackGresClusterCrd.CR_DEFINITION,
        StackGresClusterCrd.CRD_NAME::equals);

    KubernetesDeserializer.registerCustomKind(StackGresClusterCrd.CRD_APIVERSION,
        StackGresClusterCrd.CRD_KIND, StackGresCluster.class);

    NonNamespaceOperation<StackGresCluster, StackGresClusterList, StackGresClusterDoneable, Resource<StackGresCluster, StackGresClusterDoneable>> clusterClient =
        kubClientFactory.retrieveKubernetesClient()
            .customResources(StackGresClusterCrd.CR_DEFINITION,
                StackGresCluster.class,
                StackGresClusterList.class,
                StackGresClusterDoneable.class)
            .inNamespace(namespace);

    clusterClient.watch(new StackGresClusterWatcher());
  }

  private static void createCRDIfNotExists(KubernetesClient client,
      CustomResourceDefinition definition, Predicate<String> matcher) {
    boolean exists = client.customResourceDefinitions().list().getItems().stream()
        .map(CustomResourceDefinition::getMetadata)
        .map(ObjectMeta::getName)
        .anyMatch(matcher);

    if (!exists) {
      client.customResourceDefinitions().create(definition);
    }
  }

  private static void log(VersionInfo versionInfo) {
    LOGGER.info("Version details of this Kubernetes cluster :-");
    LOGGER.info("Major        : {}", versionInfo.getMajor());
    LOGGER.info("Minor        : {}", versionInfo.getMinor());
    LOGGER.info("GitVersion   : {}", versionInfo.getGitVersion());
    LOGGER.info("BuildDate    : {}", versionInfo.getBuildDate());
    LOGGER.info("GoVersion    : {}", versionInfo.getGoVersion());
  }

}
