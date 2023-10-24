/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.distributedlogs;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.distributedlogs.LogTableFields;
import io.stackgres.common.distributedlogs.PostgresTableFields;
import io.stackgres.common.distributedlogs.Tables;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.OrderField;
import org.jooq.Param;
import org.jooq.Record;
import org.jooq.Select;
import org.jooq.SelectConditionStep;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

public class DistributedLogsQueryGenerator {

  public static final String LOG_POSTGRES_TABLE = Tables.LOG_POSTGRES.getTableName();
  public static final String LOG_PATRONI_TABLE = Tables.LOG_PATRONI.getTableName();
  public static final String LOG_POSTGRES_WINDOW = "log_postgres_window";
  public static final String LOG_PATRONI_WINDOW = "log_patroni_window";
  public static final String LOG_POSTGRES_TSVECTOR_FUNCTION = "log_postgres_tsvector";
  public static final String LOG_PATRONI_TSVECTOR_FUNCTION = "log_patroni_tsvector";

  public static final Param<String> PATRONI_LOG_TYPE_VALUE = DSL.value("pa");
  public static final Param<String> POSTGRES_LOG_TYPE_VALUE = DSL.value("pg");

  public static final Param<String> PRIMARY_ROLE_VALUE = DSL.value("Primary");
  public static final Param<String> REPLICA_ROLE_VALUE = DSL.value("Replica");
  public static final Param<String> PROMOTED_ROLE_VALUE = DSL.value("Promoted");
  public static final Param<String> DEMOTED_ROLE_VALUE = DSL.value("Demoted");
  public static final Param<String> UNINITIALIZED_ROLE_VALUE = DSL.value("Uninitialized");
  public static final Param<String> REPLICA_LEADER_ROLE_VALUE = DSL.value("ReplicaLeader");
  public static final Param<String> SYNC_REPLICA_ROLE_VALUE = DSL.value("SyncReplica");

  public static final String LOG_TIME = LogTableFields.LOG_TIME;
  public static final String LOG_TIME_INDEX = LogTableFields.LOG_TIME_INDEX;
  public static final String LOG_TYPE = LogTableFields.LOG_TYPE;
  public static final String MESSAGE = LogTableFields.MESSAGE;
  public static final String POD_NAME = LogTableFields.POD_NAME;
  public static final String ROLE = LogTableFields.ROLE;
  public static final String ERROR_SEVERITY = LogTableFields.ERROR_SEVERITY;

  public static final String USER_NAME = PostgresTableFields.USER_NAME.getFieldName();
  public static final String DATABASE_NAME = PostgresTableFields.DATABASE_NAME.getFieldName();
  public static final String PROCESS_ID = PostgresTableFields.PROCESS_ID.getFieldName();

  public static final String CONNECTION_FROM = PostgresTableFields.CONNECTION_FROM.getFieldName();
  public static final String SESSION_ID = PostgresTableFields.SESSION_ID.getFieldName();
  public static final String SESSION_LINE_NUM = PostgresTableFields.SESSION_LINE_NUM.getFieldName();
  public static final String COMMAND_TAG = PostgresTableFields.COMMAND_TAG.getFieldName();
  public static final String SESSION_START_TIME = PostgresTableFields.SESSION_START_TIME
      .getFieldName();
  public static final String VIRTUAL_TRANSACTION_ID = PostgresTableFields.VIRTUAL_TRANSACTION_ID
      .getFieldName();
  public static final String TRANSACTION_ID = PostgresTableFields.TRANSACTION_ID.getFieldName();
  public static final String SQL_STATE_CODE = PostgresTableFields.SQL_STATE_CODE.getFieldName();
  public static final String DETAIL = PostgresTableFields.DETAIL.getFieldName();
  public static final String HINT = PostgresTableFields.HINT.getFieldName();
  public static final String INTERNAL_QUERY = PostgresTableFields.INTERNAL_QUERY.getFieldName();
  public static final String INTERNAL_QUERY_POS = PostgresTableFields.INTERNAL_QUERY_POS
      .getFieldName();
  public static final String CONTEXT = PostgresTableFields.CONTEXT.getFieldName();
  public static final String QUERY = PostgresTableFields.QUERY.getFieldName();
  public static final String QUERY_POS = PostgresTableFields.QUERY_POS.getFieldName();
  public static final String LOCATION = PostgresTableFields.LOCATION.getFieldName();
  public static final String APPLICATION_NAME = PostgresTableFields.APPLICATION_NAME.getFieldName();

