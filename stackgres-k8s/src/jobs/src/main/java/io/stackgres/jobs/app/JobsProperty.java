/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.app;

import java.util.Properties;

import io.stackgres.common.StackGresPropertyReader;

public enum JobsProperty implements StackGresPropertyReader {

  OPERATOR_NAME("stackgres.operatorName"),
  OPERATOR_NAMESPACE("stackgres.operatorNamespace"),
  JOB_NAMESPACE("stackgres.jobNamespace"),
  OPERATOR_VERSION("stackgres.operatorVersion"),
  CRD_UPGRADE("stackgres.crdUpgrade"),
  CONVERSION_WEBHOOKS("stackgres.conversionWebhooks"),
  CR_UPDATER("stackgres.crUpdater"),
  DATABASE_OPERATION_JOB("stackgres.databaseOperationJob"),
  DATABASE_OPERATION_CR_NAME("stackgres.databaseOperationCrName"),
  SERVICE_ACCOUNT("stackgres.dbops.serviceAccount"),
  POD_NAME("stackgres.dbops.podName"),
  DBOPS_LOCK_POLL_INTERVAL("stackgres.dbops.lockPollInterval"),
  DBOPS_LOCK_TIMEOUT("stackgres.dbops.lockTimeout");

  private static final Properties APPLICATION_PROPERTIES =
      StackGresPropertyReader.readApplicationProperties(JobsProperty.class);

  private final String propertyName;

  JobsProperty(String propertyName) {
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
