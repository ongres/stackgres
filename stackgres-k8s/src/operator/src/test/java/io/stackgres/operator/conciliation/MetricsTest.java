/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.ClientBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

@QuarkusTest
class MetricsTest {

  @Inject
  private Metrics metrics;

  @ConfigProperty(name = "quarkus.http.test-port")
  int port;
  
  @Test
  public void checkMetricsAreServedByPrometheus() {
    metrics.gauge("test", 1234);
    ClientBuilder clientBuilder = ClientBuilder.newBuilder();
    assertTrue(Arrays.asList(
        clientBuilder.build()
        .target("http://localhost:" + port)
        .path("/q/metrics")
        .request()
        .buildGet()
        .invoke(String.class)
        .split("\\n"))
        .contains("operator_test 1234.0"));
    metrics.gauge("test", 4321);
    assertTrue(Arrays.asList(
        clientBuilder.build()
        .target("http://localhost:" + port)
        .path("/q/metrics")
        .request()
        .buildGet()
        .invoke(String.class)
        .split("\\n"))
        .contains("operator_test 4321.0"));
  }

}
