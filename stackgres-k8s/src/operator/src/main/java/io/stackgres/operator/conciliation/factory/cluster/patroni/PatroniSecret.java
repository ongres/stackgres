/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.SecretVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.StackGresVolume;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.common.patroni.StackGresPasswordKeys;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.ImmutableVolumePair;
import io.stackgres.operator.conciliation.factory.VolumeFactory;
import io.stackgres.operator.conciliation.factory.VolumePair;
import io.stackgres.operatorframework.resource.ResourceUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.jooq.impl.DSL;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.jooq.lambda.tuple.Tuple3;

@Singleton
@OperatorVersionBinder
public class PatroniSecret
    implements VolumeFactory<StackGresClusterContext>, StackGresPasswordKeys {

  private LabelFactoryForCluster labelFactory;

  public static String name(StackGresClusterContext clusterContext) {
    return PatroniUtil.secretName(clusterContext.getSource().getMetadata().getName());
  }

  public static String name(StackGresCluster cluster) {
    return PatroniUtil.secretName(cluster.getMetadata().getName());
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
        .withDefaultMode(0400)
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
    final Map<String, String> labels = labelFactory.genericLabels(cluster);

    final Map<String, String> previousSecretData = context.getDatabaseSecret()
        .map(Secret::getData)
        .map(ResourceUtil::decodeSecret)
        .orElse(Map.of());

    final Map<String, String> data = new HashMap<>();

    data.put(ROLES_UPDATE_SQL_KEY, "SET log_statement TO 'none';");

    setSuperuserCredentials(context, previousSecretData, data);

    setReplicationCredentials(context, previousSecretData, data);

    setAuthenticatorCredentials(context, previousSecretData, data);

    if (getPostgresFlavorComponent(context.getSource()) == StackGresComponent.BABELFISH) {
      setBabelfishCredentials(context, previousSecretData, data);
    }

    setPgBouncerCredentials(context, previousSecretData, data);

    setRestApiCredentials(context, previousSecretData, data);

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

  private void setSuperuserCredentials(
      StackGresClusterContext context,
      Map<String, String> previousSecretData,
      Map<String, String> data) {
    var superuserCredentials = getSuperuserCredentials(context, previousSecretData);
    data.put(SUPERUSER_USERNAME_KEY, superuserCredentials.v1);
    data.put(SUPERUSER_USERNAME_ENV, superuserCredentials.v1);
    data.put(SUPERUSER_PASSWORD_KEY, superuserCredentials.v2);
    data.put(SUPERUSER_PASSWORD_ENV, superuserCredentials.v2);
    data.put(
        ROLES_UPDATE_SQL_KEY,
        Optional.ofNullable(data.get(ROLES_UPDATE_SQL_KEY)).orElse("") + "\n"
        + "ALTER ROLE " + superuserCredentials.v1 + " WITH PASSWORD '" + superuserCredentials.v2 + "';");
  }

  public static Tuple2<String, String> getSuperuserCredentials(
      StackGresClusterContext context) {
    final Map<String, String> previousSecretData = context.getDatabaseSecret()
        .map(Secret::getData)
        .map(ResourceUtil::decodeSecret)
        .orElse(Map.of());

    return getSuperuserCredentials(context, previousSecretData);
  }

  private static Tuple2<String, String> getSuperuserCredentials(
      StackGresClusterContext context,
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
      StackGresClusterContext context,
      Map<String, String> previousSecretData,
      Map<String, String> data) {
    var replicatorCredentials = getReplicatorCredentials(context, previousSecretData);
    data.put(REPLICATION_USERNAME_KEY, replicatorCredentials.v1);
    data.put(REPLICATION_USERNAME_ENV, replicatorCredentials.v1);
    data.put(REPLICATION_PASSWORD_KEY, replicatorCredentials.v2);
    data.put(REPLICATION_PASSWORD_ENV, replicatorCredentials.v2);
    data.put(
        ROLES_UPDATE_SQL_KEY,
        Optional.ofNullable(data.get(ROLES_UPDATE_SQL_KEY)).orElse("") + "\n"
        + "ALTER ROLE " + replicatorCredentials.v1 + " WITH PASSWORD '" + replicatorCredentials.v2 + "';");
  }

  public static Tuple2<String, String> getReplicatorCredentials(
      StackGresClusterContext context) {
    final Map<String, String> previousSecretData = context.getDatabaseSecret()
        .map(Secret::getData)
        .map(ResourceUtil::decodeSecret)
        .orElse(Map.of());

    return getReplicatorCredentials(context, previousSecretData);
  }

  private static Tuple2<String, String> getReplicatorCredentials(
      StackGresClusterContext context,
      Map<String, String> previousSecretData) {
    return Tuple.tuple(
        context.getReplicationUsername()
        .orElse(previousSecretData
            .getOrDefault(REPLICATION_USERNAME_KEY, previousSecretData
                .getOrDefault(REPLICATION_USERNAME_ENV, REPLICATION_USERNAME))),
        context.getReplicationPassword()
        .orElse(previousSecretData
            .getOrDefault(REPLICATION_PASSWORD_KEY, previousSecretData
                .getOrDefault(REPLICATION_PASSWORD_ENV,
                  context.getGeneratedReplicationPassword()))));
  }

  private void setAuthenticatorCredentials(
      StackGresClusterContext context,
      Map<String, String> previousSecretData,
      Map<String, String> data) {
    var authenticatorCredentials = getAuthenticatorCredentials(context, previousSecretData);
    data.put(AUTHENTICATOR_USERNAME_KEY, authenticatorCredentials.v1);
    data.put(AUTHENTICATOR_USERNAME_ENV, authenticatorCredentials.v1);
    data.put(AUTHENTICATOR_PASSWORD_KEY, authenticatorCredentials.v2);
    data.put(authenticatorCredentials.v3, context.getAuthenticatorPassword()
        .orElse(data.get(AUTHENTICATOR_PASSWORD_KEY)));
    final String authenticatorOptionsEnv = AUTHENTICATOR_OPTIONS_ENV
        .replace(AUTHENTICATOR_USERNAME, authenticatorCredentials.v1);
    data.put(authenticatorOptionsEnv, "SUPERUSER");
    data.put(
        ROLES_UPDATE_SQL_KEY,
        Optional.ofNullable(data.get(ROLES_UPDATE_SQL_KEY)).orElse("") + "\n"
        + "DO $$\n"
        + "BEGIN\n"
        + "  IF NOT EXISTS (SELECT * FROM pg_roles WHERE rolname = "
        + DSL.inline(authenticatorCredentials.v1) + ") THEN\n"
        + "    CREATE USER " + DSL.quotedName(authenticatorCredentials.v1)
        + " WITH SUPERUSER PASSWORD " + DSL.inline(authenticatorCredentials.v2) + ";\n"
        + "  ELSE\n"
        + "    ALTER ROLE " + DSL.quotedName(authenticatorCredentials.v1)
        + " WITH SUPERUSER PASSWORD " + DSL.inline(authenticatorCredentials.v2) + ";\n"
        + "  END IF;\n"
        + "END$$;");
  }

  public static Tuple3<String, String, String> getAuthenticatorCredentials(
      StackGresClusterContext context) {
    final Map<String, String> previousSecretData = context.getDatabaseSecret()
        .map(Secret::getData)
        .map(ResourceUtil::decodeSecret)
        .orElse(Map.of());

    return getAuthenticatorCredentials(context, previousSecretData);
  }

  private static Tuple3<String, String, String> getAuthenticatorCredentials(
      StackGresClusterContext context,
      Map<String, String> previousSecretData) {
    final String authenticatorUsername = context.getAuthenticatorUsername()
        .orElse(previousSecretData
            .getOrDefault(AUTHENTICATOR_USERNAME_KEY, previousSecretData
                .getOrDefault(AUTHENTICATOR_USERNAME_ENV, AUTHENTICATOR_USERNAME)));
    final String authenticatorPasswordEnv = AUTHENTICATOR_PASSWORD_ENV
        .replace(AUTHENTICATOR_USERNAME, authenticatorUsername);
    final String authenticatorPassword = context.getAuthenticatorPassword()
        .orElse(previousSecretData
            .getOrDefault(AUTHENTICATOR_PASSWORD_KEY, previousSecretData
                .getOrDefault(authenticatorPasswordEnv,
                  context.getGeneratedAuthenticatorPassword())));
    return Tuple.tuple(
        authenticatorUsername,
        authenticatorPassword,
        authenticatorPasswordEnv);
  }

  private void setBabelfishCredentials(
      StackGresClusterContext context,
      final Map<String, String> previousSecretData,
      final Map<String, String> data) {
    final String babelfishPassword = previousSecretData
            .getOrDefault(BABELFISH_PASSWORD_KEY, context.getGeneratedBabelfishPassword());
    data.put(BABELFISH_PASSWORD_KEY, babelfishPassword);
    data.put(BABELFISH_CREATE_USER_SQL_KEY,
        "SET log_statement TO 'none';\n"
            + "DO $$\n"
            + "BEGIN\n"
            + "  IF NOT EXISTS (SELECT * FROM pg_roles WHERE rolname = " + DSL.inline(BABELFISH_USERNAME) + ") THEN\n"
            + "    CREATE USER " + DSL.quotedName(BABELFISH_USERNAME)
            + " WITH SUPERUSER PASSWORD " + DSL.inline(babelfishPassword) + ";\n"
            + "  ELSE\n"
            + "    ALTER ROLE " + DSL.quotedName(BABELFISH_USERNAME)
            + " WITH SUPERUSER PASSWORD " + DSL.inline(babelfishPassword) + ";\n"
            + "  END IF;\n"
            + "END$$;");
  }

  private void setPgBouncerCredentials(
      StackGresClusterContext context,
      final Map<String, String> previousSecretData,
      final Map<String, String> data) {
    var adminCredentials = getPgBouncerAdminCredentials(context, previousSecretData);
    var statsCredentials = getPgBouncerStatsCredentials(context, previousSecretData);
    data.put(PGBOUNCER_ADMIN_USERNAME_ENV, adminCredentials.v1);
    data.put(PGBOUNCER_ADMIN_PASSWORD_KEY, adminCredentials.v2);
    data.put(PGBOUNCER_STATS_USERNAME_ENV, statsCredentials.v1);
    data.put(PGBOUNCER_STATS_PASSWORD_KEY, statsCredentials.v2);
    final String pgbouncerAdminMd5 =
        StackGresUtil.getMd5Sum(
            adminCredentials.v2,
            adminCredentials.v1)
        .toLowerCase(Locale.US);
    final String pgbouncerStatsMd5 =
        StackGresUtil.getMd5Sum(
            statsCredentials.v2,
            statsCredentials.v1)
        .toLowerCase(Locale.US);
    data.put(
        PGBOUNCER_USERS_KEY,
        '"' + PGBOUNCER_ADMIN_USERNAME + '"' + " " + '"' + "md5" + pgbouncerAdminMd5 + '"' + '\n'
        + '"' + PGBOUNCER_STATS_USERNAME + '"' + " " + '"' + "md5" + pgbouncerStatsMd5 + '"' + '\n');
  }

  public static Tuple2<String, String> getPgBouncerAdminCredentials(
      StackGresClusterContext context) {
    final Map<String, String> previousSecretData = context.getDatabaseSecret()
        .map(Secret::getData)
        .map(ResourceUtil::decodeSecret)
        .orElse(Map.of());

    return getPgBouncerAdminCredentials(context, previousSecretData);
  }

  private static Tuple2<String, String> getPgBouncerAdminCredentials(StackGresClusterContext context,
      Map<String, String> previousSecretData) {
    return Tuple.tuple(
        previousSecretData
        .getOrDefault(PGBOUNCER_ADMIN_USERNAME_ENV, PGBOUNCER_ADMIN_USERNAME),
        previousSecretData
        .getOrDefault(PGBOUNCER_ADMIN_PASSWORD_KEY, context.getGeneratedPgBouncerAdminPassword()));
  }

  public static Tuple2<String, String> getPgBouncerStatsCredentials(
      StackGresClusterContext context) {
    final Map<String, String> previousSecretData = context.getDatabaseSecret()
        .map(Secret::getData)
        .map(ResourceUtil::decodeSecret)
        .orElse(Map.of());

    return getPgBouncerStatsCredentials(context, previousSecretData);
  }

  private static Tuple2<String, String> getPgBouncerStatsCredentials(StackGresClusterContext context,
      Map<String, String> previousSecretData) {
    return Tuple.tuple(
        previousSecretData
        .getOrDefault(PGBOUNCER_STATS_USERNAME_ENV, PGBOUNCER_STATS_USERNAME),
        previousSecretData
        .getOrDefault(PGBOUNCER_STATS_PASSWORD_KEY, context.getGeneratedPgBouncerStatsPassword()));
  }

  private void setRestApiCredentials(
      StackGresClusterContext context,
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
  public void setFactoryFactory(LabelFactoryForCluster labelFactory) {
    this.labelFactory = labelFactory;
  }
}
