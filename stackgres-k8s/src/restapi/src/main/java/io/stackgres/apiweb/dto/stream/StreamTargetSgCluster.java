/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.stream;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.SecretKeySelector;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class StreamTargetSgCluster {

  private String name;

  private String database;

  private SecretKeySelector username;

  private SecretKeySelector password;

  private String usernameValue;

  private String passwordValue;

  private Boolean skipDdlImport;

  private String ddlImportRoleSkipFilter;

  private Boolean skipDropIndexesAndConstraints;

  private Boolean skipRestoreIndexesAfterSnapshot;

  private StreamTargetJdbcSinkDebeziumProperties debeziumProperties;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDatabase() {
    return database;
  }

  public void setDatabase(String database) {
    this.database = database;
  }

  public SecretKeySelector getUsername() {
    return username;
  }

  public void setUsername(SecretKeySelector username) {
    this.username = username;
  }

  public SecretKeySelector getPassword() {
    return password;
  }

  public void setPassword(SecretKeySelector password) {
    this.password = password;
  }

  public String getUsernameValue() {
    return usernameValue;
  }

  public void setUsernameValue(String usernameValue) {
    this.usernameValue = usernameValue;
  }

  public String getPasswordValue() {
    return passwordValue;
  }

  public void setPasswordValue(String passwordValue) {
    this.passwordValue = passwordValue;
  }

  public Boolean getSkipDdlImport() {
    return skipDdlImport;
  }

  public void setSkipDdlImport(Boolean skipDdlImport) {
    this.skipDdlImport = skipDdlImport;
  }

  public String getDdlImportRoleSkipFilter() {
    return ddlImportRoleSkipFilter;
  }

  public void setDdlImportRoleSkipFilter(String ddlImportRoleSkipFilter) {
    this.ddlImportRoleSkipFilter = ddlImportRoleSkipFilter;
  }

  public Boolean getSkipDropIndexesAndConstraints() {
    return skipDropIndexesAndConstraints;
  }

  public void setSkipDropIndexesAndConstraints(Boolean skipDropIndexesAndConstraints) {
    this.skipDropIndexesAndConstraints = skipDropIndexesAndConstraints;
  }

  public Boolean getSkipRestoreIndexesAfterSnapshot() {
    return skipRestoreIndexesAfterSnapshot;
  }

  public void setSkipRestoreIndexesAfterSnapshot(Boolean skipRestoreIndexesAfterSnapshot) {
    this.skipRestoreIndexesAfterSnapshot = skipRestoreIndexesAfterSnapshot;
  }

  public StreamTargetJdbcSinkDebeziumProperties getDebeziumProperties() {
    return debeziumProperties;
  }

  public void setDebeziumProperties(
      StreamTargetJdbcSinkDebeziumProperties debeziumProperties) {
    this.debeziumProperties = debeziumProperties;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