  public static final Field<String> LOG_TYPE_FIELD = DSL.field(
      LOG_TYPE, SQLDataType.VARCHAR);
  public static final Field<OffsetDateTime> LOG_TIME_FIELD = DSL.field(
      LOG_TIME, SQLDataType.TIMESTAMPWITHTIMEZONE);
  public static final Field<Integer> LOG_TIME_INDEX_FIELD = DSL.field(
      LOG_TIME_INDEX, SQLDataType.INTEGER);
  public static final Field<String> MESSAGE_FIELD = DSL.field(
      MESSAGE, SQLDataType.VARCHAR);
  public static final Field<String> POD_NAME_FIELD = DSL.field(POD_NAME, SQLDataType.VARCHAR);
  public static final Field<String> ROLE_FIELD = DSL.field(ROLE, SQLDataType.VARCHAR);
  public static final Field<String> PATRONI_LOG_TYPE_FIELD = PATRONI_LOG_TYPE_VALUE
      .as(LOG_TYPE_FIELD);
  public static final Field<String> POSTGRES_LOG_TYPE_FIELD = POSTGRES_LOG_TYPE_VALUE
      .as(LOG_TYPE_FIELD);
  public static final Field<String> MAPPED_ROLE_FIELD = DSL.case_(ROLE_FIELD)
      .when(PatroniUtil.PRIMARY_ROLE, PRIMARY_ROLE_VALUE)
      .when(PatroniUtil.REPLICA_ROLE, REPLICA_ROLE_VALUE)
      .when(PatroniUtil.PROMOTED_ROLE, PROMOTED_ROLE_VALUE)
      .when(PatroniUtil.DEMOTED_ROLE, DEMOTED_ROLE_VALUE)
      .when(PatroniUtil.UNINITIALIZED_ROLE, UNINITIALIZED_ROLE_VALUE)
      .when(PatroniUtil.STANDBY_LEADER_ROLE, REPLICA_LEADER_ROLE_VALUE)
      .when(PatroniUtil.SYNC_STANDBY_ROLE, SYNC_REPLICA_ROLE_VALUE)
      .else_(DSL.castNull(ROLE_FIELD))
      .as(ROLE_FIELD);

  public static final ImmutableMap<String, String> REVERSE_ROLE_MAP =
      ImmutableMap.<String, String>builder()
          .put(PRIMARY_ROLE_VALUE.getValue(), PatroniUtil.PRIMARY_ROLE)
          .put(REPLICA_ROLE_VALUE.getValue(), PatroniUtil.REPLICA_ROLE)
          .put(PROMOTED_ROLE_VALUE.getValue(), PatroniUtil.PROMOTED_ROLE)
          .put(DEMOTED_ROLE_VALUE.getValue(), PatroniUtil.DEMOTED_ROLE)
          .put(UNINITIALIZED_ROLE_VALUE.getValue(), PatroniUtil.UNINITIALIZED_ROLE)
          .put(REPLICA_LEADER_ROLE_VALUE.getValue(), PatroniUtil.STANDBY_LEADER_ROLE)
          .put(SYNC_REPLICA_ROLE_VALUE.getValue(), PatroniUtil.SYNC_STANDBY_ROLE)
          .build();

  public static final ImmutableMap<String, String> FILTER_CONVERSION_MAP =
      ImmutableMap.<String, String>builder()
      .put("logTime", LOG_TIME)
      .put("logTimeIndex", LOG_TIME_INDEX)
      .put("logType", LOG_TYPE)
      .put("podName", POD_NAME)
      .put("role", ROLE)
      .put("errorLevel", ERROR_SEVERITY)
      .put("message", MESSAGE)
      .put("userName", USER_NAME)
      .put("databaseName", DATABASE_NAME)
      .put("processId", PROCESS_ID)
      .put("connectionFrom", CONNECTION_FROM)
      .put("sessionId", SESSION_ID)
      .put("sessionLineNum", SESSION_LINE_NUM)
      .put("commandTag", COMMAND_TAG)
      .put("sessionStartTime", SESSION_START_TIME)
      .put("virtualTransactionId", VIRTUAL_TRANSACTION_ID)
      .put("transactionId", TRANSACTION_ID)
      .put("sqlStateCode", SQL_STATE_CODE)
      .put("detail", DETAIL)
      .put("hint", HINT)
      .put("internalQuery", INTERNAL_QUERY)
      .put("internalQueryPos", INTERNAL_QUERY_POS)
      .put("context", CONTEXT)
      .put("query", QUERY)
      .put("queryPos", QUERY_POS)
      .put("location", LOCATION)
      .put("applicationName", APPLICATION_NAME)
      .build();

