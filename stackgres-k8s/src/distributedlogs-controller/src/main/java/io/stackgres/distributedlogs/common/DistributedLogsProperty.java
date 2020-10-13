/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.distributedlogs.common;

import java.util.Properties;

import io.stackgres.common.StackGresPropertyReader;
import org.jooq.lambda.Unchecked;

public enum DistributedLogsProperty implements StackGresPropertyReader {

  DISTRIBUTEDLOGS_NAMESPACE("stackgres.distributedlogsNamespace"),
  DISTRIBUTEDLOGS_NAME("stackgres.distributedlogsName"),
  DISTRIBUTEDLOGS_CONTROLLER_POD_NAME("stackgres.distributedlogsControllerPodName");

  private static final Properties APPLICATION_PROPERTIES =
      Unchecked.supplier(() -> StackGresPropertyReader
          .readApplicationProperties(DistributedLogsProperty.class)).get();

  private final String propertyName;

  DistributedLogsProperty(String propertyName) {
    this.propertyName = propertyName;
  }

  @Override
  public String getEnvironmentVariableName() {
    return name();
  }

  @Override
  public String getPropertyName() {
    return propertyName;
  }

  @Override
  public Properties getApplicationProperties() {
    return APPLICATION_PROPERTIES;
  }
}
