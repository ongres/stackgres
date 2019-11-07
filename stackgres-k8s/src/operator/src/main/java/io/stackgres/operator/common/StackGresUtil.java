/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common;

import java.util.Optional;
import java.util.Properties;

import com.google.common.base.Preconditions;

public enum StackGresUtil {

  INSTANCE;

  public static final String OPERATOR_NAME = INSTANCE.operatorName;
  public static final String OPERATOR_NAMESPACE = INSTANCE.operatorNamespace;
  public static final String OPERATOR_VERSION = INSTANCE.operatorVersion;

  public static final String CRD_GROUP = INSTANCE.group;
  public static final String CRD_VERSION = INSTANCE.version;

  public static final String CONTAINER_BUILD = INSTANCE.containerBuild;

  public static final String PROMETHEUS_AUTOBIND = INSTANCE.prometheusAutobind;

  private final String operatorName;
  private final String operatorNamespace;
  private final String operatorVersion;

  private final String group;
  private final String version;

  private final String containerBuild;

  private final String prometheusAutobind;

  StackGresUtil() {
    try {
      Properties properties = new Properties();
      properties.load(StackGresUtil.class.getResourceAsStream("/application.properties"));
      operatorName = getProperty(properties, ConfigProperty.OPERATOR_NAME);
      operatorNamespace = getProperty(properties, ConfigProperty.OPERATOR_NAMESPACE);
      operatorVersion = getProperty(properties, ConfigProperty.OPERATOR_VERSION);
      group = getProperty(properties, ConfigProperty.CRD_GROUP);
      version = getProperty(properties, ConfigProperty.CRD_VERSION);
      containerBuild = getProperty(properties, ConfigProperty.CONTAINER_BUILD);
      prometheusAutobind = getProperty(properties, ConfigProperty.PROMETHEUS_AUTOBIND);
      Preconditions.checkNotNull(operatorName);
      Preconditions.checkNotNull(operatorNamespace);
      Preconditions.checkNotNull(operatorVersion);
      Preconditions.checkNotNull(group);
      Preconditions.checkNotNull(version);
      Preconditions.checkNotNull(containerBuild);
      Preconditions.checkNotNull(prometheusAutobind);
    } catch (RuntimeException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  private static String getProperty(Properties properties, ConfigProperty configProperty) {
    return Optional.ofNullable(System.getenv(configProperty.property()))
        .orElseGet(() -> properties.getProperty(configProperty.systemProperty()));
  }

}
