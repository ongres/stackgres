/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.common;

import static io.stackgres.common.patroni.StackGresPasswordKeys.SUPERUSER_DATABASE;
import static io.stackgres.common.patroni.StackGresPasswordKeys.SUPERUSER_PASSWORD_KEY;
import static io.stackgres.common.patroni.StackGresPasswordKeys.SUPERUSER_PREVIOUS_USERNAME_KEY;
import static io.stackgres.common.patroni.StackGresPasswordKeys.SUPERUSER_USERNAME_KEY;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.Secret;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.ClusterPath;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.postgres.PostgresConnectionManager;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operatorframework.resource.ResourceUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jooq.lambda.Seq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The credentials of a superuser that exists, to connect to Postgres from the Pod. When the
 * username of the superuser changes the user with the new username is created by the operator
 * connecting as a superuser that exists: the one that Patroni uses, that is updated only once the
 * new one works, the one of the Secret or the previous one of the Secret, in this order.
 */
@Singleton
public class ClusterSuperuserCredentials {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClusterSuperuserCredentials.class);

  private static final Path PATRONI_CONFIG_PATH =
      Path.of(ClusterPath.PATRONI_CONFIG_FILE_PATH.path());

  private static final String INVALID_AUTHORIZATION_SPECIFICATION_SQL_STATE = "28000";

  private final ResourceFinder<Secret> secretFinder;
  private final PostgresConnectionManager postgresConnectionManager;
  private final PatroniConfigCredentials patroniConfigCredentials;

  @Inject
  public ClusterSuperuserCredentials(
      ResourceFinder<Secret> secretFinder,
      PostgresConnectionManager postgresConnectionManager,
      PatroniConfigCredentials patroniConfigCredentials) {
    this.secretFinder = secretFinder;
    this.postgresConnectionManager = postgresConnectionManager;
    this.patroniConfigCredentials = patroniConfigCredentials;
  }

  public PostgresUtil.Credentials get(ClusterContext context) {
    return get(context, readPatroniConfigLines());
  }

  PostgresUtil.Credentials get(ClusterContext context, List<String> patroniConfigLines) {
    final Map<String, String> secretData = secretFinder.findByNameAndNamespace(
        context.getCluster().getMetadata().getName(),
        context.getCluster().getMetadata().getNamespace())
        .map(Secret::getData)
        .map(ResourceUtil::decodeSecret)
        .orElseThrow(() -> new RuntimeException("Can not find secret "
            + context.getCluster().getMetadata().getName()));
    final String username = Optional.ofNullable(secretData.get(SUPERUSER_USERNAME_KEY))
        .orElseThrow(() -> new RuntimeException("Can not find key "
            + SUPERUSER_USERNAME_KEY + " in secret "
            + context.getCluster().getMetadata().getName()));
    final String password = secretData.get(SUPERUSER_PASSWORD_KEY);
    final List<PostgresUtil.Credentials> candidates = Seq.seq(
        patroniConfigCredentials.getSuperuser(patroniConfigLines))
        .append(new PostgresUtil.Credentials(username, password))
        .append(Seq.seq(Optional.ofNullable(secretData.get(SUPERUSER_PREVIOUS_USERNAME_KEY)))
            .map(previousUsername -> new PostgresUtil.Credentials(previousUsername, password)))
        .distinct(PostgresUtil.Credentials::username)
        .toList();
    if (candidates.size() == 1) {
      return candidates.getFirst();
    }
    return Seq.seq(candidates)
        .filter(this::exists)
        .findFirst()
        .orElse(candidates.getFirst());
  }

  private boolean exists(PostgresUtil.Credentials credentials) {
    try (Connection connection = postgresConnectionManager.getUnixConnection(
        ClusterPath.PG_RUN_PATH.path(), EnvoyUtil.PG_PORT,
        SUPERUSER_DATABASE,
        credentials.username(),
        "")) {
      return true;
    } catch (SQLException ex) {
      if (INVALID_AUTHORIZATION_SPECIFICATION_SQL_STATE.equals(ex.getSQLState())) {
        LOGGER.debug("Superuser {} does not exist: {}", credentials.username(), ex.getMessage());
        return false;
      }
      // Postgres may not be available, any superuser is as good as the others
      return true;
    }
  }

  private List<String> readPatroniConfigLines() {
    if (!Files.exists(PATRONI_CONFIG_PATH)) {
      return List.of();
    }
    try {
      return Files.readAllLines(PATRONI_CONFIG_PATH);
    } catch (IOException ex) {
      LOGGER.warn("Can not read Patroni config file {}: {}", PATRONI_CONFIG_PATH, ex.getMessage());
      return List.of();
    }
  }

}
