/*
 * Copyright (C) 2023 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest.utils;

import io.stackgres.apiweb.rest.sgcluster.NamespacedClusterPgResource;
import jakarta.enterprise.context.ApplicationScoped;
import org.jooq.DSLContext;
import org.jooq.Select;
import org.jooq.SelectLimitStep;
import org.jooq.SelectOrderByStep;
import org.jooq.SortField;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

@ApplicationScoped
public class NamespacedClusterPgResourceQueryGenerator {
  public static final String ASC = "asc";
  public static final String DESC = "desc";

  public String generateQuery(
      final DSLContext context,
      final String table,
      final String sort,
      final String dir,
      final Integer limit) {
    final SelectOrderByStep<?> selectOrderByStep = switch (table) {
      case NamespacedClusterPgResource.TOP_PG_STAT_STATEMENTS -> context
          .select(
              DSL.field("pg_database.datname"),
              DSL.field("pg_roles.rolname"),
              DSL.table("pg_stat_statements").asterisk())
          .from("pg_stat_statements")
          .leftJoin("pg_database")
          .on(DSL.field("pg_database.oid", SQLDataType.INTEGER).eq(
              DSL.field("pg_stat_statements.dbid", SQLDataType.INTEGER)))
          .leftJoin("pg_roles")
          .on(DSL.field("pg_roles.oid", SQLDataType.INTEGER).eq(
              DSL.field("pg_stat_statements.userid", SQLDataType.INTEGER)));
      case NamespacedClusterPgResource.TOP_PG_STAT_ACTIVITY -> context
          .select(DSL.asterisk())
          .from(DSL.table("pg_stat_activity"));
      case NamespacedClusterPgResource.TOP_PG_LOCKS -> context
          .select(
              DSL.field("pg_database.datname"),
              DSL.table("pg_locks").asterisk())
          .from(DSL.table("pg_locks"))
          .leftJoin(DSL.table("pg_database"))
          .on(DSL.field("pg_locks.database").eq(DSL.field("pg_database.oid")));
      default -> throw new IllegalArgumentException(
          String.format("Cannot generate query for table %s", table));
    };
    final SelectLimitStep<?> selectLimitStep;
    if (sort != null) {
      final SortField<?> sortField;
      if (dir != null && dir.equals(DESC)) {
        sortField = DSL.field(DSL.name(sort)).desc();
      } else {
        sortField = DSL.field(DSL.name(sort)).asc();
      }
      selectLimitStep = selectOrderByStep.orderBy(sortField);
    } else {
      selectLimitStep = selectOrderByStep;
    }
    final Select<?> select;
    if (limit != null) {
      select = selectLimitStep.limit(limit);
    } else {
      select = selectLimitStep;
    }
    return select.getSQL();
  }
}
