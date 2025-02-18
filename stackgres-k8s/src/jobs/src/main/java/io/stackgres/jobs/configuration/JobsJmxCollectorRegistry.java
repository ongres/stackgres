/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.configuration;

import javax.management.MalformedObjectNameException;

import io.stackgres.common.metrics.AbstractJmxCollectorRegistry;
import jakarta.inject.Singleton;

@Singleton
public class JobsJmxCollectorRegistry extends AbstractJmxCollectorRegistry {

  public JobsJmxCollectorRegistry() throws MalformedObjectNameException {
    super(JobsProperty.JOBS_JMX_COLLECTOR_YAML_CONFIG.get().orElse(""));
  }

}
