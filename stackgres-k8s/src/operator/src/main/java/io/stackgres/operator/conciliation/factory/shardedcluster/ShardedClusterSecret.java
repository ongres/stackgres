/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardedcluster;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.labels.LabelFactoryForShardedCluster;
import io.stackgres.common.patroni.StackGresPasswordKeys;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext;
import io.stackgres.operatorframework.resource.ResourceUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

@Singleton
@OperatorVersionBinder
public class ShardedClusterSecret
    implements ResourceGenerator<StackGresShardedClusterContext>, StackGresPasswordKeys {

  private LabelFactoryForShardedCluster labelFactory;

  public static String name(StackGresShardedClusterContext clusterContext) {
    return ResourceUtil.resourceName(clusterContext.getSource().getMetadata().getName());
  }

  public static String name(StackGresShardedCluster cluster) {
    return name(cluster.getMetadata().getName());
  }

  public static String name(String clusterName) {
    return ResourceUtil.resourceName(clusterName);
  }

  /**
   * Create the Secret for patroni associated to the cluster.
   */
  @Override
  public @NotNull Stream<HasMetadata> generateResource(StackGresShardedClusterContext context) {
    final StackGresShardedCluster cluster = context.getSource();
    final String name = cluster.getMetadata().getName();
    final String namespace = cluster.getMetadata().getNamespace();
    final Map<String, String> labels = labelFactory.genericLabels(cluster);

    final Map<String, String> previousSecretData = context.getDatabaseSecret()
        .map(Secret::getData)
        .map(ResourceUtil::decodeSecret)
        .orElse(Map.of());

    final Map<String, String> data = new HashMap<>();

    setSuperuserCredentials(context, previousSecretData, data);

    setReplicationCredentials(context, previousSecretData, data);

    setAuthenticatorCredentials(context, previousSecretData, data);

    if (getPostgresFlavorComponent(context.getSource()) == StackGresComponent.BABELFISH) {
      setBabelfishCredentials(context, previousSecretData, data);
    }

    setPgBouncerCredentials(context, previousSecretData, data);

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

  private void setSuperuserCredentials(
      StackGresShardedClusterContext context,
      Map<String, String> previousSecretData,
      Map<String, String> data) {
    var superuserCredentials = getSuperuserCredentials(context, previousSecretData);
    data.put(SUPERUSER_USERNAME_KEY, superuserCredentials.v1);
    data.put(SUPERUSER_USERNAME_ENV, superuserCredentials.v1);
    data.put(SUPERUSER_PASSWORD_KEY, superuserCredentials.v2);
    data.put(SUPERUSER_PASSWORD_ENV, superuserCredentials.v2);
  }

  public static Tuple2<String, String> getSuperuserCredentials(
      StackGresShardedClusterContext context) {
    final Map<String, String> previousSecretData = context.getDatabaseSecret()
        .map(Secret::getData)
        .map(ResourceUtil::decodeSecret)
        .orElse(Map.of());

    return getSuperuserCredentials(context, previousSecretData);
  }

  private static Tuple2<String, String> getSuperuserCredentials(
      StackGresShardedClusterContext context,
      Map<String, String> previousSecretData) {
    return Tuple.tuple(
        context.getSuperuserUsername()
        .orElse(previousSecretData
            .getOrDefault(SUPERUSER_USERNAME_KEY, previousSecretData
                .getOrDefault(SUPERUSER_USERNAME_ENV, SUPERUSER_USERNAME))),
        context.getSuperuserPassword()
        .orElse(previousSecretData
            .getOrDefault(SUPERUSER_PASSWORD_KEY, previousSecretData
                .getOrDefault(SUPERUSER_PASSWORD_ENV, context.getGeneratedSuperuserPassword()))));
  }

  private void setReplicationCredentials(
      StackGresShardedClusterContext context,
      Map<String, String> previousSecretData,
      Map<String, String> data) {
    data.put(REPLICATION_USERNAME_KEY, context.getReplicationUsername()
        .orElse(previousSecretData
            .getOrDefault(REPLICATION_USERNAME_KEY, previousSecretData
                .getOrDefault(REPLICATION_USERNAME_ENV, REPLICATION_USERNAME))));
    data.put(REPLICATION_USERNAME_ENV, data.get(REPLICATION_USERNAME_KEY));
    data.put(REPLICATION_PASSWORD_KEY, context.getReplicationPassword()
        .orElse(previousSecretData
            .getOrDefault(REPLICATION_PASSWORD_KEY, previousSecretData
                .getOrDefault(REPLICATION_PASSWORD_ENV,
                  context.getGeneratedReplicationPassword()))));
    data.put(REPLICATION_PASSWORD_ENV, context.getReplicationPassword()
        .orElse(data.get(REPLICATION_PASSWORD_KEY)));
  }

  private void setAuthenticatorCredentials(
      StackGresShardedClusterContext context,
      Map<String, String> previousSecretData,
      Map<String, String> data) {
    data.put(AUTHENTICATOR_USERNAME_KEY, context.getAuthenticatorUsername()
        .orElse(previousSecretData
            .getOrDefault(AUTHENTICATOR_USERNAME_KEY, previousSecretData
                .getOrDefault(AUTHENTICATOR_USERNAME_ENV, AUTHENTICATOR_USERNAME))));
    data.put(AUTHENTICATOR_USERNAME_ENV, data.get(AUTHENTICATOR_USERNAME_KEY));
    final String authenticatorPasswordEnv = AUTHENTICATOR_PASSWORD_ENV
        .replace(AUTHENTICATOR_USERNAME, data.get(AUTHENTICATOR_USERNAME_ENV));
    final String authenticatorOptionsEnv = AUTHENTICATOR_OPTIONS_ENV
        .replace(AUTHENTICATOR_USERNAME, data.get(AUTHENTICATOR_USERNAME_ENV));
    data.put(AUTHENTICATOR_PASSWORD_KEY, context.getAuthenticatorPassword()
        .orElse(previousSecretData
            .getOrDefault(AUTHENTICATOR_PASSWORD_KEY, previousSecretData
                .getOrDefault(authenticatorPasswordEnv,
                  context.getGeneratedAuthenticatorPassword()))));
    data.put(authenticatorPasswordEnv, context.getAuthenticatorPassword()
        .orElse(data.get(AUTHENTICATOR_PASSWORD_KEY)));
    data.put(authenticatorOptionsEnv, "superuser");
  }

  private void setBabelfishCredentials(
      StackGresShardedClusterContext context,
      final Map<String, String> previousSecretData,
      final Map<String, String> data) {
    data.put(BABELFISH_PASSWORD_KEY, previousSecretData
        .getOrDefault(BABELFISH_PASSWORD_KEY, context.getGeneratedBabelfishPassword()));
    data.put(BABELFISH_CREATE_USER_SQL_KEY,
        "SET log_statement TO 'none';\n"
            + "DROP ROLE IF EXISTS " + BABELFISH_USERNAME + ";\n"
            + "CREATE USER " + BABELFISH_USERNAME + " SUPERUSER"
            + " PASSWORD '" + data.get(BABELFISH_PASSWORD_KEY) + "';");
  }

  private void setPgBouncerCredentials(
      StackGresShardedClusterContext context,
      final Map<String, String> previousSecretData,
      final Map<String, String> data) {
    data.put(PGBOUNCER_ADMIN_USERNAME_ENV, PGBOUNCER_ADMIN_USERNAME);
    data.put(PGBOUNCER_ADMIN_PASSWORD_KEY, previousSecretData
        .getOrDefault(PGBOUNCER_ADMIN_PASSWORD_KEY, context.getGeneratedPgBouncerAdminPassword()));
    data.put(PGBOUNCER_STATS_USERNAME_ENV, PGBOUNCER_STATS_USERNAME);
    data.put(PGBOUNCER_STATS_PASSWORD_KEY, previousSecretData
        .getOrDefault(PGBOUNCER_STATS_PASSWORD_KEY, context.getGeneratedPgBouncerStatsPassword()));
  }

  private void setRestApiCredentials(
      StackGresShardedClusterContext context,
      final Map<String, String> previousSecretData,
      final Map<String, String> data) {
    data.put(RESTAPI_USERNAME_KEY, RESTAPI_USERNAME);
    data.put(RESTAPI_USERNAME_ENV, data.get(RESTAPI_USERNAME_KEY));
    data.put(RESTAPI_PASSWORD_KEY, context.getPatroniRestApiPassword()
        .orElse(previousSecretData
            .getOrDefault(RESTAPI_PASSWORD_KEY, previousSecretData
                .getOrDefault(RESTAPI_PASSWORD_ENV,
                  context.getGeneratedPatroniRestApiPassword()))));
    data.put(RESTAPI_PASSWORD_ENV, data.get(RESTAPI_PASSWORD_KEY));
  }

  @Inject
  public void setFactoryFactory(LabelFactoryForShardedCluster labelFactory) {
    this.labelFactory = labelFactory;
  }

}
