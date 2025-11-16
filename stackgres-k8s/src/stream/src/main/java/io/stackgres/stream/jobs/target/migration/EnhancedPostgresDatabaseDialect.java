/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.stream.jobs.target.migration;

import java.util.List;
import java.util.Locale;

import io.debezium.connector.jdbc.JdbcSinkConnectorConfig;
import io.debezium.connector.jdbc.JdbcSinkRecord;
import io.debezium.connector.jdbc.dialect.SqlStatementBuilder;
import io.debezium.connector.jdbc.field.JdbcFieldDescriptor;
import io.debezium.connector.jdbc.relational.TableDescriptor;
import io.debezium.connector.jdbc.type.JdbcType;
import io.debezium.sink.column.ColumnDescriptor;
import io.stackgres.stream.jobs.target.migration.SgClusterStreamMigrationHandler.JdbcHandler;
import io.stackgres.stream.jobs.target.migration.dialect.postgres.PostgresDatabaseDialect;
import org.apache.kafka.connect.data.Schema;
import org.hibernate.SessionFactory;

public class EnhancedPostgresDatabaseDialect extends PostgresDatabaseDialect {

  private final JdbcHandler jdbcHandler;

  public EnhancedPostgresDatabaseDialect(JdbcHandler jdbcHandler, JdbcSinkConnectorConfig config,
      SessionFactory sessionFactory) {
    super(config, sessionFactory);
    this.jdbcHandler = jdbcHandler;
  }

  @Override
  public String getQueryBindingWithValueCast(ColumnDescriptor column, Schema schema,
      JdbcType type) {
    final String typeName = column.getTypeName().toLowerCase(Locale.US);
    if (schema.type() == Schema.Type.STRING) {
      if ("uuid".equals(typeName)) {
        return "cast(? as uuid)";
      } else if ("json".equals(typeName)) {
        return "cast(? as json)";
      } else if ("jsonb".equals(typeName)) {
        return "cast(? as jsonb)";
      }
    }
    if ("smallserial".equals(typeName)) {
      return "?::smallint";
    }
    if ("serial".equals(typeName)) {
      return "?::integer";
    }
    if ("bigserial".equals(typeName)) {
      return "?::bigint";
    }
    if (schema.type() == Schema.Type.BYTES) {
      if (List.of("aclitem", "cid", "jsonpath", "pg_lsn", "pg_snapshot", "tid", "tsquery",
          "tsvector", "txid_snapshot", "xid", "xid8", "box", "circle", "datemultirange",
          "dblink_pkey_results", "int4multirange", "int8multirange", "line", "lseg",
          "nummultirange", "path", "polygon", "regclass", "regcollation", "regconfig",
          "regdictionary", "regnamespace", "regoper", "regoperator", "regproc", "regprocedure",
          "regrole", "regtype", "tsmultirange", "tstzmultirange").contains(typeName)) {
        return "cast(encode(cast(? as bytea), 'escape') as " + typeName + ")";
      }
    }
    String queryBindingWithValueCast = super.getQueryBindingWithValueCast(column, schema, type);
    if (queryBindingWithValueCast.equals("?")) {
      return "?::" + typeName;
    }
    return queryBindingWithValueCast;
  }

  @Override
  public String getUpsertStatement(TableDescriptor table, JdbcSinkRecord record) {
    final SqlStatementBuilder builder = new SqlStatementBuilder();
    builder.append("INSERT INTO ");
    builder.append(getQualifiedTableName(table.getId()));
    builder.append(" (");
    builder.appendLists(",", record.keyFieldNames(), record.nonKeyFieldNames(),
        (name) -> columnNameFromField(name, record));
    builder.append(") VALUES (");
    builder.appendLists(",", record.keyFieldNames(), record.nonKeyFieldNames(),
        (name) -> columnQueryBindingFromField(name, table, record));
    builder.append(") ON CONFLICT (");
    builder.appendList(",", record.keyFieldNames(), (name) -> columnNameFromField(name, record));
    if (record.nonKeyFieldNames().isEmpty()) {
      builder.append(") DO NOTHING");
    } else {
      builder.append(") DO UPDATE SET ");
      builder.appendList(",", record.nonKeyFieldNames(), (fieldName) -> {
        final String columnName = columnNameFromField(fieldName, record);
        if (jdbcHandler.isPlaceholder(record.getPayload().get(fieldName))) {
          return columnName + "=" + getQualifiedTableName(table.getId()) + "." + columnName;
        }
        return columnName + "=EXCLUDED." + columnName;
      });
    }
    return builder.build();
  }

  @Override
  public String getUpdateStatement(TableDescriptor table, JdbcSinkRecord record) {
    final SqlStatementBuilder builder = new SqlStatementBuilder();
    builder.append("UPDATE ");
    builder.append(getQualifiedTableName(table.getId()));
    builder.append(" SET ");
    builder.appendList(", ", record.nonKeyFieldNames(),
        (name) -> columnNameEqualsBinding(name, table, record));

    if (!record.keyFieldNames().isEmpty()) {
      builder.append(" WHERE ");
      builder.appendList(" AND ", record.keyFieldNames(),
          (name) -> columnNameEqualsBinding(name, table, record));
    }

    return builder.build();
  }

  private String columnNameEqualsBinding(String fieldName, TableDescriptor table, JdbcSinkRecord record) {
    final JdbcFieldDescriptor field = record.jdbcFields().get(fieldName);
    final String columnName = resolveColumnName(field);
    if (jdbcHandler.isPlaceholder(record.getPayload().get(fieldName))) {
      return toIdentifier(columnName) + "=" + getQualifiedTableName(table.getId()) + "." + toIdentifier(columnName);
    }
    final ColumnDescriptor column = table.getColumnByName(columnName);
    return toIdentifier(columnName) + "=" + field.getQueryBinding(column, record.getPayload());
  }

}
