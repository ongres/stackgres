/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.stream.jobs.target.migration;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import io.debezium.connector.jdbc.JdbcSinkConnectorConfig;
import io.debezium.connector.jdbc.JdbcSinkRecord;
import io.debezium.connector.jdbc.dialect.DatabaseDialect;
import io.debezium.connector.jdbc.relational.TableDescriptor;
import io.debezium.metadata.CollectionId;
import io.debezium.util.Stopwatch;
import io.stackgres.stream.jobs.target.migration.jdbc.JdbcChangeEventSink;
import org.hibernate.StatelessSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnhancedJdbcChangeEventSink extends JdbcChangeEventSink {

  private static final Logger LOGGER = LoggerFactory.getLogger(JdbcChangeEventSink.class);

  private final DatabaseDialect dialect;
  private final EnhancedRecordWriter recordWriter;

  public EnhancedJdbcChangeEventSink(
      JdbcSinkConnectorConfig config,
      StatelessSession session,
      DatabaseDialect dialect,
      EnhancedRecordWriter recordWriter) {
    super(config, session, dialect, recordWriter);
    this.dialect = dialect;
    this.recordWriter = recordWriter;
  }

  @Override
  protected void flushBuffer(
      CollectionId collectionId,
      List<JdbcSinkRecord> toFlush,
      TableDescriptor table) throws SQLException {
    if (!recordWriter.isDetectInsertMode()) {
      super.flushBuffer(collectionId, toFlush, table);
      return;
    }
    Stopwatch flushBufferStopwatch = Stopwatch.reusable();
    Stopwatch tableChangesStopwatch = Stopwatch.reusable();
    if (!toFlush.isEmpty()) {
      LOGGER.debug("Flushing records in JDBC Writer for table: {}", collectionId.name());
      tableChangesStopwatch.start();
      tableChangesStopwatch.stop();
      final int size = toFlush.size();
      int currentIndex = 0;
      String currentSqlStatement = getSqlStatementWithHints(table, toFlush.get(0));
      boolean wasSnapshot = recordWriter.isSnapshot(toFlush.get(0));
      for (int index = 1; index <= size; index++) {
        final String sqlStatement;
        if (index < size) {
          final var currentToFlush = toFlush.get(index);
          final boolean isSnapshot = recordWriter.isSnapshot(currentToFlush);
          if (wasSnapshot && isSnapshot) {
            sqlStatement = currentSqlStatement;
          } else {
            sqlStatement = getSqlStatementWithHints(table, currentToFlush);
          }
          wasSnapshot = isSnapshot;
          if (Objects.equals(sqlStatement, currentSqlStatement)) {
            continue;
          }
        } else {
          sqlStatement = null;
        }
        flushBufferStopwatch.start();
        recordWriter.write(toFlush.subList(currentIndex, index), currentSqlStatement);
        flushBufferStopwatch.stop();
        currentIndex = index;
        currentSqlStatement = sqlStatement;
        LOGGER.trace("[PERF] Flush buffer execution time {}", flushBufferStopwatch.durations());
        LOGGER.trace("[PERF] Table changes execution time {}", tableChangesStopwatch.durations());
      }
    }
  }

  private String getSqlStatementWithHints(
      TableDescriptor table,
      JdbcSinkRecord record) {
    if (!record.isDelete()) {
      if (recordWriter.isInsert(record)) {
        return dialect.getInsertStatement(table, record);
      } else {
        return dialect.getUpdateStatement(table, record);
      }
    } else {
      return dialect.getDeleteStatement(table, record);
    }
  }

}
