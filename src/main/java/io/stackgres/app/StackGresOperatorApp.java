/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.app;

import java.net.SocketTimeoutException;
import java.util.function.Predicate;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.VersionInfo;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.stackgres.crd.sgcluster.StackGresCluster;
import io.stackgres.crd.sgcluster.StackGresClusterDefinition;
import io.stackgres.crd.sgcluster.StackGresClusterDoneable;
import io.stackgres.crd.sgcluster.StackGresClusterList;
import io.stackgres.watcher.StackGresClusterWatcher;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class StackGresOperatorApp {

  private static final Logger LOGGER = LoggerFactory.getLogger(StackGresOperatorApp.class);

  @ConfigProperty(name = "stackgres.namespace", defaultValue = "stackgres")
  String namespace;

  @Inject
  KubernetesClientFactory kubClientFactory;

  @Inject
  StackGresClusterWatcher watcher;

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
      startClusterCrdWatcher(client);
    } catch (KubernetesClientException e) {
      if (e.getCause() instanceof SocketTimeoutException) {
        LOGGER.error("Kubernetes cluster is not reachable, check your connection.");
      }
      throw e;
    }
  }

  void onStop(@Observes ShutdownEvent ev) {
    LOGGER.info("The application is stopping...");
  }

  private void startClusterCrdWatcher(KubernetesClient client) {
    LOGGER.info("startClusterCRDWatcher");
    createCrdIfNotExists(client, StackGresClusterDefinition.CR_DEFINITION,
        StackGresClusterDefinition.NAME::equals);

    KubernetesDeserializer.registerCustomKind(StackGresClusterDefinition.APIVERSION,
        StackGresClusterDefinition.KIND, StackGresCluster.class);

    kubClientFactory.retrieveKubernetesClient()
        .customResources(StackGresClusterDefinition.CR_DEFINITION,
            StackGresCluster.class,
            StackGresClusterList.class,
            StackGresClusterDoneable.class)
        .inNamespace(namespace)
        .watch(watcher);
  }

  private static void createCrdIfNotExists(KubernetesClient client,
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
  }

}
