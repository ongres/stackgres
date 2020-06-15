/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.distributedlogs;

public enum PatroniTableFields {

  LOG_TIME(LogTableFields.LOG_TIME),
  LOG_TIME_INDEX(LogTableFields.LOG_TIME_INDEX),
  POD_NAME(LogTableFields.POD_NAME),
  ROLE(LogTableFields.ROLE),
  ERROR_SEVERITY(LogTableFields.ERROR_SEVERITY),
  MESSAGE(LogTableFields.MESSAGE);

  private final String fieldName;

  PatroniTableFields(String fieldName) {
    this.fieldName = fieldName;
  }

  public String getFieldName() {
    return fieldName;
  }
}
