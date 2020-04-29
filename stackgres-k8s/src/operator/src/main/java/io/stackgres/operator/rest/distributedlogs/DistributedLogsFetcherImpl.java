/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest.distributedlogs;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.Secret;
import io.stackgres.operator.common.ArcUtil;
import io.stackgres.operator.common.StackGresUtil;
import io.stackgres.operator.distributedlogs.fluentd.Fluentd;
import io.stackgres.operator.patroni.factory.PatroniServices;
import io.stackgres.operator.resource.ResourceFinder;
import io.stackgres.operator.rest.dto.cluster.ClusterDistributedLogs;
import io.stackgres.operator.rest.dto.cluster.ClusterDto;
import io.stackgres.operator.rest.dto.cluster.ClusterLogEntryDto;
import io.stackgres.operator.rest.dto.cluster.ClusterSpec;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.OrderField;
import org.jooq.Param;
import org.jooq.Record;
import org.jooq.Select;
import org.jooq.SelectConditionStep;
import org.jooq.conf.ParamType;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class DistributedLogsFetcherImpl implements DistributedLogsFetcher {

  public static final String LOG_POSTGRES_TABLE = "log_postgres";
  public static final String LOG_PATRONI_TABLE = "log_patroni";
  public static final String LOG_POSTGRES_WINDOW = "log_postgres_window";
  public static final String LOG_PATRONI_WINDOW = "log_patroni_window";
  public static final String LOG_POSTGRES_TSVECTOR_FUNCTION = "log_postgres_tsvector";
  public static final String LOG_PATRONI_TSVECTOR_FUNCTION = "log_patroni_tsvector";

  public static final Param<String> PATRONI_LOG_TYPE_VALUE = DSL.value("pa");
  public static final Param<String> POSTGRES_LOG_TYPE_VALUE = DSL.value("pg");
  public static final Param<String> PRIMARY_ROLE_VALUE = DSL.value("pr");
  public static final Param<String> REPLICA_ROLE_VALUE = DSL.value("re");

  public static final Field<OffsetDateTime> LOG_TIME_FIELD = DSL.field(
      "log_time", SQLDataType.TIMESTAMPWITHTIMEZONE);
  public static final Field<Integer> LOG_TIME_INDEX_FIELD = DSL.field(
      "log_time_index", SQLDataType.INTEGER);
  public static final Field<String> LOG_TYPE_FIELD = DSL.field(
      "log_type", SQLDataType.VARCHAR);
  public static final Field<String> MESSAGE_FIELD = DSL.field(
      "message", SQLDataType.VARCHAR);
  public static final Field<String> POD_NAME_FIELD = DSL.field("pod_name", SQLDataType.VARCHAR);
  public static final Field<String> ROLE_FIELD = DSL.field("role", SQLDataType.VARCHAR);
  public static final Field<String> PATRONI_LOG_TYPE_FIELD = PATRONI_LOG_TYPE_VALUE
      .as(LOG_TYPE_FIELD);
  public static final Field<String> POSTGRES_LOG_TYPE_FIELD = POSTGRES_LOG_TYPE_VALUE
      .as(LOG_TYPE_FIELD);
  public static final Field<String> MAPPED_ROLE_FIELD = DSL.case_(ROLE_FIELD)
      .when(StackGresUtil.PRIMARY_ROLE, PRIMARY_ROLE_VALUE)
      .when(StackGresUtil.REPLICA_ROLE, REPLICA_ROLE_VALUE)
      .else_(DSL.castNull(ROLE_FIELD))
      .as(ROLE_FIELD);

  public static final ImmutableMap<String, String> REVERSE_ROLE_MAP =
      ImmutableMap.of(
          PRIMARY_ROLE_VALUE.getValue(), StackGresUtil.PRIMARY_ROLE,
          REPLICA_ROLE_VALUE.getValue(), StackGresUtil.REPLICA_ROLE);

  public static final ImmutableMap<String, String> FILTER_CONVERSION_MAP =
      ImmutableMap.<String, String>builder()
      .put("logTime", "log_time")
      .put("logTimeIndex", "log_time_index")
      .put("logType", "log_type")
      .put("podName", "pod_name")
      .put("role", "role")
      .put("errorSeverity", "error_severity")
      .put("message", "message")
      .put("userName", "user_name")
      .put("databaseName", "database_name")
      .put("processId", "process_id")
      .put("connectionFrom", "connection_from")
      .put("sessionId", "session_id")
      .put("sessionLineNum", "session_line_num")
      .put("commandTag", "command_tag")
      .put("sessionStartTime", "session_start_time")
      .put("virtualTransactionId", "virtual_transaction_id")
      .put("transactionId", "transaction_id")
      .put("sqlStateCode", "sql_state_code")
      .put("detail", "detail")
      .put("hint", "hint")
      .put("internalQuery", "internal_query")
      .put("internalQueryPos", "internal_query_pos")
      .put("context", "context")
      .put("query", "query")
      .put("queryPos", "query_pos")
      .put("location", "location")
      .put("applicationName", "application_name")
      .build();

  public static final ImmutableList<Field<?>> POSTGRES_FIELDS = ImmutableList.of(
      LOG_TIME_FIELD,
      LOG_TIME_INDEX_FIELD,
      POD_NAME_FIELD,
      MAPPED_ROLE_FIELD,
      DSL.field("error_severity", SQLDataType.VARCHAR),
      MESSAGE_FIELD,
      DSL.field("user_name", SQLDataType.VARCHAR),
      DSL.field("database_name", SQLDataType.VARCHAR),
      DSL.field("process_id", SQLDataType.INTEGER),
      DSL.field("connection_from", SQLDataType.VARCHAR),
      DSL.field("session_id", SQLDataType.VARCHAR),
      DSL.field("session_line_num", SQLDataType.BIGINT),
      DSL.field("command_tag", SQLDataType.VARCHAR),
      DSL.field("session_start_time", SQLDataType.TIMESTAMPWITHTIMEZONE),
      DSL.field("virtual_transaction_id", SQLDataType.VARCHAR),
      DSL.field("transaction_id", SQLDataType.INTEGER),
      DSL.field("sql_state_code", SQLDataType.VARCHAR),
      DSL.field("detail", SQLDataType.VARCHAR),
      DSL.field("hint", SQLDataType.VARCHAR),
      DSL.field("internal_query", SQLDataType.VARCHAR),
      DSL.field("internal_query_pos", SQLDataType.INTEGER),
      DSL.field("context", SQLDataType.VARCHAR),
      DSL.field("query", SQLDataType.VARCHAR),
      DSL.field("query_pos", SQLDataType.INTEGER),
      DSL.field("location", SQLDataType.VARCHAR),
      DSL.field("application_name", SQLDataType.VARCHAR)
      );

  public static final ImmutableList<Field<?>> PATRONI_FIELDS = ImmutableList.of(
      LOG_TIME_FIELD,
      LOG_TIME_INDEX_FIELD,
      POD_NAME_FIELD,
      MAPPED_ROLE_FIELD,
      DSL.field("error_severity", SQLDataType.VARCHAR),
      MESSAGE_FIELD
      );

  public static final ImmutableList<Field<?>> LOG_FIELDS = Seq.<Field<?>>of()
      .append(LOG_TYPE_FIELD)
      .append(POSTGRES_FIELDS)
      .map(field -> field == MAPPED_ROLE_FIELD ? ROLE_FIELD : field)
      .collect(ImmutableList.toImmutableList());

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

  public DistributedLogsFetcherImpl() {
    ArcUtil.checkPublicNoArgsConstructorIsCalledFromArc();
    this.secretFinder = null;
    this.postgresConnectionManager = null;
  }

  public List<ClusterLogEntryDto> logs(
      ClusterDto cluster,
      int records,
      Tuple2<Instant, Integer> from,
      Tuple2<Instant, Integer> to,
      ImmutableMap<String, String> filters,
      boolean sortAsc,
      String text) {
    final List<Field<?>> selectedFields = LOG_FIELDS;
    final Name[] selectedFieldsArray = Seq.seq(selectedFields)
        .map(Field::getQualifiedName).toArray(Name[]::new);
    final ImmutableList<OrderField<?>> orderFields;
    if (sortAsc) {
      orderFields = ImmutableList.of(LOG_TIME_FIELD.asc(), LOG_TIME_INDEX_FIELD.asc());
    } else {
      orderFields = ImmutableList.of(LOG_TIME_FIELD.desc(), LOG_TIME_INDEX_FIELD.desc());
    }
    Seq.seq(filters)
        .forEach(filter -> Preconditions.checkArgument(
            FILTER_CONVERSION_MAP.containsKey(filter.v1),
            "Key " + filter.v1 + " is not a valid filter key"));
    try (Connection connection = getConnection(cluster);
        DSLContext context = DSL.using(connection)) {
      connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
      connection.setReadOnly(true);
      connection.setAutoCommit(true);
      try (PreparedStatement statement = connection.prepareStatement("SET TIME ZONE 'UTC'")) {
        statement.execute();
      }
      SelectConditionStep<Record> selectFromLogPatroni = context.select(
          Seq.<Field<?>>of(PATRONI_LOG_TYPE_FIELD)
          .append(PATRONI_FIELDS)
          .append(
              Seq.seq(POSTGRES_FIELDS)
              .filter(pgField -> Seq.seq(PATRONI_FIELDS)
                  .noneMatch(paField -> paField.getName().equals(pgField.getName())))
              .map(pgField -> DSL.castNull(pgField).as(pgField.getName())))
          .toList())
          .from(LOG_PATRONI_TABLE)
          .where(Seq.seq(filters)
              .map(filter -> Tuple.tuple(FILTER_CONVERSION_MAP.get(filter.v1), filter.v2))
              .filter(filter -> filter.v1.equals(LOG_TYPE_FIELD.getName()))
              .map(filter -> filter.v2.equals(PATRONI_LOG_TYPE_VALUE.getValue())
                  ? DSL.trueCondition() : DSL.falseCondition())
              .findAny()
              .orElse(DSL.trueCondition()));
      SelectConditionStep<Record> selectFromLogPostgres = context.select(
          Seq.<Field<?>>of(POSTGRES_LOG_TYPE_FIELD)
          .append(POSTGRES_FIELDS)
          .toList())
          .from(LOG_POSTGRES_TABLE)
          .where(Seq.seq(filters)
              .map(filter -> Tuple.tuple(FILTER_CONVERSION_MAP.get(filter.v1), filter.v2))
              .filter(filter -> filter.v1.equals(LOG_TYPE_FIELD.getName()))
              .map(filter -> filter.v2.equals(POSTGRES_LOG_TYPE_VALUE.getValue())
                  ? DSL.trueCondition() : DSL.falseCondition())
              .findAny()
              .orElse(DSL.trueCondition()));
      if (from != null) {
        if (sortAsc) {
          selectFromLogPatroni = selectFromLogPatroni
              .and(DSL.row(LOG_TIME_FIELD, LOG_TIME_INDEX_FIELD).greaterThan(
                  DSL.row(OffsetDateTime.ofInstant(from.v1, ZoneOffset.UTC), from.v2)));
          selectFromLogPostgres = selectFromLogPostgres
              .and(DSL.row(LOG_TIME_FIELD, LOG_TIME_INDEX_FIELD).greaterThan(
                  DSL.row(OffsetDateTime.ofInstant(from.v1, ZoneOffset.UTC), from.v2)));
        } else {
          selectFromLogPatroni = selectFromLogPatroni
              .and(DSL.row(LOG_TIME_FIELD, LOG_TIME_INDEX_FIELD).lessThan(
                  DSL.row(OffsetDateTime.ofInstant(from.v1, ZoneOffset.UTC), from.v2)));
          selectFromLogPostgres = selectFromLogPostgres
              .and(DSL.row(LOG_TIME_FIELD, LOG_TIME_INDEX_FIELD).lessThan(
                  DSL.row(OffsetDateTime.ofInstant(from.v1, ZoneOffset.UTC), from.v2)));
        }
      }
      if (to != null) {
        if (sortAsc) {
          selectFromLogPatroni = selectFromLogPatroni
              .and(DSL.row(LOG_TIME_FIELD, LOG_TIME_INDEX_FIELD).lessOrEqual(
                  DSL.row(OffsetDateTime.ofInstant(to.v1, ZoneOffset.UTC), to.v2)));
          selectFromLogPostgres = selectFromLogPostgres
              .and(DSL.row(LOG_TIME_FIELD, LOG_TIME_INDEX_FIELD).lessOrEqual(
                  DSL.row(OffsetDateTime.ofInstant(to.v1, ZoneOffset.UTC), to.v2)));
        } else {
          selectFromLogPatroni = selectFromLogPatroni
              .and(DSL.row(LOG_TIME_FIELD, LOG_TIME_INDEX_FIELD).greaterOrEqual(
                  DSL.row(OffsetDateTime.ofInstant(to.v1, ZoneOffset.UTC), to.v2)));
          selectFromLogPostgres = selectFromLogPostgres
              .and(DSL.row(LOG_TIME_FIELD, LOG_TIME_INDEX_FIELD).greaterOrEqual(
                  DSL.row(OffsetDateTime.ofInstant(to.v1, ZoneOffset.UTC), to.v2)));
        }
      }
      for (Tuple2<String, String> filter : Seq.seq(filters)
          .map(filter -> Tuple.tuple(FILTER_CONVERSION_MAP.get(filter.v1), filter.v2))
          .toList()) {
        selectFromLogPatroni = applyFilterForFields(
            selectFromLogPatroni, filter, PATRONI_FIELDS);
        selectFromLogPostgres = applyFilterForFields(
            selectFromLogPostgres, filter, POSTGRES_FIELDS);
      }
      if (text != null) {
        selectFromLogPatroni = selectFromLogPatroni
            .and(DSL.condition("{0} @@ {1}::tsquery",
                DSL.function(LOG_PATRONI_TSVECTOR_FUNCTION,
                    SQLDataType.OTHER, DSL.field(LOG_PATRONI_TABLE)),
                DSL.value(text)));
        selectFromLogPostgres = selectFromLogPostgres
            .and(DSL.condition("{0} @@ {1}::tsquery",
                DSL.function(LOG_POSTGRES_TSVECTOR_FUNCTION,
                    SQLDataType.OTHER, DSL.field(LOG_POSTGRES_TABLE)),
                DSL.value(text)));
      }
      Select<Record> query = context
          .with(DSL.name(LOG_PATRONI_WINDOW)
              .fields(selectedFieldsArray)
              .as(selectFromLogPatroni.orderBy(orderFields).limit(records)))
          .with(DSL.name(LOG_POSTGRES_WINDOW)
              .fields(selectedFieldsArray)
              .as(selectFromLogPostgres.orderBy(orderFields).limit(records)))
          .select(Seq.seq(selectedFields).map(DSL::field).toList())
          .from(LOG_PATRONI_WINDOW)
          .union(DSL.select(Seq.seq(selectedFields).map(DSL::field).toList())
              .from(LOG_POSTGRES_WINDOW))
          .orderBy(orderFields)
          .limit(records);
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("Query for cluster logs {}.{} with params"
            + " (records: {}, from: {}, to: {}, filters: {}, asc: {}, text: {}): {}",
            cluster.getMetadata().getNamespace(), cluster.getMetadata().getName(),
            records, from, to, filters, sortAsc, text, query.getSQL(ParamType.INLINED));
        LOGGER.trace("Explain query for cluster logs {}.{} with params"
            + " (records: {}, from: {}, to: {}, filters: {}, asc: {}, text: {}): {}",
            cluster.getMetadata().getNamespace(), cluster.getMetadata().getName(),
            records, from, to, filters, sortAsc, text,
            context.explain(query).toString().replace("\n", "\t"));
      }
      return Seq.seq(query.fetch())
          .map(record -> record.into(MappedClusterLogEntryDto.class))
          .collect(ImmutableList.toImmutableList());
    } catch (SQLException ex) {
      final String databaseName = Fluentd.databaseName(
          cluster.getMetadata().getNamespace(),
          cluster.getMetadata().getName());
      if (Objects.equals(ex.getMessage(),
          "FATAL: database \"" + databaseName + "\" does not exist")) {
        return ImmutableList.of();
      }
      throw new RuntimeException(ex);
    }
  }

  private SelectConditionStep<Record> applyFilterForFields(
      SelectConditionStep<Record> selectFrom, Tuple2<String, String> filter,
      ImmutableList<Field<?>> fields) {
    final SelectConditionStep<Record> currentSelectFrom = selectFrom;
    selectFrom = fields.stream()
        .filter(field -> filter.v1.equals(field.getName()))
        .findAny()
        .map(field -> currentSelectFrom
          .and(filterCondition(filter, field)))
        .orElse(selectFrom);
    return selectFrom;
  }

  protected Condition filterCondition(Tuple2<String, String> filter, Field<?> field) {
    if (filter.v2 == null) {
      return DSL.field(field.getName()).isNull();
    }
    if (field == MAPPED_ROLE_FIELD) {
      return DSL.field(field.getName())
          .eq(DSL.cast(REVERSE_ROLE_MAP.getOrDefault(filter.v2, filter.v2), field));
    }
    return DSL.field(field.getName()).eq(DSL.cast(filter.v2, field));
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
    String serviceName = PatroniServices.readWriteName(name);
    Secret secret = secretFinder.findByNameAndNamespace(name, namespace)
        .orElseThrow(() -> new NotFoundException(
            "Secret with username and password for user postgres can not be found."));
    return postgresConnectionManager.getConnection(
        serviceName + "." + namespace,
        "postgres",
        ResourceUtil.dencodeSecret(secret.getData().get("superuser-password")),
        Fluentd.databaseName(
            cluster.getMetadata().getNamespace(),
            cluster.getMetadata().getName()));
  }
}
