/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.distributedlogs;

import static io.stackgres.common.patroni.StackGresPasswordKeys.SUPERUSER_PASSWORD_KEY;
import static io.stackgres.common.patroni.StackGresPasswordKeys.SUPERUSER_USERNAME;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.Secret;
import io.stackgres.apiweb.dto.cluster.ClusterDistributedLogs;
import io.stackgres.apiweb.dto.cluster.ClusterDto;
import io.stackgres.apiweb.dto.cluster.ClusterLogEntryDto;
import io.stackgres.apiweb.dto.cluster.ClusterSpec;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.FluentdUtil;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.postgres.PostgresConnectionManager;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceUtil;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Select;
import org.jooq.conf.ParamType;
import org.jooq.impl.DSL;
import org.jooq.lambda.Seq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class DistributedLogsFetcherImpl implements DistributedLogsFetcher {

  private static final Logger LOGGER = LoggerFactory.getLogger(DistributedLogsFetcherImpl.class);

  private final ResourceFinder<Secret> secretFinder;
  private final PostgresConnectionManager postgresConnectionManager;

  @Inject
  public DistributedLogsFetcherImpl(ResourceFinder<Secret> secretFinder,
      PostgresConnectionManager postgresConnectionManager) {
    super();
    this.secretFinder = secretFinder;
    this.postgresConnectionManager = postgresConnectionManager;
  }

  @Override
  public List<ClusterLogEntryDto> logs(DistributedLogsQueryParameters parameters) {
    try (Connection connection = getConnection(parameters.getCluster());
        DSLContext context = DSL.using(connection)) {
      connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
      connection.setReadOnly(true);
      connection.setAutoCommit(true);
      try (PreparedStatement statement = connection.prepareStatement("SET TIME ZONE 'UTC'")) {
        statement.execute();
      }
      Select<Record> query = new DistributedLogsQueryGenerator(context, parameters)
          .generateQuery();
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("Query for cluster logs {}.{} with params"
            + " (records: {}, from: {}, to: {}, filters: {}, asc: {}, text: {}): {}",
            parameters.getCluster().getMetadata().getNamespace(),
            parameters.getCluster().getMetadata().getName(),
            parameters.getRecords(), parameters.getFromTimeAndIndex(),
            parameters.getToTimeAndIndex(),
            parameters.getFilters(),
            parameters.isSortAsc(),
            parameters.getFullTextSearchQuery(),
            query.getSQL(ParamType.INLINED));
        LOGGER.trace("Explain query for cluster logs {}.{} with params"
            + " (records: {}, from: {}, to: {}, filters: {}, asc: {}, text: {}): {}",
            parameters.getCluster().getMetadata().getNamespace(),
            parameters.getCluster().getMetadata().getName(),
            parameters.getRecords(),
            parameters.getFromTimeAndIndex(),
            parameters.getToTimeAndIndex(),
            parameters.getFilters(),
            parameters.isSortAsc(),
            parameters.getFullTextSearchQuery(),
            context.explain(query).toString().replace("\n", "\t"));
      }
      return Seq.seq(query.fetch())
          .map(record -> record.into(MappedClusterLogEntryDto.class))
          .collect(ImmutableList.toImmutableList());
    } catch (SQLException ex) {
      final String databaseName = FluentdUtil.databaseName(
          parameters.getCluster().getMetadata().getNamespace(),
          parameters.getCluster().getMetadata().getName());
      if (Objects.equals(ex.getMessage(),
          "FATAL: database \"" + databaseName + "\" does not exist")) {
        return ImmutableList.of();
      }
      throw new RuntimeException(ex);
    }
  }

  private Connection getConnection(ClusterDto cluster) throws SQLException {
    final String distributedLogs = Optional.ofNullable(cluster.getSpec())
        .map(ClusterSpec::getDistributedLogs)
        .map(ClusterDistributedLogs::getDistributedLogs)
        .orElseThrow(() -> new IllegalArgumentException(
            "Distributed logs are not configured for this cluster"));
    String namespace = StackGresUtil.getNamespaceFromRelativeId(
        distributedLogs, cluster.getMetadata().getNamespace());
    String name = StackGresUtil.getNameFromRelativeId(distributedLogs);
    String serviceName = PatroniUtil.defaultReadWriteName(name);
    Secret secret = secretFinder.findByNameAndNamespace(name, namespace)
        .orElseThrow(() -> new NotFoundException(
            "Secret with username and password for user postgres can not be found."));
    return postgresConnectionManager.getConnection(
        serviceName + "." + namespace, EnvoyUtil.PG_PORT,
        FluentdUtil.databaseName(
            cluster.getMetadata().getNamespace(),
            cluster.getMetadata().getName()),
        SUPERUSER_USERNAME,
        ResourceUtil.decodeSecret(secret.getData()
            .get(SUPERUSER_PASSWORD_KEY)));
  }
}
