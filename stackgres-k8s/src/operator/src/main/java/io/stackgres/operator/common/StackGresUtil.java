/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common;

import java.util.Optional;
import java.util.Properties;

import com.google.common.base.Preconditions;

import io.stackgres.operator.config.ConfigContext;

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
      operatorName = Optional.ofNullable(System.getenv(ConfigContext.OPERATOR_NAME))
          .orElseGet(() -> properties.getProperty("stackgres.operatorName"));
      operatorNamespace = Optional.ofNullable(System.getenv(ConfigContext.OPERATOR_NAMESPACE))
          .orElseGet(() -> properties.getProperty("stackgres.operatorNamespace"));
      operatorVersion = Optional.ofNullable(System.getenv(ConfigContext.OPERATOR_VERSION))
          .orElseGet(() -> properties.getProperty("stackgres.operatorVersion"));
      group = Optional.ofNullable(System.getenv(ConfigContext.CRD_GROUP))
          .orElseGet(() -> properties.getProperty("stackgres.group"));
      version = Optional.ofNullable(System.getenv(ConfigContext.CRD_VERSION))
          .orElseGet(() -> properties.getProperty("stackgres.crd.version"));
      containerBuild = Optional.ofNullable(System.getenv(ConfigContext.CONTAINER_BUILD))
          .orElseGet(() -> properties.getProperty("stackgres.containerBuild"));
      prometheusAutobind = Optional.ofNullable(System.getenv(ConfigContext.PROMETHEUS_AUTOBIND))
          .orElseGet(() -> properties.getProperty("stackgres.prometheus.allowAutobind"));
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

}
