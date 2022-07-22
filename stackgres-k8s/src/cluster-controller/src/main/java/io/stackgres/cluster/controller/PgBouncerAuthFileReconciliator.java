/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.controller;

import static io.stackgres.common.patroni.StackGresRandomPasswordKeys.SUPERUSER_DATABASE;
import static io.stackgres.common.patroni.StackGresRandomPasswordKeys.SUPERUSER_PASSWORD_KEY;
import static io.stackgres.common.patroni.StackGresRandomPasswordKeys.SUPERUSER_USER_NAME;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.Secret;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.FileSystemHandler;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigPgBouncer;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigPgBouncerPgbouncerIni;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigSpec;
import io.stackgres.common.postgres.PostgresConnectionManager;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jooq.lambda.Seq;

public class PgBouncerAuthFileReconciliator {

  private static final Path ORIGINAL_AUTH_FILE_PATH =
      Paths.get(ClusterStatefulSetPath.PGBOUNCER_AUTH_FILE_PATH.path() + ".original");
  private static final Path AUTH_FILE_PATH =
      Paths.get(ClusterStatefulSetPath.PGBOUNCER_AUTH_FILE_PATH.path());
  private static final String SELECT_PGBOUNCER_USERS_FROM_PG_SHADOW =
      "SELECT '\"' || usename || '\" \"' || passwd || '\"'"
          + " FROM pg_shadow where usename = ANY (?)";

  private final CustomResourceFinder<StackGresPoolingConfig> poolingConfigFinder;
  private final ResourceFinder<Secret> secretFinder;
  private final PostgresConnectionManager postgresConnectionManager;
  private final FileSystemHandler fileSystemHandler;

  public PgBouncerAuthFileReconciliator(
      CustomResourceFinder<StackGresPoolingConfig> poolingConfigFinder,
      ResourceFinder<Secret> secretFinder,
      PostgresConnectionManager postgresConnectionManager, FileSystemHandler fileSystemHandler) {
    super();
    this.poolingConfigFinder = poolingConfigFinder;
    this.postgresConnectionManager = postgresConnectionManager;
    this.secretFinder = secretFinder;
    this.fileSystemHandler = fileSystemHandler;
  }

  public void updatePgbouncerUsersInAuthFile(ClusterContext context)
      throws IOException, SQLException {
    if (!fileSystemHandler.exists(ORIGINAL_AUTH_FILE_PATH)) {
      fileSystemHandler.copyOrReplace(AUTH_FILE_PATH, ORIGINAL_AUTH_FILE_PATH);
    }
    Collection<String> users = getPoolingConfigUserNames(context);
    String postgresPassword = getPostgresPassword(context);
    final String usersSection = extractAuthFileSectionForUsers(postgresPassword, users);
    try (
        InputStream originalInputStream = fileSystemHandler.newInputStream(
            ORIGINAL_AUTH_FILE_PATH);
        InputStream additionalInputStream = new ByteArrayInputStream(
            usersSection.getBytes(StandardCharsets.UTF_8));
        SequenceInputStream inputStream = new SequenceInputStream(
            originalInputStream, additionalInputStream)) {
      fileSystemHandler.copyOrReplace(inputStream, AUTH_FILE_PATH);
    }
  }

  private String getPostgresPassword(ClusterContext context) {
    Secret secret = secretFinder.findByNameAndNamespace(
        context.getCluster().getMetadata().getName(),
        context.getCluster().getMetadata().getNamespace())
        .orElseThrow(() -> new RuntimeException("Can not find secret "
            + context.getCluster().getMetadata().getName()));
    return Optional.of(secret).map(Secret::getData)
        .filter(data -> data.containsKey(SUPERUSER_PASSWORD_KEY))
        .map(data -> data.get(SUPERUSER_PASSWORD_KEY))
        .map(ResourceUtil::dencodeSecret)
        .orElseThrow(() -> new RuntimeException("Can not find key "
            + SUPERUSER_PASSWORD_KEY + " in secret "
            + context.getCluster().getMetadata().getName()));
  }

  private Collection<String> getPoolingConfigUserNames(ClusterContext context) {
    StackGresPoolingConfig poolingConfig = poolingConfigFinder.findByNameAndNamespace(
        context.getCluster().getSpec().getConfiguration().getConnectionPoolingConfig(),
        context.getCluster().getMetadata().getNamespace())
        .orElseThrow(() -> new RuntimeException("Can not find pool config "
            + context.getCluster().getSpec().getConfiguration().getConnectionPoolingConfig()));
    return Optional.of(poolingConfig)
        .map(StackGresPoolingConfig::getSpec)
        .map(StackGresPoolingConfigSpec::getPgBouncer)
        .map(StackGresPoolingConfigPgBouncer::getPgbouncerIni)
        .map(StackGresPoolingConfigPgBouncerPgbouncerIni::getUsers)
        .<Collection<String>>map(Map::keySet)
        .orElseGet(List::of);
  }

  private String extractAuthFileSectionForUsers(String postgresPassword, Collection<String> users)
      throws SQLException {
    List<String> authFileUsersLines = new ArrayList<>();
    try (Connection connection = postgresConnectionManager.getConnection(
        "localhost", EnvoyUtil.PG_PORT,
        SUPERUSER_DATABASE,
        SUPERUSER_USER_NAME,
        postgresPassword);
        PreparedStatement statement = connection.prepareStatement(
            SELECT_PGBOUNCER_USERS_FROM_PG_SHADOW)) {
      statement.setArray(1, connection.createArrayOf(
          "varchar", users.toArray(new String[0])));
      try (ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          authFileUsersLines.add(resultSet.getString(1));
        }
      }
    }
    return "\n"
        + Seq.seq(authFileUsersLines).toString("\n")
        + "\n";
  }

}
