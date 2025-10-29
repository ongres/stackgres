/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.stream.jobs.target.migration;

import io.debezium.connector.jdbc.JdbcKafkaSinkRecord;
import io.debezium.connector.jdbc.JdbcSinkConnectorConfig;
import io.debezium.connector.jdbc.JdbcSinkRecord;
import io.debezium.connector.jdbc.QueryBinder;
import io.debezium.connector.jdbc.QueryBinderResolver;
import io.debezium.connector.jdbc.dialect.DatabaseDialect;
import io.stackgres.stream.jobs.target.migration.jdbc.RecordWriter;
import org.hibernate.SharedSessionContract;

public class EnhancedRecordWriter extends RecordWriter {

  private final boolean detectInsertMode;

  public EnhancedRecordWriter(
      SharedSessionContract session,
      QueryBinderResolver queryBinderResolver,
      JdbcSinkConnectorConfig config,
      DatabaseDialect dialect,
      boolean detectInsertMode) {
    super(session, queryBinderResolver, config, dialect);
    this.detectInsertMode = detectInsertMode;
  }

  public boolean isDetectInsertMode() {
    return detectInsertMode;
  }

  @Override
  protected void bindValues(JdbcSinkRecord record, QueryBinder queryBinder) {
    if (!detectInsertMode) {
      super.bindValues(record, queryBinder);
      return;
    }
    if (record.isDelete()) {
      bindKeyValuesToQuery(record, queryBinder, 1);
      return;
    }

    if (isInsert(record)) {
      int index = bindKeyValuesToQuery(record, queryBinder, 1);
      bindNonKeyValuesToQuery(record, queryBinder, index);
    } else {
      int index = bindNonKeyValuesToQuery(record, queryBinder, 1);
      bindKeyValuesToQuery(record, queryBinder, index);
    }
  }

  public boolean isSnapshot(JdbcSinkRecord jdbcSinkRecord) {
    return jdbcSinkRecord instanceof JdbcKafkaSinkRecord kafkaSinkRecord
        && kafkaSinkRecord.getOriginalKafkaRecord().headers()
        .lastWithName(SgClusterStreamMigrationHandler.JdbcHandler.SNAPSHOT_HEADER_KEY) != null;
  }

  public boolean isInsert(JdbcSinkRecord jdbcSinkRecord) {
    return jdbcSinkRecord instanceof JdbcKafkaSinkRecord kafkaSinkRecord
        && kafkaSinkRecord.getOriginalKafkaRecord().headers()
        .lastWithName(SgClusterStreamMigrationHandler.JdbcHandler.INSERT_HEADER_KEY) != null;
  }

}
