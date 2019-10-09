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

public enum StackGresUtil {

  INSTANCE;

  public static final String OPERATOR_NAME = INSTANCE.operatorName;
  public static final String OPERATOR_NAMESPACE = INSTANCE.operatorNamespace;
  public static final String OPERATOR_VERSION = INSTANCE.operatorVersion;

  public static final String CRD_GROUP = INSTANCE.group;
  public static final String CRD_VERSION = INSTANCE.version;

  public static final String CONTAINER_BUILD = INSTANCE.containerBuild;

  private final String operatorName;
  private final String operatorNamespace;
  private final String operatorVersion;

  private final String group;
  private final String version;

  private final String containerBuild;

  StackGresUtil() {
    try {
      ObjectMapper objectMapper = new YAMLMapper();
      JsonNode operatorConfig = objectMapper.readTree(
          StackGresUtil.class.getResourceAsStream("/stackgres-operator/values.yaml"));
      operatorName = Optional.ofNullable(System.getenv("OPERATOR_NAME"))
          .orElseGet(() -> operatorConfig.get("operatorName").asText());
      operatorNamespace = Optional.ofNullable(System.getenv("OPERATOR_NAMESPACE"))
          .orElseGet(() -> operatorConfig.get("operatorNamespace").asText());
      operatorVersion = Optional.ofNullable(System.getenv("OPERATOR_VERSION"))
          .orElseGet(() -> operatorConfig.get("operatorVersion").asText());
      group = Optional.ofNullable(System.getenv("CRD_GROUP"))
          .orElseGet(() -> operatorConfig.get("group").asText());
      version = Optional.ofNullable(System.getenv("CRD_VERSION"))
          .orElseGet(() -> operatorConfig.get("crd").get("version").asText());
      containerBuild = Optional.ofNullable(System.getenv("CONTAINER_BUILD"))
          .orElseGet(() -> operatorConfig.get("containerBuild").asText());
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
