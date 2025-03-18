/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class PoolingConfigPgBouncerPgbouncerIni {

  private Map<String, String> pgbouncer;

  private Map<String, Map<String, String>> databases;

  private Map<String, Map<String, String>> users;

  public Map<String, String> getPgbouncer() {
    return pgbouncer;
  }

  public void setPgbouncer(Map<String, String> pgbouncer) {
    this.pgbouncer = pgbouncer;
  }

  public Map<String, Map<String, String>> getDatabases() {
    return databases;
  }

  public void setDatabases(Map<String, Map<String, String>> databases) {
    this.databases = databases;
  }

  public Map<String, Map<String, String>> getUsers() {
    return users;
  }

  public void setUsers(Map<String, Map<String, String>> users) {
    this.users = users;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
