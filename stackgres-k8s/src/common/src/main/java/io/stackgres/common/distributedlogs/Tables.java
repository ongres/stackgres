/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.distributedlogs;

public enum Tables {

  LOG_POSTGRES("log_postgres"),
  LOG_PATRONI("log_patroni");

  private final String tableName;

  Tables(String tableName) {
    this.tableName = tableName;
  }

  public String getTableName() {
    return tableName;
  }
}
