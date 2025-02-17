/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.configuration;

import javax.management.MalformedObjectNameException;

import io.stackgres.common.metrics.AbstractJmxCollectorRegistry;
import jakarta.inject.Singleton;

@Singleton
public class WebApiJmxCollectorRegistry extends AbstractJmxCollectorRegistry {

  public WebApiJmxCollectorRegistry() throws MalformedObjectNameException {
    super(WebApiProperty.RESTAPI_JMX_COLLECTOR_YAML_CONFIG.get().orElse(""));
  }

}
