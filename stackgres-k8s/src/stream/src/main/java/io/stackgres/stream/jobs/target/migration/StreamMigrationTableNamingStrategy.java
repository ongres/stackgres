/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.stream.jobs.target.migration;

import io.debezium.connector.jdbc.JdbcSinkConnectorConfig;
import io.debezium.connector.jdbc.naming.DefaultTableNamingStrategy;
import org.apache.kafka.connect.sink.SinkRecord;

public class StreamMigrationTableNamingStrategy extends DefaultTableNamingStrategy {

  private static String topicPrefix;

  public static void setTopicPrefix(String topicPrefix) {
    StreamMigrationTableNamingStrategy.topicPrefix = topicPrefix + ".";
  }

  @Override
  public String resolveTableName(JdbcSinkConnectorConfig config, SinkRecord record) {
    String table = super.resolveTableName(config, record);
    final String originalName = record.topic().substring(StreamMigrationTableNamingStrategy.topicPrefix.length());
    table = config.getTableNameFormat().replace("${original}", originalName);
    return table;
  }

}
