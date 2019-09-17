/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.patroni;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.common.collect.ImmutableMap;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.ResourceUtils;
import io.stackgres.common.sgcluster.StackGresCluster;
import io.stackgres.operator.app.KubernetesClientFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class SgSecrets {

  private static final Logger LOGGER = LoggerFactory.getLogger(SgSecrets.class);

  @Inject
  KubernetesClientFactory kubClientFactory;

  /**
   * Create the Service associated to the cluster.
   */
  public Secret create(StackGresCluster resource) {
    final String name = resource.getMetadata().getName();
    final String namespace = resource.getMetadata().getNamespace();

    Map<String, String> labels = ResourceUtils.defaultLabels(name);

    Map<String, String> data = new HashMap<>();
    data.put("superuser-password", generatePassword());
    data.put("replication-password", generatePassword());
    data.put("authenticator-password", generatePassword());

    try (KubernetesClient client = kubClientFactory.create()) {
      Secret secret = new Secret();
      if (!exists(client, name, namespace)) {
        secret = new SecretBuilder()
            .withNewMetadata()
            .withName(name)
            .withLabels(labels)
            .endMetadata()
            .withType("Opaque")
            .withData(data)
            .build();

        client.secrets().inNamespace(namespace).create(secret);
        LOGGER.trace("Secret: {}", new SecretBuilder(secret)
            .withData(data.entrySet().stream()
                .collect(ImmutableMap.toImmutableMap(
                    e -> e.getKey(), e -> "<obfuscated>")))
            .build());
      }

      Optional<Secret> created = client.secrets().inNamespace(namespace).list().getItems()
          .stream()
          .filter(p -> p.getMetadata().getName().equals(name))
          .findAny();

      LOGGER.debug("Creating Secret: {}", name);
      return created.orElse(secret);
    }
  }

  private boolean exists(KubernetesClient client, String secretName, String namespace) {
    return ResourceUtils.exists(client.secrets().inNamespace(namespace).list().getItems(),
        secretName);
  }

  private static String generatePassword() {
    return base64(UUID.randomUUID().toString().substring(4, 22).getBytes(StandardCharsets.UTF_8));
  }

  private static String base64(byte[] bytes) {
    return Base64.getEncoder().encodeToString(bytes);
  }

  /**
   * Delete resource.
   */
  public Secret delete(StackGresCluster resource) {
    try (KubernetesClient client = kubClientFactory.create()) {
      return delete(client, resource);
    }
  }

  /**
   * Delete resource.
   */
  public Secret delete(KubernetesClient client, StackGresCluster resource) {
    final String name = resource.getMetadata().getName();
    final String namespace = resource.getMetadata().getNamespace();

    Secret secret = client.secrets().inNamespace(namespace).withName(name).get();
    if (secret != null) {
      client.secrets().inNamespace(namespace).withName(name).delete();
      LOGGER.debug("Deleting Secret: {}", name);
    }

    return secret;
  }

}
