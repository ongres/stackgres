/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.util.Properties;

public enum DistributedLogsControllerProperty implements StackGresPropertyReader {

  DISTRIBUTEDLOGS_NAMESPACE("stackgres.distributedlogsNamespace"),
  DISTRIBUTEDLOGS_NAME("stackgres.distributedlogsName"),
  DISTRIBUTEDLOGS_CONTROLLER_POD_NAME("stackgres.distributedlogsControllerPodName"),
  DISTRIBUTEDLOGS_CONTROLLER_EXTENSIONS_REPOSITORY_URLS(
      "stackgres.distributedlogsControllerExtensionsRepositoryUrls"),
  DISTRIBUTEDLOGS_CONTROLLER_SKIP_OVERWRITE_SHARED_LIBRARIES(
      "stackgres.distributedlogsSkipOverwriteSharedLibraries");

  private static final Properties APPLICATION_PROPERTIES =
      StackGresPropertyReader.readApplicationProperties(DistributedLogsControllerProperty.class);

  private final String propertyName;

  DistributedLogsControllerProperty(String propertyName) {
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
