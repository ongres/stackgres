/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator;

import java.util.Properties;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

public class CheckOperatorValuesExtension implements ExecutionCondition {

  @Override
  public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
    try {
      Properties properties = new Properties();
      properties.load(CheckOperatorValuesExtension.class.getResourceAsStream("/test.properties"));
      if (!Boolean.valueOf(properties.getProperty("check-operator-values"))) {
        return ConditionEvaluationResult.disabled("Check operator values disabled");
      }
      return ConditionEvaluationResult.enabled("Check operator values enabled");
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

}
