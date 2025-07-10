/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.stream.jobs.target.migration;

import io.debezium.sink.DebeziumSinkRecord;
import io.debezium.sink.naming.DefaultCollectionNamingStrategy;

public class StreamMigrationCollectionNamingStrategy extends DefaultCollectionNamingStrategy {

  private static String topicPrefix;

  public static void setTopicPrefix(String topicPrefix) {
    StreamMigrationCollectionNamingStrategy.topicPrefix = topicPrefix + ".";
  }

  @Override
  public String resolveCollectionName(DebeziumSinkRecord record, String collectionNameFormat) {
    String collectionName = super.resolveCollectionName(record, collectionNameFormat);
    if (record.topicName().startsWith(StreamMigrationCollectionNamingStrategy.topicPrefix)) {
      final String originalName = record.topicName().substring(StreamMigrationCollectionNamingStrategy.topicPrefix.length());
      collectionName = collectionNameFormat.replace("${original}", originalName);
    }
    return collectionName;
  }

}
