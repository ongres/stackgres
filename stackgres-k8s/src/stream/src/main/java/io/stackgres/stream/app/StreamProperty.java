/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.stream.app;

import java.util.Properties;

import io.stackgres.common.StackGresPropertyReader;

public enum StreamProperty implements StackGresPropertyReader {

  OPERATOR_NAME("stackgres.operatorName"),
  OPERATOR_NAMESPACE("stackgres.operatorNamespace"),
  OPERATOR_CERTIFICATE_SECRET_NAME("stackgres.operatorCertificateSecretName"),
  STREAM_NAMESPACE("stackgres.streamNamespace"),
  OPERATOR_VERSION("stackgres.operatorVersion"),
  STREAM_NAME("stackgres.streamCrName"),
  SERVICE_ACCOUNT("stackgres.stream.serviceAccount"),
  POD_NAME("stackgres.stream.podName"),
  STREAM_LOCK_POLL_INTERVAL("stackgres.stream.lockPollInterval"),
  STREAM_LOCK_DURATION("stackgres.stream.lockDuration"),
  STREAM_JMX_COLLECTOR_YAML_CONFIG("stackgres.stream.jmxCollectorYamlConfig"),
  STREAM_MBEAN_POLLING_PERIOD("stackgres.stream.mBeanPollingPeriod");

  private static final Properties APPLICATION_PROPERTIES =
      StackGresPropertyReader.readApplicationProperties(StreamProperty.class);

  private final String propertyName;

  StreamProperty(String propertyName) {
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
