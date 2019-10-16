/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.google.common.base.Preconditions;
import io.stackgres.common.config.ConfigContext;

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
      ObjectMapper objectMapper = new YAMLMapper();
      JsonNode operatorConfig = objectMapper.readTree(
          StackGresUtil.class.getResourceAsStream("/stackgres-operator/values.yaml"));
      operatorName = Optional.ofNullable(System.getenv(ConfigContext.OPERATOR_NAME))
          .orElseGet(() -> operatorConfig.get("operatorName").asText());
      operatorNamespace = Optional.ofNullable(System.getenv(ConfigContext.OPERATOR_NAMESPACE))
          .orElseGet(() -> operatorConfig.get("operatorNamespace").asText());
      operatorVersion = Optional.ofNullable(System.getenv(ConfigContext.OPERATOR_VERSION))
          .orElseGet(() -> operatorConfig.get("operatorVersion").asText());
      group = Optional.ofNullable(System.getenv(ConfigContext.CRD_GROUP))
          .orElseGet(() -> operatorConfig.get("group").asText());
      version = Optional.ofNullable(System.getenv(ConfigContext.CRD_VERSION))
          .orElseGet(() -> operatorConfig.get("crd").get("version").asText());
      containerBuild = Optional.ofNullable(System.getenv(ConfigContext.CONTAINER_BUILD))
          .orElseGet(() -> operatorConfig.get("containerBuild").asText());

      prometheusAutobind = Optional.ofNullable(System.getenv(ConfigContext.PROMETHEUS_AUTOBIND))
          .orElseGet(() -> operatorConfig.get("prometheus").get("allowAutobind").asText());
      Preconditions.checkNotNull(operatorName);
      Preconditions.checkNotNull(operatorNamespace);
      Preconditions.checkNotNull(operatorVersion);
      Preconditions.checkNotNull(group);
      Preconditions.checkNotNull(version);
      Preconditions.checkNotNull(containerBuild);
    } catch (RuntimeException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

}
