/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops;

import io.micrometer.core.instrument.MeterRegistry;
import io.stackgres.common.metrics.AbstractMetrics;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class Metrics extends AbstractMetrics {

  @Inject
  public Metrics(
      MeterRegistry registry) {
    super(registry, "jobs");
  }

}
