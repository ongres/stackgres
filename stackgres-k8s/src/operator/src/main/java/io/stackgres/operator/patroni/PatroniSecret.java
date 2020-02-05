/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.patroni;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;

import com.google.common.collect.ImmutableList;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.resource.ResourceUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class PatroniSecret {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(PatroniSecret.class);

  public static String restoreCopiedSecretName(StackGresClusterContext context, String name) {
    return context.getCluster().getMetadata().getName() + "-restore-" + name;
  }

  /**
   * Create the Secret for patroni associated to the cluster.
   */
  public List<Secret> create(StackGresClusterContext context) {
    final String name = context.getCluster().getMetadata().getName();
    final String namespace = context.getCluster().getMetadata().getNamespace();
    final Map<String, String> labels = ResourceUtil.clusterLabels(context.getCluster());

    Map<String, String> data = new HashMap<>();
    data.put("superuser-password", generatePassword());
    data.put("replication-password", generatePassword());
    data.put("authenticator-password", generatePassword());

    ImmutableList.Builder<Secret> secrets = ImmutableList.<Secret>builder().add(new SecretBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(name)
        .withLabels(labels)
        .withOwnerReferences(ImmutableList.of(
            ResourceUtil.getOwnerReference(context.getCluster())))
        .endMetadata()
        .withType("Opaque")
        .withData(data)
        .build());

    context.getRestoreConfigSource().ifPresent(restoreConfigSource -> {
      if (restoreConfigSource.getRestore().isAutoCopySecretsEnabled()) {
        LOGGER.info("restore auto copy secrets enabled.  Copying secrets...");
        restoreConfigSource.getSecrets().entrySet()
            .stream()
            .forEach(secret -> secrets
            .add(new SecretBuilder()
                .withNewMetadata()
                .withNamespace(namespace)
                .withName(restoreCopiedSecretName(context, secret.getKey()))
                .withLabels(labels)
                .withOwnerReferences(ImmutableList.of(
                    ResourceUtil.getOwnerReference(context.getCluster())))
                .endMetadata()
                .withType("Opaque")
                .withData(secret.getValue())
                .build()
            ));
      }
    });

    return secrets.build();

  }

  private static String generatePassword() {
    return base64(UUID.randomUUID().toString().substring(4, 22).getBytes(StandardCharsets.UTF_8));
  }

  private static String base64(byte[] bytes) {
    return Base64.getEncoder().encodeToString(bytes);
  }

}
