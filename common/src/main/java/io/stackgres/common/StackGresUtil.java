/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

public enum StackGresUtil {

  INSTANCE;

  public static final String OPERATOR_NAME = INSTANCE.operatorName;
  public static final String OPERATOR_NAMESPACE = INSTANCE.operatorNamespace;
  public static final String OPERATOR_VERSION = INSTANCE.operatorVersion;

  public static final String GROUP = INSTANCE.group;
  public static final String CRD_VERSION = INSTANCE.version;

  private final String operatorName;
  private final String operatorNamespace;
  private final String operatorVersion;

  private final String group;
  private final String version;

  StackGresUtil() {
    try {
      ObjectMapper objectMapper = new YAMLMapper();
      JsonNode operatorConfig = objectMapper.readTree(
          StackGresUtil.class.getResourceAsStream("/stackgres-operator/values.yaml"));
      operatorName = operatorConfig.get("operatorName").asText();
      operatorNamespace = operatorConfig.get("operatorNamespace").asText();
      operatorVersion = operatorConfig.get("operatorVersion").asText();
      group = operatorConfig.get("group").asText();
      version = operatorConfig.get("crd").get("version").asText();
    } catch (RuntimeException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

}
