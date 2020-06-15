/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common;

import java.util.Optional;
import java.util.Properties;

import com.google.common.base.Preconditions;
import io.stackgres.common.OperatorProperty;
import org.jooq.lambda.Seq;

public enum OperatorConfigDefaults {

  INSTANCE;

  public static final String OPERATOR_NAME = INSTANCE.operatorName;
  public static final String OPERATOR_NAMESPACE = INSTANCE.operatorNamespace;

  public static final String PROMETHEUS_AUTOBIND = INSTANCE.prometheusAutobind;

  public static final String OPERATOR_IP = INSTANCE.operatorIp;

  private final String operatorName;
  private final String operatorNamespace;
  private final String operatorIp;

  private final String prometheusAutobind;

  OperatorConfigDefaults() {
    try {
      Properties properties = new Properties();
      properties.load(OperatorConfigDefaults.class.getResourceAsStream("/application.properties"));
      Seq.seq(properties).forEach(t -> System.setProperty(
          String.class.cast(t.v1), String.class.cast(t.v2)));
      operatorName = getProperty(properties, OperatorProperty.OPERATOR_NAME);
      operatorNamespace = getProperty(properties, OperatorProperty.OPERATOR_NAMESPACE);
      prometheusAutobind = getProperty(properties, OperatorProperty.PROMETHEUS_AUTOBIND);
      operatorIp = getProperty(properties, OperatorProperty.OPERATOR_IP);
      Preconditions.checkNotNull(operatorName);
      Preconditions.checkNotNull(operatorNamespace);
      Preconditions.checkNotNull(prometheusAutobind);
    } catch (RuntimeException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  /**
   * Return a property value by searching first in environment variables and if not present in
   * system properties.
   */
  private static String getProperty(Properties properties, OperatorProperty configProperty) {
    return Optional.ofNullable(System.getenv(configProperty.property()))
        .orElseGet(() -> properties.getProperty(configProperty.systemProperty()));
  }

}
