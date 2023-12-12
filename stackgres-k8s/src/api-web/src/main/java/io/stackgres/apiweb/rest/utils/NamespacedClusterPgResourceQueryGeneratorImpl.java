/*
 * Copyright (C) 2023 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest.utils;

import java.sql.ResultSet;
import java.util.Objects;

import io.stackgres.apiweb.rest.NamespacedClusterPgResource;
import jakarta.enterprise.context.ApplicationScoped;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.impl.DSL;

@ApplicationScoped
public class NamespacedClusterPgResourceQueryGeneratorImpl
    implements NamespacedClusterPgResourceQueryGenerator {
  private static final String ASC = "asc";
  private static final String DESC = "desc";

  @Override
  public ResultSet generateQuery(final DSLContext context,
                              final String table,
                              final String sort,
                              final String dir,
                              final Integer limit) {
    Field<String> orderByOf = DSL.noField(String.class);
    if (sort != null && dir != null) {
      orderByOf =
          DSL.when(DSL.field(dir).eq(ASC), String.format("%s %s", sort, ASC))
              .otherwise(String.format("%s %s", sort, DESC));
    }

    switch (table) {
      case NamespacedClusterPgResource.TOP_PG_STAT_STATEMENTS -> {
        return context
                      .select(DSL.field("pg_stat_statements", DSL.asterisk()),
                              DSL.field("pg_database", "datname"),
                              DSL.field("pg_roles", "rolname"))
                      .from("pg_stat_statements")
                      .innerJoin("pg_database")
                            .on(DSL.field("oid").eq(DSL.field("dbid")))
                      .innerJoin("pg_roles")
                            .on(DSL.field("pg_database.oid").eq(DSL.field("pg_roles.oid")))
                      .orderBy(orderByOf)
                      .limit(limit)
                      .fetchResultSet();
      }

      case NamespacedClusterPgResource.TOP_PG_STAT_ACTIVITY -> {
        return context
                    .select(DSL.field("pg_stat_activity", DSL.asterisk()))
                    .from(DSL.table("pg_stat_activity"))
                    .orderBy(orderByOf)
                    .limit(limit)
                    .fetchResultSet();
      }
      case NamespacedClusterPgResource.TOP_PG_LOCKS -> {
        return  context
                .select(DSL.field("pg_locks", DSL.asterisk()), DSL.field("pg_database", "datname"))
                .from(DSL.table("pg_locks"))
                .innerJoin(DSL.table("pg_database")).on(DSL.field("database").eq(DSL.field("oid")))
                .orderBy(orderByOf)
                .limit(limit)
                .fetchResultSet();
      }
      default ->
              throw new IllegalArgumentException(
                  String.format("Cannot generate query for table %s", table)
              );
    }
  }
}
