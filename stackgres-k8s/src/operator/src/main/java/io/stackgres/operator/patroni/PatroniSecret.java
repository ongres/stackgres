/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.patroni;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.google.common.collect.ImmutableList;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.stackgres.operator.customresource.sgcluster.StackGresCluster;
import io.stackgres.operator.resource.ResourceUtil;

public class PatroniSecret {

  /**
   * Create the Secret for patroni associated to the cluster.
   */
  public static Secret create(StackGresCluster cluster) {
    final String name = cluster.getMetadata().getName();
    final String namespace = cluster.getMetadata().getNamespace();
    final Map<String, String> labels = ResourceUtil.defaultLabels(name);

    Map<String, String> data = new HashMap<>();
    data.put("superuser-password", generatePassword());
    data.put("replication-password", generatePassword());
    data.put("authenticator-password", generatePassword());

    return new SecretBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(name)
        .withLabels(labels)
        .withOwnerReferences(ImmutableList.of(ResourceUtil.getOwnerReference(cluster)))
        .endMetadata()
        .withType("Opaque")
        .withData(data)
        .build();
  }

  private static String generatePassword() {
    return base64(UUID.randomUUID().toString().substring(4, 22).getBytes(StandardCharsets.UTF_8));
  }

  private static String base64(byte[] bytes) {
    return Base64.getEncoder().encodeToString(bytes);
  }

}
