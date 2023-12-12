/*
 * Copyright (C) 2023 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest.utils;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;
import java.util.stream.Stream;

import io.stackgres.apiweb.rest.NamespacedClusterPgResource;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DefaultDSLContext;
import org.jooq.lambda.Seq;
import org.jooq.lambda.Unchecked;
import org.jooq.lambda.tuple.Tuple;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NamespacedClusterPgResourceQueryGeneratorTest {

  private final NamespacedClusterPgResourceQueryGenerator queryGenerator =
          new NamespacedClusterPgResourceQueryGenerator();

  private static final Properties EXPECTED = Unchecked.supplier(
      NamespacedClusterPgResourceQueryGeneratorTest::loadExpected).get();

  @ParameterizedTest
  @MethodSource("parameters")
  void testQueryGenerator(
      final String table,
      final String sort,
      final String dir,
      final Integer limit) throws SQLException {
    DSLContext context = new DefaultDSLContext(SQLDialect.POSTGRES);
    String sql = queryGenerator.generateQuery(context, table, sort, dir, limit);
    Assertions.assertEquals(
        table + "-" + sort + "-" + dir + "-" + limit + "="
            + EXPECTED.get(table + "-" + sort + "-" + dir + "-" + limit),
        table + "-" + sort + "-" + dir + "-" + limit + "=" + sql);
  }

  static Stream<Arguments> parameters() {
    return Stream.of(
        NamespacedClusterPgResource.TOP_PG_STAT_STATEMENTS,
        NamespacedClusterPgResource.TOP_PG_STAT_ACTIVITY,
        NamespacedClusterPgResource.TOP_PG_LOCKS)
        .map(Tuple::tuple)
        .flatMap(t -> Stream.of(
            null, "test")
            .map(t::concat))
        .flatMap(t -> Stream.of(
            null,
            NamespacedClusterPgResourceQueryGenerator.ASC,
            NamespacedClusterPgResourceQueryGenerator.DESC)
            .map(t::concat))
        .flatMap(t -> Stream.of(null, 20)
            .map(t::concat))
        .map(t -> Arguments.of(t.v1, t.v2, t.v3, t.v4));
  }

  private static Properties loadExpected() throws IOException {
    Properties expected = new Properties();
    expected.load(NamespacedClusterPgResourceQueryGeneratorTest
        .class.getResourceAsStream("/cluster-queries.properties"));
    while (true) {
      final Properties currentExpected = expected;
      if (!currentExpected.values()
          .stream()
          .map(String.class::cast)
          .anyMatch(value -> currentExpected.keySet().stream()
              .anyMatch(key -> value.contains("${" + key + "}")))) {
        break;
      }
      expected = Seq.seq(currentExpected)
          .map(t -> t.map1(String.class::cast))
          .map(t -> t.map2(String.class::cast))
          .map(t -> t.map2(currentValue -> currentExpected.keySet().stream()
              .map(String.class::cast)
              .reduce(currentValue,
                  (value, key) -> value.replace(
                      "${" + key + "}", currentExpected.getProperty(key)),
                  (u, v) -> u)))
          .reduce(new Properties(),
              (properties, t) -> {
                properties.setProperty(t.v1, t.v2);
                return properties;
              },
              (u, v) -> u);
    }
    return expected;
  }

}
