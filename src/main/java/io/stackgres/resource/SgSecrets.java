/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.resource;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.SecretList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.app.KubernetesClientFactory;
import io.stackgres.crd.sgcluster.StackGresCluster;
import io.stackgres.util.ResourceUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("initialization.fields.uninitialized")
@ApplicationScoped
public class SgSecrets {

  private static final Logger LOGGER = LoggerFactory.getLogger(SgSecrets.class);

  @ConfigProperty(name = "stackgres.namespace", defaultValue = "stackgres")
  String namespace;

  @Inject
  KubernetesClientFactory kubClientFactory;

  /**
   * Create the Service associated to the cluster.
   */
  public Secret create(StackGresCluster sgcluster) {
    final String name = sgcluster.getMetadata().getName();

    Map<String, String> labels = new HashMap<>();
    labels.put("app", "StackGres");
    labels.put("cluster-name", name);

    Map<String, String> data = new HashMap<>();
    data.put("superuser-password", generatePassword());
    data.put("replication-password", generatePassword());

    try (KubernetesClient client = kubClientFactory.retrieveKubernetesClient()) {
      Secret secret = new Secret();
      if (!exists(client, name)) {
        secret = new SecretBuilder()
            .withNewMetadata()
            .withName(name)
            .withLabels(labels)
            .endMetadata()
            .withType("Opaque")
            .withData(data)
            .build();

        client.secrets().inNamespace(namespace).create(secret);
      }

      SecretList list = client.secrets().inNamespace(namespace).list();
      for (Secret item : list.getItems()) {
        if (item.getMetadata().getName().equals(name)) {
          secret = item;
        }
      }

      LOGGER.debug("Creating Secret: {}", name);
      return secret;
    }
  }

  private boolean exists(@NonNull KubernetesClient client, @NonNull String secretName) {
    return ResourceUtils.exists(client.secrets().inNamespace(namespace).list().getItems(),
        secretName);
  }

  private static String generatePassword() {
    return base64(UUID.randomUUID().toString().substring(4, 20).getBytes(StandardCharsets.UTF_8));
  }

  private static String base64(byte[] bytes) {
    return Base64.getEncoder().encodeToString(bytes);
  }

  /**
   * Delete resource.
   */
  public Secret delete(StackGresCluster resource) {
    try (KubernetesClient client = kubClientFactory.retrieveKubernetesClient()) {
      return delete(client, resource);
    }
  }

  /**
   * Delete resource.
   */
  public Secret delete(KubernetesClient client, StackGresCluster resource) {
    final String name = resource.getMetadata().getName();

    Secret secret = client.secrets().inNamespace(namespace).withName(name).get();
    if (secret != null) {
      client.secrets().inNamespace(namespace).withName(name).delete();
      LOGGER.debug("Deleting Secret: {}", name);
    }

    return secret;
  }

}
