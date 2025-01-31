/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.stackgres.stream.jobs.target.migration.dialect.postgres;

import java.util.List;
import java.util.Map;

import io.debezium.connector.jdbc.ValueBindDescriptor;
import io.debezium.connector.jdbc.dialect.DatabaseDialect;
import io.debezium.connector.jdbc.relational.ColumnDescriptor;
import io.debezium.connector.jdbc.type.Type;
import io.debezium.connector.jdbc.type.connect.AbstractConnectMapType;
import org.apache.kafka.connect.data.Schema;

/**
 * An implementation of {@link Type} for {@code MAP} schema types that get
 * mapped to a PostgreSQL {@code HSTORE} column type.
 *
 * @author Chris Cranford
 */
class MapToHstoreType extends AbstractConnectMapType {

  public static final MapToHstoreType INSTANCE = new MapToHstoreType();

  @Override
  public String getQueryBinding(ColumnDescriptor column, Schema schema, Object value) {
    return "cast(? as hstore)";
  }

  @Override
  public String getTypeName(DatabaseDialect dialect, Schema schema, boolean key) {
    // This type explicitly maps the MAP schema type to HSTORE
    return "hstore";
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<ValueBindDescriptor> bind(int index, Schema schema, Object value) {
    return super.bind(index, schema, HstoreConverter.mapToString((Map<String, String>) value));
  }

}
