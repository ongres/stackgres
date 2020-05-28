/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.distributedlogs;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.Properties;

import com.google.common.collect.ImmutableMap;
import io.stackgres.apiweb.distributedlogs.dto.cluster.ClusterDto;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.conf.ParamType;
import org.jooq.impl.DefaultDSLContext;
import org.jooq.lambda.Seq;
import org.jooq.lambda.Unchecked;
import org.jooq.lambda.tuple.Tuple;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DistributedLogsQueryGeneratorTest {

  private static final Properties EXPECTED = Unchecked.supplier(
      DistributedLogsQueryGeneratorTest::loadExpected).get();

  {
    System.setProperty("org.jooq.no-logo", "true");
  }

  private static Properties loadExpected() throws IOException {
    Properties expected = new Properties();
    expected.load(DistributedLogsQueryGeneratorTest.class.getResourceAsStream(
        "/cluster-logs-queries.properties"));
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

  @Test
  public void descQueryTest() {
    assertEquals(EXPECTED.get("descQueryTest"),
        generateQuery(ImmutableDistributedLogsQueryParameters.builder()
            .cluster(new ClusterDto())
            .records(1)
            .isSortAsc(false)
            .isFromInclusive(false)
            .build()));
  }

  @Test
  public void fromDescQueryTest() {
    assertEquals(EXPECTED.get("fromDescQueryTest"),
        generateQuery(ImmutableDistributedLogsQueryParameters.builder()
            .cluster(new ClusterDto())
            .records(1)
            .isSortAsc(false)
            .isFromInclusive(false)
            .fromTimeAndIndex(Tuple.tuple(Instant.EPOCH, 0))
            .build()));
  }

  @Test
  public void fromInclusiveDescQueryTest() {
    assertEquals(EXPECTED.get("fromInclusiveDescQueryTest"),
        generateQuery(ImmutableDistributedLogsQueryParameters.builder()
            .cluster(new ClusterDto())
            .records(1)
            .isSortAsc(false)
            .isFromInclusive(true)
            .fromTimeAndIndex(Tuple.tuple(Instant.EPOCH, 0))
            .build()));
  }

  @Test
  public void toDescQueryTest() {
    assertEquals(EXPECTED.get("toDescQueryTest"),
        generateQuery(ImmutableDistributedLogsQueryParameters.builder()
            .cluster(new ClusterDto())
            .records(1)
            .isSortAsc(false)
            .isFromInclusive(false)
            .toTimeAndIndex(Tuple.tuple(Instant.EPOCH, 0))
            .build()));
  }

  @Test
  public void fromToDescQueryTest() {
    assertEquals(EXPECTED.get("fromToDescQueryTest"),
        generateQuery(ImmutableDistributedLogsQueryParameters.builder()
            .cluster(new ClusterDto())
            .records(1)
            .isSortAsc(false)
            .isFromInclusive(false)
            .fromTimeAndIndex(Tuple.tuple(Instant.EPOCH, 0))
            .toTimeAndIndex(Tuple.tuple(Instant.EPOCH, 0))
            .build()));
  }

  @Test
  public void fromInclusiveToDescQueryTest() {
    assertEquals(EXPECTED.get("fromInclusiveToDescQueryTest"),
        generateQuery(ImmutableDistributedLogsQueryParameters.builder()
            .cluster(new ClusterDto())
            .records(1)
            .isSortAsc(false)
            .isFromInclusive(true)
            .fromTimeAndIndex(Tuple.tuple(Instant.EPOCH, 0))
            .toTimeAndIndex(Tuple.tuple(Instant.EPOCH, 0))
            .build()));
  }

  @Test
  public void ascQueryTest() {
    assertEquals(EXPECTED.get("ascQueryTest"),
        generateQuery(ImmutableDistributedLogsQueryParameters.builder()
            .cluster(new ClusterDto())
            .records(1)
            .isSortAsc(true)
            .isFromInclusive(false)
            .build()));
  }

  @Test
  public void fromAscQueryTest() {
    assertEquals(EXPECTED.get("fromAscQueryTest"),
        generateQuery(ImmutableDistributedLogsQueryParameters.builder()
            .cluster(new ClusterDto())
            .records(1)
            .isSortAsc(true)
            .isFromInclusive(false)
            .fromTimeAndIndex(Tuple.tuple(Instant.EPOCH, 0))
            .build()));
  }

  @Test
  public void fromInclusiveAscQueryTest() {
    assertEquals(EXPECTED.get("fromInclusiveAscQueryTest"),
        generateQuery(ImmutableDistributedLogsQueryParameters.builder()
            .cluster(new ClusterDto())
            .records(1)
            .isSortAsc(true)
            .isFromInclusive(true)
            .fromTimeAndIndex(Tuple.tuple(Instant.EPOCH, 0))
            .build()));
  }

  @Test
  public void toAscQueryTest() {
    assertEquals(EXPECTED.get("toAscQueryTest"),
        generateQuery(ImmutableDistributedLogsQueryParameters.builder()
            .cluster(new ClusterDto())
            .records(1)
            .isSortAsc(true)
            .isFromInclusive(false)
            .toTimeAndIndex(Tuple.tuple(Instant.EPOCH, 0))
            .build()));
  }

  @Test
  public void fromToAscQueryTest() {
    assertEquals(EXPECTED.get("fromToAscQueryTest"),
        generateQuery(ImmutableDistributedLogsQueryParameters.builder()
            .cluster(new ClusterDto())
            .records(1)
            .isSortAsc(true)
            .isFromInclusive(false)
            .fromTimeAndIndex(Tuple.tuple(Instant.EPOCH, 0))
            .toTimeAndIndex(Tuple.tuple(Instant.EPOCH, 0))
            .build()));
  }

  @Test
  public void fromInclusiveToAscQueryTest() {
    assertEquals(EXPECTED.get("fromInclusiveToAscQueryTest"),
        generateQuery(ImmutableDistributedLogsQueryParameters.builder()
            .cluster(new ClusterDto())
            .records(1)
            .isSortAsc(true)
            .isFromInclusive(true)
            .fromTimeAndIndex(Tuple.tuple(Instant.EPOCH, 0))
            .toTimeAndIndex(Tuple.tuple(Instant.EPOCH, 0))
            .build()));
  }

  @Test
  public void filterQueryTest() {
    DistributedLogsQueryGenerator.FILTER_CONVERSION_MAP.keySet()
        .forEach(filter -> {
          assertEquals(EXPECTED.get("filterQueryTest_" + filter),
              generateQuery(ImmutableDistributedLogsQueryParameters.builder()
                  .cluster(new ClusterDto())
                  .records(1)
                  .isSortAsc(false)
                  .isFromInclusive(false)
                  .filters(ImmutableMap.of(filter, Optional.of("")))
                  .build()),
              "Expected query filterQueryTest_" + filter + " not matching actual");
        });
  }

  @Test
  public void nullFilterQueryTest() {
    DistributedLogsQueryGenerator.FILTER_CONVERSION_MAP.keySet()
        .forEach(filter -> {
          assertEquals(EXPECTED.get("nullFilterQueryTest_" + filter),
              generateQuery(ImmutableDistributedLogsQueryParameters.builder()
                  .cluster(new ClusterDto())
                  .records(1)
                  .isSortAsc(false)
                  .isFromInclusive(false)
                  .filters(ImmutableMap.of(filter, Optional.empty()))
                  .build()),
              "Expected query nullFilterQueryTest_" + filter + " not matching actual");
        });
  }

  @Test
  public void allFiltersQueryTest() {
    assertEquals(EXPECTED.get("allFiltersQueryTest"),
        generateQuery(ImmutableDistributedLogsQueryParameters.builder()
            .cluster(new ClusterDto())
            .records(1)
            .isSortAsc(false)
            .isFromInclusive(false)
            .filters(DistributedLogsQueryGenerator.FILTER_CONVERSION_MAP.keySet()
                .stream()
                .collect(ImmutableMap.toImmutableMap(filter -> filter, filter -> Optional.of(""))))
            .build()));
  }

  @Test
  public void textQueryTest() {
    assertEquals(EXPECTED.get("fullTextQueryTest"),
        generateQuery(ImmutableDistributedLogsQueryParameters.builder()
            .cluster(new ClusterDto())
            .records(1)
            .isSortAsc(false)
            .isFromInclusive(false)
            .fullTextSearchQuery(new FullTextSearchQuery("test"))
            .build()));
  }

  private String generateQuery(DistributedLogsQueryParameters parameters) {
    DSLContext context = new DefaultDSLContext(SQLDialect.POSTGRES);
    return new DistributedLogsQueryGenerator(context, parameters).generateQuery().getSQL(ParamType.INLINED);
  }

}