  public static final ImmutableList<Field<?>> POSTGRES_FIELDS = ImmutableList.of(
      LOG_TIME_FIELD,
      LOG_TIME_INDEX_FIELD,
      POD_NAME_FIELD,
      MAPPED_ROLE_FIELD,
      DSL.field(ERROR_SEVERITY, SQLDataType.VARCHAR),
      MESSAGE_FIELD,
      DSL.field(USER_NAME, SQLDataType.VARCHAR),
      DSL.field(DATABASE_NAME, SQLDataType.VARCHAR),
      DSL.field(PROCESS_ID, SQLDataType.INTEGER),
      DSL.field(CONNECTION_FROM, SQLDataType.VARCHAR),
      DSL.field(SESSION_ID, SQLDataType.VARCHAR),
      DSL.field(SESSION_LINE_NUM, SQLDataType.BIGINT),
      DSL.field(COMMAND_TAG, SQLDataType.VARCHAR),
      DSL.field(SESSION_START_TIME, SQLDataType.TIMESTAMPWITHTIMEZONE),
      DSL.field(VIRTUAL_TRANSACTION_ID, SQLDataType.VARCHAR),
      DSL.field(TRANSACTION_ID, SQLDataType.INTEGER),
      DSL.field(SQL_STATE_CODE, SQLDataType.VARCHAR),
      DSL.field(DETAIL, SQLDataType.VARCHAR),
      DSL.field(HINT, SQLDataType.VARCHAR),
      DSL.field(INTERNAL_QUERY, SQLDataType.VARCHAR),
      DSL.field(INTERNAL_QUERY_POS, SQLDataType.INTEGER),
      DSL.field(CONTEXT, SQLDataType.VARCHAR),
      DSL.field(QUERY, SQLDataType.VARCHAR),
      DSL.field(QUERY_POS, SQLDataType.INTEGER),
      DSL.field(LOCATION, SQLDataType.VARCHAR),
      DSL.field(APPLICATION_NAME, SQLDataType.VARCHAR)
      );

  public static final ImmutableList<Field<?>> PATRONI_FIELDS = ImmutableList.of(
      LOG_TIME_FIELD,
      LOG_TIME_INDEX_FIELD,
      POD_NAME_FIELD,
      MAPPED_ROLE_FIELD,
      DSL.field(ERROR_SEVERITY, SQLDataType.VARCHAR),
      MESSAGE_FIELD
      );

  public static final List<Field<?>> LOG_FIELDS = Seq.<Field<?>>of()
      .append(LOG_TYPE_FIELD)
      .append(POSTGRES_FIELDS)
      .map(field -> field == MAPPED_ROLE_FIELD ? ROLE_FIELD : field)
      .toList();

  private final DSLContext context;
  private final DistributedLogsQueryParameters parameters;

  public DistributedLogsQueryGenerator(
      DSLContext context,
      DistributedLogsQueryParameters parameters) {
    this.context = context;
    this.parameters = parameters;
  }

