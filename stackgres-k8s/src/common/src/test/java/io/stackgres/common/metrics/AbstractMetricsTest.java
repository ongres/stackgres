/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.metrics;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import jakarta.inject.Inject;
import jakarta.ws.rs.client.ClientBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

public abstract class AbstractMetricsTest {

  @Inject
  AbstractMetrics abstractMetrics;

  @ConfigProperty(name = "quarkus.http.test-port")
  int port;

  @Test
  public void checkMetricsAreServedByPrometheus() {
    ClientBuilder clientBuilder = ClientBuilder.newBuilder();
    assertTrue(Arrays.asList(
        clientBuilder.build()
        .target("http://localhost:" + port)
        .path("/q/metrics")
        .request()
        .buildGet()
        .invoke(String.class)
        .split("\\n"))
        .stream()
        .anyMatch(line -> line.startsWith("jvm_gc_overhead ")));
    abstractMetrics.gauge("test", 1234);
    assertTrue(Arrays.asList(
        clientBuilder.build()
        .target("http://localhost:" + port)
        .path("/q/metrics")
        .request()
        .buildGet()
        .invoke(String.class)
        .split("\\n"))
        .contains(abstractMetrics.getPrefix() + "test 1234.0"));
    abstractMetrics.gauge("test", 4321);
    assertTrue(Arrays.asList(
        clientBuilder.build()
        .target("http://localhost:" + port)
        .path("/q/metrics")
        .request()
        .buildGet()
        .invoke(String.class)
        .split("\\n"))
        .contains(abstractMetrics.getPrefix() + "test 4321.0"));
  }

}
