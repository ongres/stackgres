/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.stackgres.stream.jobs.target.migration.dialect.postgres;

import java.sql.Types;

import org.apache.kafka.connect.data.Schema;
import org.hibernate.engine.jdbc.Size;

import io.debezium.connector.jdbc.dialect.DatabaseDialect;
import io.debezium.connector.jdbc.type.AbstractBytesType;
import io.debezium.connector.jdbc.type.Type;
import io.debezium.connector.jdbc.util.ByteArrayUtils;

/**
 * An implementation of {@link Type} for {@code BYTES} column types.
 *
 * @author Bertrand Paquet
 */
class BytesType extends AbstractBytesType {

  public static final BytesType INSTANCE = new BytesType();

  @Override
  public String getDefaultValueBinding(DatabaseDialect dialect, Schema schema, Object value) {
    return String.format(dialect.getByteArrayFormat(), ByteArrayUtils.getByteArrayAsHex(value));
  }

  @Override
  public String getTypeName(DatabaseDialect dialect, Schema schema, boolean key) {
    final int columnSize = Integer.parseInt(getSourceColumnSize(schema).orElse("0"));
    if (columnSize > 0) {
      return dialect.getTypeName(Types.VARBINARY, Size.length(columnSize));
    } else if (key) {
      return dialect.getTypeName(Types.VARBINARY, Size.length(dialect.getMaxVarbinaryLength()));
    }
    return dialect.getTypeName(Types.VARBINARY);
  }
}