  public Select<Record> generateQuery() {
    final List<Field<?>> selectedFields = LOG_FIELDS;
    final Name[] selectedFieldsArray = Seq.seq(selectedFields)
        .map(Field::getQualifiedName).toArray(Name[]::new);
    final ImmutableList<OrderField<?>> orderFields;
    if (parameters.isSortAsc()) {
      orderFields = ImmutableList.of(LOG_TIME_FIELD.asc(), LOG_TIME_INDEX_FIELD.asc());
    } else {
      orderFields = ImmutableList.of(LOG_TIME_FIELD.desc(), LOG_TIME_INDEX_FIELD.desc());
    }
    Seq.seq(parameters.getFilters())
        .forEach(filter -> Preconditions.checkArgument(
            FILTER_CONVERSION_MAP.containsKey(filter.v1),
            "Key " + filter.v1 + " is not a valid filter key"));
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
        .where(Seq.seq(parameters.getFilters())
            .map(filter -> Tuple.tuple(FILTER_CONVERSION_MAP.get(filter.v1), filter.v2))
            .filter(filter -> filter.v1.equals(LOG_TYPE_FIELD.getName()))
            .map(filter -> filter.v2.stream()
                .map(PATRONI_LOG_TYPE_VALUE.getValue()::equals)
                .filter(Boolean.TRUE::equals).findAny().orElse(Boolean.FALSE)
                ? DSL.trueCondition() : DSL.falseCondition())
            .findAny()
            .orElse(DSL.trueCondition()));
    SelectConditionStep<Record> selectFromLogPostgres = context.select(
        Seq.<Field<?>>of(POSTGRES_LOG_TYPE_FIELD)
        .append(POSTGRES_FIELDS)
        .toList())
        .from(LOG_POSTGRES_TABLE)
        .where(Seq.seq(parameters.getFilters())
            .map(filter -> Tuple.tuple(FILTER_CONVERSION_MAP.get(filter.v1), filter.v2))
            .filter(filter -> filter.v1.equals(LOG_TYPE_FIELD.getName()))
            .map(filter -> filter.v2.stream()
                .map(POSTGRES_LOG_TYPE_VALUE.getValue()::equals)
                .filter(Boolean.TRUE::equals).findAny().orElse(Boolean.FALSE)
                ? DSL.trueCondition() : DSL.falseCondition())
            .findAny()
            .orElse(DSL.trueCondition()));
    if (parameters.getFromTimeAndIndex().isPresent()) {
      Tuple2<Instant, Integer> from = parameters.getFromTimeAndIndex().get();
      if (parameters.isFromInclusive()) {
        if (parameters.isSortAsc()) {
          selectFromLogPatroni = selectFromLogPatroni
              .and(DSL.row(LOG_TIME_FIELD, LOG_TIME_INDEX_FIELD).greaterOrEqual(
                  DSL.row(OffsetDateTime.ofInstant(from.v1, ZoneOffset.UTC), from.v2)));
          selectFromLogPostgres = selectFromLogPostgres
              .and(DSL.row(LOG_TIME_FIELD, LOG_TIME_INDEX_FIELD).greaterOrEqual(
                  DSL.row(OffsetDateTime.ofInstant(from.v1, ZoneOffset.UTC), from.v2)));
        } else {
          selectFromLogPatroni = selectFromLogPatroni
              .and(DSL.row(LOG_TIME_FIELD, LOG_TIME_INDEX_FIELD).lessOrEqual(
                  DSL.row(OffsetDateTime.ofInstant(from.v1, ZoneOffset.UTC), from.v2)));
          selectFromLogPostgres = selectFromLogPostgres
              .and(DSL.row(LOG_TIME_FIELD, LOG_TIME_INDEX_FIELD).lessOrEqual(
                  DSL.row(OffsetDateTime.ofInstant(from.v1, ZoneOffset.UTC), from.v2)));
        }
      } else {
        if (parameters.isSortAsc()) {
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
    }
    if (parameters.getToTimeAndIndex().isPresent()) {
      Tuple2<Instant, Integer> to = parameters.getToTimeAndIndex().get();
      if (parameters.isSortAsc()) {
        selectFromLogPatroni = selectFromLogPatroni
            .and(DSL.row(LOG_TIME_FIELD, LOG_TIME_INDEX_FIELD).lessOrEqual(
                DSL.row(OffsetDateTime.ofInstant(to.v1, ZoneOffset.UTC), to.v2)));
        selectFromLogPostgres = selectFromLogPostgres
            .and(DSL.row(LOG_TIME_FIELD, LOG_TIME_INDEX_FIELD).lessOrEqual(
                DSL.row(
                    OffsetDateTime.ofInstant(to.v1, ZoneOffset.UTC), to.v2)));
      } else {
        selectFromLogPatroni = selectFromLogPatroni
            .and(DSL.row(LOG_TIME_FIELD, LOG_TIME_INDEX_FIELD).greaterOrEqual(
                DSL.row(
                    OffsetDateTime.ofInstant(to.v1, ZoneOffset.UTC), to.v2)));
        selectFromLogPostgres = selectFromLogPostgres
            .and(DSL.row(LOG_TIME_FIELD, LOG_TIME_INDEX_FIELD).greaterOrEqual(
                DSL.row(
                    OffsetDateTime.ofInstant(to.v1, ZoneOffset.UTC), to.v2)));
      }
    }
    for (Tuple2<String, List<String>> filter : Seq.seq(parameters.getFilters())
        .map(filter -> Tuple.tuple(FILTER_CONVERSION_MAP.get(filter.v1), filter.v2))
        .toList()) {
      selectFromLogPatroni = applyFilterForFields(
          selectFromLogPatroni, filter, PATRONI_FIELDS);
      selectFromLogPostgres = applyFilterForFields(
          selectFromLogPostgres, filter, POSTGRES_FIELDS);
    }
    if (parameters.getFullTextSearchQuery()
        .flatMap(FullTextSearchQuery::getFullTextSearchQuery).isPresent()) {
      String fullTextSearchQuery = parameters.getFullTextSearchQuery()
          .get().getFullTextSearchQuery().get();
      selectFromLogPatroni = selectFromLogPatroni
          .and(DSL.condition("{0} @@ {1}::tsquery",
              DSL.function(LOG_PATRONI_TSVECTOR_FUNCTION,
                  SQLDataType.OTHER, DSL.field(LOG_PATRONI_TABLE)),
              DSL.value(fullTextSearchQuery)));
      selectFromLogPostgres = selectFromLogPostgres
          .and(DSL.condition("{0} @@ {1}::tsquery",
              DSL.function(LOG_POSTGRES_TSVECTOR_FUNCTION,
                  SQLDataType.OTHER, DSL.field(LOG_POSTGRES_TABLE)),
              DSL.value(fullTextSearchQuery)));
    }
    Select<Record> query = context
        .with(DSL.name(LOG_PATRONI_WINDOW)
            .fields(selectedFieldsArray)
            .as(selectFromLogPatroni.orderBy(orderFields).limit(parameters.getRecords())))
        .with(DSL.name(LOG_POSTGRES_WINDOW)
            .fields(selectedFieldsArray)
            .as(selectFromLogPostgres.orderBy(orderFields).limit(parameters.getRecords())))
        .select(Seq.seq(selectedFields).map(DSL::field).toList())
        .from(LOG_PATRONI_WINDOW)
        .union(DSL.select(Seq.seq(selectedFields).map(DSL::field).toList())
            .from(LOG_POSTGRES_WINDOW))
        .orderBy(orderFields)
        .limit(parameters.getRecords());
    return query;
  }

  private SelectConditionStep<Record> applyFilterForFields(
      SelectConditionStep<Record> selectFrom, Tuple2<String, List<String>> filter,
      List<Field<?>> fields) {
    final SelectConditionStep<Record> currentSelectFrom = selectFrom;
    selectFrom = fields.stream()
        .filter(field -> filter.v1.equals(field.getName()))
        .findAny()
        .map(field -> currentSelectFrom
          .and(filterCondition(filter, field)))
        .orElse(selectFrom);
    return selectFrom;
  }

  protected Condition filterCondition(Tuple2<String, List<String>> filter,
      Field<?> field) {
    if (filter.v2.isEmpty()) {
      if (field == MAPPED_ROLE_FIELD) {
        return DSL.field(field.getName()).isNull()
            .or(DSL.field(field.getName()).notIn(
                Seq.seq(REVERSE_ROLE_MAP.keySet())
                    .map(value -> DSL.cast(value, field))
                    .toArray(Field<?>[]::new)));
      }
      return DSL.field(field.getName()).isNull();
    }
    if (field == MAPPED_ROLE_FIELD) {
      return DSL.field(field.getName())
          .in(filter.v2.stream()
              .map(f -> REVERSE_ROLE_MAP.getOrDefault(f, f))
              .map(f -> DSL.cast(f, field))
              .collect(Collectors.toList()));
    }
    return DSL.field(field.getName())
        .in(filter.v2.stream()
            .map(f -> DSL.cast(f, field))
            .collect(Collectors.toList()));
  }
}
