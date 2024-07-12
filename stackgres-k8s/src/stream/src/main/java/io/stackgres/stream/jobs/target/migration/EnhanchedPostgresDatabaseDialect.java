/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.stream.jobs.target.migration;

import java.util.List;
import java.util.Locale;

import io.debezium.connector.jdbc.JdbcSinkConnectorConfig;
import io.debezium.connector.jdbc.relational.ColumnDescriptor;
import io.debezium.connector.jdbc.type.Type;
import io.stackgres.stream.jobs.target.migration.dialect.postgres.PostgresDatabaseDialect;
import org.apache.kafka.connect.data.Schema;
import org.hibernate.SessionFactory;

public class EnhanchedPostgresDatabaseDialect extends PostgresDatabaseDialect {

  public EnhanchedPostgresDatabaseDialect(
      JdbcSinkConnectorConfig config,
      SessionFactory sessionFactory) {
    super(config, sessionFactory);
  }

  @Override
  public String getQueryBindingWithValueCast(ColumnDescriptor column, Schema schema, Type type) {
    final String typeName = column.getTypeName().toLowerCase(Locale.US);
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
      if (List.of(
          "aclitem", "cid", "jsonpath", "pg_lsn", "pg_snapshot",
          "tid", "tsquery", "tsvector", "txid_snapshot", "xid", "xid8",
          "box", "circle", "datemultirange", "dblink_pkey_results",
          "int4multirange", "int8multirange", "line", "lseg",
          "nummultirange", "path", "polygon", "regclass",
          "regcollation", "regconfig", "regdictionary", "regnamespace",
          "regoper", "regoperator", "regproc", "regprocedure",
          "regrole", "regtype", "tsmultirange", "tstzmultirange")
          .contains(typeName)) {
        return "cast(encode(cast(? as bytea), 'escape') as " + typeName + ")";
      }
    }
    String queryBindingWithValueCast = super.getQueryBindingWithValueCast(column, schema, type);
    if (queryBindingWithValueCast.equals("?")) {
      return "?::" + typeName;
    }
    return queryBindingWithValueCast;
  }

}
