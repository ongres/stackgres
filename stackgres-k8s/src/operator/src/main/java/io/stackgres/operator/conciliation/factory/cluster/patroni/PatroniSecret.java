/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.SecretVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.stackgres.common.LabelFactoryForCluster;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.StackGresVolume;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.patroni.StackGresPasswordKeys;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.ImmutableVolumePair;
import io.stackgres.operator.conciliation.factory.VolumeFactory;
import io.stackgres.operator.conciliation.factory.VolumePair;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jetbrains.annotations.NotNull;

@Singleton
@OperatorVersionBinder
public class PatroniSecret
    implements VolumeFactory<StackGresClusterContext>, StackGresPasswordKeys {

  private LabelFactoryForCluster<StackGresCluster> factoryFactory;

  public static String name(StackGresClusterContext clusterContext) {
    return ResourceUtil.resourceName(clusterContext.getSource().getMetadata().getName());
  }

  public static String name(StackGresCluster cluster) {
    return name(cluster.getMetadata().getName());
  }

  public static String name(String clusterName) {
    return ResourceUtil.resourceName(clusterName);
  }

  private static String generatePassword() {
    return UUID.randomUUID().toString().substring(4, 22);
  }

  @Override
  public @NotNull Stream<VolumePair> buildVolumes(StackGresClusterContext context) {
    return Stream.of(
        ImmutableVolumePair.builder()
            .volume(buildVolume(context))
            .source(buildSource(context))
            .build()
    );
  }

  public @NotNull Volume buildVolume(StackGresClusterContext context) {
    return new VolumeBuilder()
        .withName(StackGresVolume.PATRONI_CREDENTIALS.getName())
        .withSecret(new SecretVolumeSourceBuilder()
            .withSecretName(name(context))
            .withDefaultMode(400)
            .build())
        .build();
  }

  /**
   * Create the Secret for patroni associated to the cluster.
   */
  public @NotNull Secret buildSource(StackGresClusterContext context) {
    final StackGresCluster cluster = context.getSource();
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

    if (getPostgresFlavorComponent(context.getSource()) == StackGresComponent.BABELFISH) {
      setBabelfishCredentials(previousSecretData, data);
    }

    setPgBouncerCredentials(previousSecretData, data);

    setRestApiCredentials(previousSecretData, data);

    return new SecretBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(name)
        .withLabels(labels)
        .endMetadata()
        .withType("Opaque")
        .withData(ResourceUtil.encodeSecret(StackGresUtil.addMd5Sum(data)))
        .build();
  }

  private void setSuperuserCredentials(StackGresClusterContext context,
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

  private void setReplicationCredentials(StackGresClusterContext context,
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

  private void setAuthenticatorCredentials(StackGresClusterContext context,
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

  private void setBabelfishCredentials(final Map<String, String> previousSecretData,
      final Map<String, String> data) {
    data.put(BABELFISH_PASSWORD_KEY, previousSecretData
        .getOrDefault(BABELFISH_PASSWORD_KEY, generatePassword()));
    data.put(BABELFISH_CREATE_USER_SQL_KEY,
        "SET log_statement TO 'none';\n"
            + "DROP ROLE IF EXISTS " + BABELFISH_USERNAME + ";\n"
            + "CREATE USER " + BABELFISH_USERNAME + " SUPERUSER"
            + " PASSWORD '" + data.get(BABELFISH_PASSWORD_KEY) + "';");
  }

  private void setPgBouncerCredentials(final Map<String, String> previousSecretData,
      final Map<String, String> data) {
    data.put(PGBOUNCER_ADMIN_USERNAME_ENV, PGBOUNCER_ADMIN_USERNAME);
    data.put(PGBOUNCER_ADMIN_PASSWORD_KEY, previousSecretData
        .getOrDefault(PGBOUNCER_ADMIN_PASSWORD_KEY, generatePassword()));
    data.put(PGBOUNCER_STATS_USERNAME_ENV, PGBOUNCER_STATS_USERNAME);
    data.put(PGBOUNCER_STATS_PASSWORD_KEY, previousSecretData
        .getOrDefault(PGBOUNCER_STATS_PASSWORD_KEY, generatePassword()));
  }

  private void setRestApiCredentials(final Map<String, String> previousSecretData,
      final Map<String, String> data) {
    data.put(RESTAPI_USERNAME_ENV, RESTAPI_USERNAME);
    data.put(RESTAPI_PASSWORD_KEY, previousSecretData
        .getOrDefault(RESTAPI_PASSWORD_KEY, previousSecretData
            .getOrDefault(RESTAPI_PASSWORD_ENV, generatePassword())));
    data.put(RESTAPI_PASSWORD_ENV, data.get(RESTAPI_PASSWORD_KEY));
  }

  @Inject
  public void setFactoryFactory(LabelFactoryForCluster<StackGresCluster> factoryFactory) {
    this.factoryFactory = factoryFactory;
  }

}
