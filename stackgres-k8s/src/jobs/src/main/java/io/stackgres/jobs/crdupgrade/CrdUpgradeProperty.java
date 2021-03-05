/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.crdupgrade;

import java.util.Properties;

import io.stackgres.common.StackGresPropertyReader;
import org.jooq.lambda.Unchecked;

public enum CrdUpgradeProperty implements StackGresPropertyReader {

  OPERATOR_NAME("stackgres.operatorName"),
  OPERATOR_NAMESPACE("stackgres.operatorNamespace"),
  CRD_UPGRADE("stackgres.crdUpgrade"),
  CONVERSION_WEBHOOKS("stackgres.conversionWebhooks");

  private static final Properties APPLICATION_PROPERTIES =
      Unchecked.supplier(() -> StackGresPropertyReader
          .readApplicationProperties(CrdUpgradeProperty.class)).get();

  private final String propertyName;

  CrdUpgradeProperty(String propertyName) {
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
