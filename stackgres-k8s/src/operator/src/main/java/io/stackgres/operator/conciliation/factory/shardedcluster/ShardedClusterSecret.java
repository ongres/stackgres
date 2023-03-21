/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardedcluster;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.labels.LabelFactoryForShardedCluster;
import io.stackgres.common.patroni.StackGresPasswordKeys;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jetbrains.annotations.NotNull;

@Singleton
@OperatorVersionBinder
public class ShardedClusterSecret
    implements ResourceGenerator<StackGresShardedClusterContext>, StackGresPasswordKeys {

  private LabelFactoryForShardedCluster factoryFactory;

  public static String name(StackGresShardedClusterContext clusterContext) {
    return ResourceUtil.resourceName(clusterContext.getSource().getMetadata().getName());
  }

  public static String name(StackGresShardedCluster cluster) {
    return name(cluster.getMetadata().getName());
  }

  public static String name(String clusterName) {
    return ResourceUtil.resourceName(clusterName);
  }

  private static String generatePassword() {
    return UUID.randomUUID().toString().substring(4, 22);
  }

  /**
   * Create the Secret for patroni associated to the cluster.
   */
  @Override
  public @NotNull Stream<HasMetadata> generateResource(StackGresShardedClusterContext context) {
    final StackGresShardedCluster cluster = context.getSource();
    final String name = cluster.getMetadata().getName();
    final String namespace = cluster.getMetadata().getNamespace();
    final Map<String, String> labels = factoryFactory.genericLabels(cluster);

    final Map<String, String> previousSecretData = context.getDatabaseSecret()
        .map(Secret::getData)
        .map(ResourceUtil::decodeSecret)
        .orElse(Map.of());

    final Map<String, String> data = new HashMap<>();

    setSuperuserCredentials(context, previousSecretData, data);

    setReplicationCredentials(context, previousSecretData, data);

    setAuthenticatorCredentials(context, previousSecretData, data);

    setRestApiCredentials(context, previousSecretData, data);

    return Stream.of(new SecretBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(name)
        .withLabels(labels)
        .endMetadata()
        .withType("Opaque")
        .withData(ResourceUtil.encodeSecret(StackGresUtil.addMd5Sum(data)))
        .build());
  }

  private void setSuperuserCredentials(StackGresShardedClusterContext context,
      Map<String, String> previousSecretData, Map<String, String> data) {
    data.put(SUPERUSER_USERNAME_ENV, context.getSuperuserUsername()
        .orElse(previousSecretData
            .getOrDefault(SUPERUSER_USERNAME_ENV, SUPERUSER_USERNAME)));
    data.put(SUPERUSER_PASSWORD_KEY, context.getSuperuserPassword()
        .orElse(previousSecretData
            .getOrDefault(SUPERUSER_PASSWORD_KEY, previousSecretData
                .getOrDefault(SUPERUSER_PASSWORD_ENV, generatePassword()))));
    data.put(SUPERUSER_PASSWORD_ENV, context.getSuperuserPassword()
        .orElse(data.get(SUPERUSER_PASSWORD_KEY)));
  }

  private void setReplicationCredentials(StackGresShardedClusterContext context,
      Map<String, String> previousSecretData, Map<String, String> data) {
    data.put(REPLICATION_USERNAME_ENV, context.getReplicationUsername()
        .orElse(previousSecretData
            .getOrDefault(REPLICATION_USERNAME_ENV, REPLICATION_USERNAME)));
    data.put(REPLICATION_PASSWORD_KEY, context.getReplicationPassword()
        .orElse(previousSecretData
            .getOrDefault(REPLICATION_PASSWORD_KEY, previousSecretData
                .getOrDefault(REPLICATION_PASSWORD_ENV, generatePassword()))));
    data.put(REPLICATION_PASSWORD_ENV, context.getReplicationPassword()
        .orElse(data.get(REPLICATION_PASSWORD_KEY)));
  }

  private void setAuthenticatorCredentials(StackGresShardedClusterContext context,
      Map<String, String> previousSecretData, Map<String, String> data) {
    data.put(AUTHENTICATOR_USERNAME_ENV, context.getAuthenticatorUsername()
        .orElse(previousSecretData
            .getOrDefault(AUTHENTICATOR_USERNAME_ENV, AUTHENTICATOR_USERNAME)));
    final String authenticatorPasswordEnv = AUTHENTICATOR_PASSWORD_ENV
        .replace(AUTHENTICATOR_USERNAME, data.get(AUTHENTICATOR_USERNAME_ENV));
    final String authenticatorOptionsEnv = AUTHENTICATOR_OPTIONS_ENV
        .replace(AUTHENTICATOR_USERNAME, data.get(AUTHENTICATOR_USERNAME_ENV));
    data.put(AUTHENTICATOR_PASSWORD_KEY, context.getAuthenticatorPassword()
        .orElse(previousSecretData
            .getOrDefault(AUTHENTICATOR_PASSWORD_KEY, previousSecretData
                .getOrDefault(authenticatorPasswordEnv, generatePassword()))));
    data.put(authenticatorPasswordEnv, context.getAuthenticatorPassword()
        .orElse(data.get(AUTHENTICATOR_PASSWORD_KEY)));
    data.put(authenticatorOptionsEnv, "superuser");
  }

  private void setRestApiCredentials(StackGresShardedClusterContext context,
      final Map<String, String> previousSecretData,
      final Map<String, String> data) {
    data.put(RESTAPI_USERNAME_ENV, RESTAPI_USERNAME);
    data.put(RESTAPI_PASSWORD_KEY, context.getPatroniRestApiPassword()
        .orElse(previousSecretData
            .getOrDefault(RESTAPI_PASSWORD_KEY, previousSecretData
                .getOrDefault(RESTAPI_PASSWORD_ENV, generatePassword()))));
    data.put(RESTAPI_PASSWORD_ENV, data.get(RESTAPI_PASSWORD_KEY));
  }

  @Inject
  public void setFactoryFactory(LabelFactoryForShardedCluster factoryFactory) {
    this.factoryFactory = factoryFactory;
  }

}
